package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import com.google.common.primitives.Ints;

import edu.ncsu.wls.CloudletDAG;
import edu.ncsu.wls.DAGCentralScheduler;
import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudSimHelper;
import edu.ncsu.wls.MyCloudlet;
import edu.ncsu.wls.OnlineDatacenterBroker;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionType;
import jmetal.core.Variable;
import jmetal.metaheuristics.moead.Utils;
import jmetal.util.JMException;

/**
 * Storing element type of chromosome for VM scheduling problem Used by
 * VMScheduleSoltionType
 * 
 * @author jianfeng
 *
 */
class VmEncoding extends Variable {
	private static final long serialVersionUID = 7889788136550407561L;
	protected int[] task2ins;
	protected int[] ins2type;
	protected int[] taskInOrder;

	public VmEncoding(int[] taskInOrder, int[] task2ins, int[] ins2type) {
		this.taskInOrder = taskInOrder;
		this.task2ins = task2ins;
		this.ins2type = ins2type;
	}

	public VmEncoding() {
	}

	public VmEncoding(int l) {
		task2ins = new int[l];
		ins2type = new int[l];
		taskInOrder = new int[l];
	}

	@Override
	public Variable deepCopy() {
		return new VmEncoding(taskInOrder, task2ins, ins2type);
	}

}

/**
 * Solution type of VMS problem. including a function-- randomly assigning vm
 * scheduling
 * 
 * @author jianfeng
 *
 */
class VMScheduleSolutionType extends SolutionType {
	Random rand;
	VmsProblem problem_;

	public VMScheduleSolutionType(VmsProblem problem, Random rand) {
		super(problem);
		problem_ = problem;
		this.rand = rand;
	}

	@Override
	public Variable[] createVariables() {
		int totalV = problem_.cloudletNum;
		VmEncoding coding = new VmEncoding();

		int[] orders = IntStream.range(0, totalV).toArray();
		List<Integer> o = Ints.asList(orders);
		Collections.shuffle(o);
		orders = o.stream().mapToInt(i -> i).toArray();
		coding.taskInOrder = orders;

		int[] task2ins = new int[totalV];
		for (int i = 0; i < task2ins.length; i++)
			task2ins[i] = rand.nextInt(totalV - 1);
		coding.task2ins = task2ins;

		int[] ins2type = new int[totalV];
		int avalType = Infrastructure.getAvailableVmTypes();
		for (int i = 0; i < ins2type.length; i++)
			ins2type[i] = rand.nextInt(avalType);
		coding.ins2type = ins2type;

		return new Variable[] { coding };
	}

}

/**
 * Definition of VM scheduling problem
 * 
 * @author jianfeng
 *
 */
public class VmsProblem extends Problem {
	private static final long serialVersionUID = 6371615104008697832L;
	public Random rand;
	public int cloudletNum;
	private int evalCount;
	private List<MyCloudlet> cloudletList;

	private CloudletDAG workflow;

	private String name;

	@SuppressWarnings("unchecked")
	public VmsProblem(String dataset, Random rand) {
		this.rand = rand;
		this.name = dataset;

		Object[] tmpInfo = Infrastructure.getCaseCloudlets(this.name, -1);
		cloudletList = (List<MyCloudlet>) tmpInfo[0];
		cloudletNum = cloudletList.size();
		workflow = (CloudletDAG) (tmpInfo[1]);

		this.numberOfVariables_ = 1; // TODO ATTENTION
		this.numberOfObjectives_ = 2; // TODO change this if needed

		this.solutionType_ = new VMScheduleSolutionType(this, rand);

		this.evalCount = 0;
	}

