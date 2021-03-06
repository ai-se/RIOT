package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

import com.google.common.primitives.Ints;

import edu.ncsu.model.DAG;
import edu.ncsu.model.DAGCentralSimulator;
import edu.ncsu.model.INFRA;
import edu.ncsu.model.MyCloudSimHelper;
import edu.ncsu.model.Task;
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
	protected int[] task2ins;
	protected int[] ins2type;
	protected int[] taskInOrder;

	public VmEncoding(int l) {
		task2ins = new int[l];
		ins2type = new int[l];
		taskInOrder = new int[l];
	}

	@Override
	public Variable deepCopy() {
		int l = task2ins.length;
		VmEncoding v = new VmEncoding(l);
		System.arraycopy(task2ins, 0, v.task2ins, 0, l);
		System.arraycopy(ins2type, 0, v.ins2type, 0, l);
		System.arraycopy(taskInOrder, 0, v.taskInOrder, 0, l);
		return v;
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

	/**
	 * Following Zhu's RandTypeOrIns
	 */
	@Override
	public Variable[] createVariables() {
		int totalV = problem_.tasksNum;
		VmEncoding coding = new VmEncoding(totalV);

		coding.taskInOrder = IntStream.range(0, totalV).toArray();

		if (rand.nextDouble() < 0.5)
			for (int i = 0; i < totalV; i++)
				coding.task2ins[i] = 0;
		else
			for (int i = 0; i < totalV; i++)
				coding.task2ins[i] = rand.nextInt(totalV);

		int t2 = rand.nextInt(INFRA.getAvalVmTypeNum());
		for (int i = 0; i < totalV; i++)
			coding.ins2type[i] = t2;

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
	public int tasksNum;
	private int evalCount;
	public List<Task> tasks;
	public Map<Integer, Double> lstTaskExp;

	private DAG dag;
	// private String name;

	public static boolean isSameSolution(Solution a, Solution b) {
		VmEncoding left = (VmEncoding) a.getDecisionVariables()[0];
		VmEncoding right = (VmEncoding) b.getDecisionVariables()[0];

		for (int i = 0; i < left.task2ins.length; i++)
			if (left.task2ins[i] != right.task2ins[i] || left.taskInOrder[i] != right.taskInOrder[i]
					|| left.ins2type[i] != right.ins2type[i])
				return false;

		return true;
	}

	public void setSolTask2Ins(Solution sol, int[] task2ins) {
		VmEncoding v = (VmEncoding) sol.getDecisionVariables()[0];
		for (int i = 0; i < this.tasksNum; i++)
			v.task2ins[i] = task2ins[i];
	}

	public void setSolIns2Type(Solution sol, int[] ins2type) {
		VmEncoding v = (VmEncoding) sol.getDecisionVariables()[0];
		for (int i = 0; i < this.tasksNum; i++)
			v.ins2type[i] = ins2type[i];
	}

	public void setSolTaskInOrder(Solution sol, int[] taskInOrder) {
		VmEncoding v = (VmEncoding) sol.getDecisionVariables()[0];
		for (int i = 0; i < this.tasksNum; i++)
			v.taskInOrder[i] = taskInOrder[i];
	}

	public Solution deepCopySol(Solution sol) throws ClassNotFoundException {
		Solution x = new Solution(this);
		x.setDecisionVariables(new Variable[] { sol.getDecisionVariables()[0].deepCopy() });
		return x;
	}

	public static VmEncoding fetchSolDecs(Solution sol) {
		return (VmEncoding) sol.getDecisionVariables()[0];
	}

	@SuppressWarnings("unchecked")
	public VmsProblem(String dataset, Random rand) {
		this.rand = rand;
		this.problemName_ = dataset;

		Object[] tmpInfo = INFRA.getCaseCloudlets(this.problemName_, 0);
		tasks = (List<Task>) tmpInfo[0];
		tasksNum = tasks.size();
		dag = (DAG) (tmpInfo[1]);

		this.numberOfVariables_ = 1; // TODO ATTENTION
		this.numberOfObjectives_ = 2; // TODO change this if needed

		this.solutionType_ = new VMScheduleSolutionType(this, rand);
		this.lstTaskExp = new HashMap<Integer, Double>();

		this.evalCount = 0;
	}

	@Override
	public int getNumberOfVariables() {
		System.err.println("really need this?");
		System.exit(-1);
		return tasksNum;
	};

	@Override
	public void evaluate(Solution solution) {
		evalCount += 1;
		// if (evalCount % 100 == 0 || evalCount == 1) {
		// System.out.println(
		// "[VmsP] Time -- " + System.currentTimeMillis() / 1000 % 100000 + "
		// Eval # so far : " + evalCount);
		// }

		Log.disable();

		Variable[] decs = solution.getDecisionVariables();
		int[] order = ((VmEncoding) decs[0]).taskInOrder;
		int[] task2ins = ((VmEncoding) decs[0]).task2ins;
		int[] ins2type = ((VmEncoding) decs[0]).ins2type;

		// System.out.println(Arrays.toString(order));
		// System.out.println(Arrays.toString(task2ins));
		// System.out.println(Arrays.toString(ins2type));

		// reset cloudlet to factory configurations
		for (Task c : tasks)
			c.setCloudletFinishedSoFar(0L);

		dag.rmCache();

		// Create vm list
		List<Vm> vmlist = new ArrayList<Vm>();
		vmlist = INFRA.createVms(task2ins, ins2type);

		// map task to vm
		for (int var = 0; var < tasksNum; var++) {
			if (task2ins[var] == -1) {
				dag.totalCloudletNum -= 1;
				continue;
			}
			tasks.get(var).setVmId(vmlist.get(task2ins[var]).getId());
		}

		if (!Ints.contains(task2ins, -1))
			dag.calcFileTransferTimes(task2ins, vmlist, tasks);
		else
			dag.clearFileTransferTimes(tasks);

		// binding global workflow to vm
		vmlist.removeAll(Collections.singleton(null)); // remove null

		DAGCentralSimulator cloudSim = new DAGCentralSimulator();

		// binding dag to simulator
		cloudSim.setCloudletDAG(dag);
		// broker.submitVmList(vmlist);
		cloudSim.setVmList(vmlist);

		// submit cloudletList according to order
		// broker.submitCloudletList(tmp);
		for (int i : order) {
			if (task2ins[i] != -1)
				cloudSim.taskSubmit(tasks.get(i), dag.fileTransferTime.get(tasks.get(i)));
		}

		cloudSim.boot();
		List<Task> revList = new ArrayList<Task>();
		this.lstTaskExp.clear();
		for (Task i : tasks) {
			if (i.getStatus() == Cloudlet.SUCCESS) {
				revList.add(i);
				lstTaskExp.put(i.id, i.getFinishTime());
			}
		}

		MyCloudSimHelper.printCloudletList(revList);

		if (revList.size() != dag.totalCloudletNum) {
			System.err.println("[VmsP] can not simulating all cloudlets!");
			System.err.println("left # = " + (dag.totalCloudletNum - revList.size()));
			MyCloudSimHelper.forcePrintCloudList(revList);
			System.exit(-1);
			return;
		}

		// MyCloudSimHelper.forcePrintCloudList(revList);
		// calculating objectives
		double makespan = 0;
		for (Task c : revList) {
			makespan = Math.max(makespan, c.getFinishTime());
		}

		// calculating cost
		double cost = 0;
		for (Vm v : vmlist) {
			double start = Double.MAX_VALUE;
			double end = -Double.MAX_VALUE;
			for (Task c : revList) {
				if (c.getVmId() != v.getId())
					continue;
				start = Double.min(start, c.getExecStartTime());
				end = Double.max(start, c.getFinishTime());
			}
			if (end - start > 0)
				cost += INFRA.getUnitPrice(v) * Math.ceil((end - start) / 3600);
		}

		// System.out.printf("%.1fs with $%.3f\n", makespan, cost);
		solution.setObjective(0, makespan);
		solution.setObjective(1, cost);
		long s7 = System.currentTimeMillis();
		// System.err.println(System.currentTimeMillis() - s1);
	}

	public int randInt(int bound) {
		return this.rand.nextInt(bound);
	}

	public DAG getDAG() {
		return dag;
	}

	public static void printSolution(Solution s) {
		Variable[] decs = s.getDecisionVariables();

		int[] order = ((VmEncoding) decs[0]).taskInOrder;
		int[] task2ins = ((VmEncoding) decs[0]).task2ins;
		int[] ins2type = ((VmEncoding) decs[0]).ins2type;

		System.out.println("order " + Arrays.toString(order));
		System.out.println("insta " + Arrays.toString(task2ins));
		System.out.println("type_ " + Arrays.toString(ins2type));
		System.out.println("------");
		System.out.println();
	}

	public static void main(String[] args) throws ClassNotFoundException, JMException {
		VmsProblem p = new VmsProblem("sci_CyberShake_30", new Random());
		for (int i = 0; i < 40; i++) {
			Solution sol = new Solution(p);
			p.evaluate(sol);
			System.out.print(sol.getObjective(0) + " ");
			System.out.println(sol.getObjective(1));
		}
	}
}