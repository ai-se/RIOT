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
	private int task2ins;
	private int ins2type;
	private int taskInOrder;

	public VmEncoding(int taskInOrder, int task2ins, int ins2type) {
		this.taskInOrder = taskInOrder;
		this.task2ins = task2ins;
		this.ins2type = ins2type;
	}

	public void setTask2ins(int ins) {
		this.task2ins = ins;
	}

	public void setIns2Type(int type) {
		this.ins2type = type;
	}

	public void setOrder(int order) {
		this.taskInOrder = order;
	}

	public int getTask2ins() {
		return task2ins;
	}

	public int getIns2type() {
		return ins2type;
	}

	public int getOrder() {
		return taskInOrder;
	}

	@Override
	public String toString() {
		return this.task2ins + "";
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
		Variable[] variables = new Variable[problem_.getNumberOfVariables()];
		int[] tmp_orders = IntStream.range(0, problem_.getNumberOfVariables()).toArray();
		List<Integer> orders = IntStream.of(tmp_orders).boxed().collect(Collectors.toList());

		Collections.shuffle(orders, problem_.rand);

		for (int var = 0; var < problem_.getNumberOfVariables(); var++) {
			int order = orders.get(var);
			int task2ins = rand.nextInt(problem_.getNumberOfVariables() - 1);
			int ins2type = rand.nextInt(Infrastructure.getAvailableVmTypes());
			variables[var] = new VmEncoding(order, task2ins, ins2type);
		}
		return variables;
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
	private int cloudletNum;
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

		this.numberOfVariables_ = this.cloudletNum;
		this.numberOfObjectives_ = 2; // TODO change this if needed

		this.solutionType_ = new VMScheduleSolutionType(this, rand);

		this.evalCount = 0;
	}

	@Override
	public void evaluate(Solution solution) {
		evalCount += 1;
		if (evalCount % 100 == 0 || evalCount == 1)
			System.out
					.println("Time -- " + System.currentTimeMillis() / 1000 % 100000 + " Eval # so far : " + evalCount);

		Variable[] decs = solution.getDecisionVariables();
		int[] order = new int[getNumberOfVariables()];
		int[] task2ins = new int[getNumberOfVariables()];
		int[] ins2type = new int[getNumberOfVariables()];

		for (int var = 0; var < getNumberOfVariables(); var++) {
			order[var] = ((VmEncoding) decs[var]).getOrder();
			task2ins[var] = ((VmEncoding) decs[var]).getTask2ins();
			ins2type[var] = ((VmEncoding) decs[var]).getIns2type();
		}

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
		long s3 = System.currentTimeMillis();
		// System.err.println(s3 - s2 + " B");

		// Create vm list
		List<Vm> vmlist = new ArrayList<Vm>();
		vmlist = Infrastructure.createVms(dcBrokerId, task2ins, ins2type);

		// map task to vm
		for (int var = 0; var < cloudletNum; var++)
			cloudletList.get(var).setVmId(vmlist.get(task2ins[var]).getId());
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

		for (int i : order)
			tmp.add(cloudletList.get(i));
		broker.submitCloudletList(tmp);
		broker.submitVmList(vmlist);

		long s5 = System.currentTimeMillis();
		// System.err.println(s5 - s4 + " D");

		CloudSim.startSimulation();
		CloudSim.stopSimulation();
		long s6 = System.currentTimeMillis();
		// System.err.println(s6 - s5 + " E");

		List<Cloudlet> newList = broker.getCloudletReceivedList();
		MyCloudSimHelper.printCloudletList(newList);

		if (newList.size() != cloudletList.size()) {
			System.err.println("can not simulating all cloudlets!");
			System.err.println("left # = " + (cloudletList.size() - newList.size()));
			MyCloudSimHelper.forcePrintCloudList(newList);
			System.exit(-1);
			return;
		}

		// calculating objectives
		double makespan = 0;
		for (Cloudlet c : newList) {
			makespan = Math.max(makespan, c.getFinishTime());
		}

		double cost = Infrastructure.getUnitPrice(vmlist) * makespan / 3600;

		System.out.println(makespan + " $" + cost);
		solution.setObjective(0, makespan);
		solution.setObjective(1, cost);
		long s7 = System.currentTimeMillis();
		// System.err.println(s7 - s6 + " F");
		// System.err.println(System.currentTimeMillis() - s1);
		// System.out.println("makespan = " + makespan);
		// System.out.println("cost = " + cost);
		// System.out.println();
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
		int varLength = decs.length;

		int[] order = new int[varLength];
		int[] task2ins = new int[varLength];
		int[] ins2type = new int[varLength];

		for (int v = 0; v < varLength; v++) {
			VmEncoding var = (VmEncoding) decs[v];
			order[v] = var.getOrder();
			task2ins[v] = var.getTask2ins();
			ins2type[v] = var.getIns2type();
		}
		System.out.println(Arrays.toString(order));
		System.out.println(Arrays.toString(task2ins));
		System.out.println(Arrays.toString(ins2type));
		System.out.println("------");
		System.out.println();

	}

	public static void main(String[] args) throws ClassNotFoundException, JMException {
		VmsProblem p = new VmsProblem("sci_Epigenomics_997", new Random(15188));
		for (int i = 0; i < 30; i++) {
			Solution randS = new Solution(p);
			p.evaluate(randS);
		}
	}
}