	@Override
	public void evaluate(Solution solution) {
		evalCount += 1;
		if (evalCount % 100 == 0 || evalCount == 1) {
			System.out.println(
					"[VmsP] Time -- " + System.currentTimeMillis() / 1000 % 100000 + " Eval # so far : " + evalCount);
		}

		Variable[] decs = solution.getDecisionVariables();
		int[] order = ((VmEncoding) decs[0]).taskInOrder;
		int[] task2ins = ((VmEncoding) decs[0]).task2ins;
		int[] ins2type = ((VmEncoding) decs[0]).ins2type;

		// ****** starting cloudsim simulation
		long s1 = System.currentTimeMillis();

		Log.disable();
		// Create Cloudsim server
		int num_user = 1; // number of cloud users
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false; // trace events

		CloudSim.init(num_user, calendar, trace_flag, Infrastructure.PEROID_BETWEEN_EVENTS);
		Infrastructure.createDatacenter(this.cloudletNum);
		OnlineDatacenterBroker broker = null;
		try {
			broker = new OnlineDatacenterBroker("dc_broker");
		} catch (Exception e) {
			e.printStackTrace();
		}

		int dcBrokerId = broker.getId();
		long s2 = System.currentTimeMillis();

		// Get dataset
		Object[] info = Infrastructure.getCaseCloudlets(this.name, broker.getId());
		@SuppressWarnings("unchecked")
		List<MyCloudlet> cloudletList = (List<MyCloudlet>) info[0];
		CloudletDAG workflow = (CloudletDAG) info[1];

		// reset cloudlet to factory config
		for (MyCloudlet c : cloudletList)
			c.setCloudletFinishedSoFar(0);
		workflow.rmCache();

		if (Ints.contains(task2ins, -1))
			workflow.turnOnIgnoreDAGMode();

		long s3 = System.currentTimeMillis();
		// System.err.println(s3 - s2 + " B");

		// Create vm list
		List<Vm> vmlist = new ArrayList<Vm>();
		vmlist = Infrastructure.createVms(dcBrokerId, task2ins, ins2type);

		// map task to vm
		for (int var = 0; var < cloudletNum; var++) {
			if (task2ins[var] == -1) {
				workflow.totalCloudletNum -= 1;
				continue;
			}
			cloudletList.get(var).setVmId(vmlist.get(task2ins[var]).getId());
		}

		workflow.calcFileTransferTimes(task2ins, vmlist, cloudletList);

		// binding global workflow to vm
		vmlist.removeAll(Collections.singleton(null)); // remove null in vmlist
		for (Vm vm : vmlist.subList(0, 1)) { // ATTENTION: NOW SHARE SAME
												// DAGCENTRALSCHEDULER
			((DAGCentralScheduler) (vm.getCloudletScheduler())).setCloudletPassport(workflow);
			((DAGCentralScheduler) (vm.getCloudletScheduler())).setVmList(vmlist);
		}

		long s4 = System.currentTimeMillis();
		// System.err.println(s4 - s3 + " C");

		// re-range cloudletList according to order
		List<MyCloudlet> tmp = new ArrayList<MyCloudlet>();

		for (int i : order) {
			if (task2ins[i] != -1)
				tmp.add(cloudletList.get(i));
		}

		broker.submitCloudletList(tmp);
		broker.submitVmList(vmlist);

		long s5 = System.currentTimeMillis();
		// System.err.println(s5 - s4 + " D");

		CloudSim.startSimulation();
		CloudSim.stopSimulation();
		long s6 = System.currentTimeMillis();
		// System.err.println(s6 - s5 + " E");

		List<Cloudlet> revList = broker.getCloudletReceivedList();
		MyCloudSimHelper.printCloudletList(revList);

		if (revList.size() != workflow.totalCloudletNum) {
			System.err.println("[VmsP] can not simulating all cloudlets!");
			System.err.println("left # = " + (cloudletList.size() - revList.size()));
			MyCloudSimHelper.forcePrintCloudList(revList);
			System.exit(-1);
			return;
		}

		// calculating objectives
		double makespan = 0;
		for (Cloudlet c : revList) {
			makespan = Math.max(makespan, c.getFinishTime());
		}

		// calculating cost
		double cost = 0;
		for (Vm v : vmlist) {
			double start = Double.MAX_VALUE;
			double end = -Double.MAX_VALUE;
			for (Cloudlet c : revList) {
				if (c.getVmId() != v.getId())
					continue;
				start = Double.min(start, c.getExecStartTime());
				end = Double.max(start, c.getFinishTime());
			}
			if (end - start > 0)
				cost += Infrastructure.getUnitPrice(v) * Math.ceil((end - start) / 3600);
		}

		// System.out.printf("%.1fs with $%.3f\n", makespan, cost);
		solution.setObjective(0, makespan);
		solution.setObjective(1, cost);
		long s7 = System.currentTimeMillis();
		// System.err.println(s7 - s6 + " F");
		// System.err.println(System.currentTimeMillis() - s1);
	}

	public int randInt(int bound) {
		return this.rand.nextInt(bound);
	}

	public CloudletDAG getWorkflow() {
		return workflow;
	}

	public List<MyCloudlet> getCloudletList() {
		return cloudletList;
	}

	public List<Cloudlet> getCloudletList2() {
		List<Cloudlet> l = new ArrayList<Cloudlet>();
		for (MyCloudlet c : cloudletList)
			l.add((Cloudlet) c);
		return l;
	}

	public void printSolution(Solution s) {
		Variable[] decs = s.getDecisionVariables();

		int[] order = ((VmEncoding) decs[0]).taskInOrder;
		int[] task2ins = ((VmEncoding) decs[0]).task2ins;
		int[] ins2type = ((VmEncoding) decs[0]).ins2type;

		System.out.println(Arrays.toString(order));
		System.out.println(Arrays.toString(task2ins));
		System.out.println(Arrays.toString(ins2type));
		System.out.println("------");
		System.out.println();
	}

	public static void main(String[] args) throws ClassNotFoundException, JMException {
		VmsProblem p = new VmsProblem("sci_Inspiral_100", new Random(15188));
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			Solution randS = new Solution(p);
			p.evaluate(randS);
			System.out.println(i + " done.");
		}

		System.out.println("Total time = " + (System.currentTimeMillis() - start) / 1000);
	}
}