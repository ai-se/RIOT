package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.cloudbus.cloudsim.Vm;

import edu.ncsu.wls.DAG;
import edu.ncsu.wls.INFRA;
import edu.ncsu.wls.Task;
import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.NonDominatedSolutionList;
import jmetal.util.PseudoRandom;

/**
 * This file implements the following paper Juan J. Durillo and Radu Prodan
 * Multi-objective workflow scheduling in Amazon EC2 Cluster computing 17.2
 * (2014): 169-189.
 * 
 * MOHEFT- MultiObjective Heterogeneous Earliest Finish Time
 *
 * MOHEFT is a heuristic algorithm. time complexity is O(NMK). not suitable for
 * extremely large problems. For details, see the original paper
 * 
 * @author jianfeng
 *
 */

class HEFTScheduler {
	private DAG dag;
	private int maxSimuIns;
	public int[] vmTypes;
	double[] unitPrice;
	private double[] vmFinishTime;
	public int usedVM;
	private Map<Task, Integer> assingedTo;
	private Map<Task, Double> singleCloudletFinishedTime;
	public double makespan, cost;
	public List<Task> cloudletsOrder;
	public double crowdDist; // for crowd sorting used

	public HEFTScheduler(DAG dag, int maxSimultaneousIns, List<Task> order) {
		this.dag = dag;
		this.maxSimuIns = maxSimultaneousIns;
		this.cloudletsOrder = order;

		usedVM = 0;
		assingedTo = new HashMap<Task, Integer>();
		singleCloudletFinishedTime = new HashMap<Task, Double>();
		makespan = -1;
		cost = -1;

		vmTypes = new int[maxSimultaneousIns];
		vmFinishTime = new double[maxSimultaneousIns];
		unitPrice = new double[maxSimultaneousIns];

		for (int i = 0; i < maxSimultaneousIns; i++) {
			vmTypes[i] = 0;
			vmFinishTime[i] = 0.0;
			unitPrice[i] = 0.0;
		}
	}

	/**
	 * @param problem
	 * @param orderedCloudlets
	 * @return
	 */
	public Solution translate(VmsProblem problem) {
		Solution sol = null;
		try {
			sol = new Solution(problem);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		VmEncoding code = (VmEncoding) sol.getDecisionVariables()[0];

		// set orders
		List<Task> problem_cloudlets = problem.tasks;
		for (int i = 0; i < problem_cloudlets.size(); i++) {
			code.taskInOrder[i] = cloudletsOrder.indexOf(problem_cloudlets.get(i));
		}

		// set task2ins
		for (int i = 0; i < problem_cloudlets.size(); i++) {
			if (!assingedTo.containsKey(problem_cloudlets.get(i)))
				code.task2ins[i] = -1;
			else
				code.task2ins[i] = assingedTo.get(problem_cloudlets.get(i));
		}

		// set ins2type
		for (int i = 0; i < vmTypes.length; i++) {
			code.ins2type[i] = vmTypes[i];
		}
		return sol;
	}

	public boolean appendToExistVM(Task cl, int assignVmI, double expExecTime) {
		if (assignVmI >= usedVM)
			return false;

		assingedTo.put(cl, assignVmI);

		double earliestStart = 0;
		for (Task requirement : dag.meRequires(cl)) {
			earliestStart = Double.max(earliestStart, singleCloudletFinishedTime.get(requirement));
		}

		earliestStart = Double.max(earliestStart, vmFinishTime[assignVmI]);

		singleCloudletFinishedTime.put(cl, earliestStart + expExecTime);
		vmFinishTime[assignVmI] = earliestStart + expExecTime;

		makespan = -1;
		cost = -1;

		return true;
	}

	public boolean useNewVm(Task cl, int type, double vmUnitPrice, double expExecTime) {
		if (usedVM >= maxSimuIns)
			return false;

		usedVM += 1;
		vmTypes[usedVM - 1] = type;
		unitPrice[usedVM - 1] = vmUnitPrice;

		assingedTo.put(cl, usedVM - 1);

		double earliestStart = 0;
		for (Task requirement : dag.meRequires(cl)) {
			earliestStart = Double.max(earliestStart, singleCloudletFinishedTime.get(requirement));
		}

		singleCloudletFinishedTime.put(cl, earliestStart + expExecTime);
		vmFinishTime[usedVM - 1] = earliestStart + expExecTime;

		makespan = -1;
		cost = -1;

		return true;
	}

	public double[] getCurrentObj() {
		if (makespan != -1)
			return new double[] { makespan, cost };

		double totalUnitPrice = 0;
		double makespan = 0;

		for (int i = 0; i < usedVM; i++) {
			totalUnitPrice += unitPrice[i];
			makespan = Double.max(makespan, vmFinishTime[i]);
		}

		this.makespan = makespan;
		this.cost = Math.ceil(makespan / 3600) * totalUnitPrice;

		return new double[] { this.makespan, this.cost };
	}

	public void setCurrentObj(double makespan, double cost) {
		this.makespan = makespan;
		this.cost = cost;
	}

	public double getMakespan() {
		return this.makespan;
	}

	public double getCost() {
		return this.cost;
	}

	public double getCrowdDist() {
		return this.crowdDist;
	}

	public String toString() {
		return "\n" + this.makespan + " " + this.cost;
	}

	@Override
	public boolean equals(Object other) {
		HEFTScheduler o = (HEFTScheduler) other;
		if (Math.abs(this.makespan - o.makespan) < 0.01 && Math.abs(this.cost - o.cost) < 0.001)
			return true;
		return false;
	}

	public HEFTScheduler clone() {
		HEFTScheduler res = new HEFTScheduler(dag, maxSimuIns, cloudletsOrder);
		res.vmTypes = this.vmTypes.clone();
		res.unitPrice = this.unitPrice.clone();
		res.usedVM = this.usedVM;
		res.vmFinishTime = this.vmFinishTime.clone();
		res.assingedTo = new HashMap<>();
		res.makespan = makespan;
		res.cost = cost;
		res.assingedTo = new HashMap<>();
		res.singleCloudletFinishedTime = new HashMap<>();

		for (Task cl : this.singleCloudletFinishedTime.keySet()) {
			res.assingedTo.put(cl, assingedTo.get(cl));
			res.singleCloudletFinishedTime.put(cl, singleCloudletFinishedTime.get(cl));
		}
		return res;
	}
}

class MOHEFTcore extends Algorithm {
	private static final long serialVersionUID = 6855363312229721720L;

	public MOHEFTcore(Problem problem) {
		super(problem);
	}

	/**
	 * B-Rank One time execute. no need to optimize :)
	 * 
	 * @param p
	 * @return the rank. use pointers of cloudlets
	 */
	public static Map<Task, Double> bRank(VmsProblem p) {
		List<Task> cloudlets = p.tasks;
		DAG cp = p.getWorkflow();

		Map<Task, Integer> upwardRank = new HashMap<Task, Integer>();

		int dep = 0;
		int seted = 0;

		for (Task c : cloudlets) {
			if (!cp.hasPred(c)) {
				upwardRank.put(c, 0);
				seted += 1;
			} else
				upwardRank.put(c, -1);
		}

		while (seted < cp.totalCloudletNum) {
			dep += 1;
			for (Task c : cloudlets) {
				if (upwardRank.get(c) != -1)
					continue;
				boolean ready = true;
				for (Task r : cp.getRequiring().get(c)) {
					if (upwardRank.get(r) == -1 || upwardRank.get(r) == dep) {
						ready = false;
						break;
					} // if
				} // for r
				if (ready) {
					upwardRank.put(c, dep);
					seted += 1;
				}
			}

		}

		Map<Task, Double> res = new HashMap<Task, Double>();

		// for the same rank cloudlet, one with more succeed have higher rank
		int maxDeap = Collections.max(upwardRank.values());
		List<Task> match = new ArrayList<Task>();
		for (int deap = 0; deap <= maxDeap; deap++) {
			match.clear();
			for (Entry<Task, Integer> entry : upwardRank.entrySet())
				if (entry.getValue() == deap)
					match.add(entry.getKey());

			match.sort((Task a, Task b) -> cp.meContributeTo(b).size() - (cp.meContributeTo(a).size()));

			for (Task i : match)
				res.put(i, (deap + (match.indexOf(i) + 0.0) / (match.size() + 1.0)));

		}
		return res;
	}

	private double unitPrice(Vm v) {
		List<Vm> tmp = new ArrayList<Vm>();
		tmp.add(v);
		return INFRA.getUnitPrice(tmp);
	}

	/**
	 * Sort and prune candidates with crowd distance
	 * 
	 * @param nextPlans
	 * @param finalNum
	 * @return
	 */
	private List<HEFTScheduler> pruneNextPlans(List<HEFTScheduler> nextPlans, int finalNum) {
		// step 1 remove repeat solutions
		List<HEFTScheduler> notr = new ArrayList<HEFTScheduler>();
		for (HEFTScheduler i : nextPlans) {
			boolean exist = false;
			for (HEFTScheduler j : notr)
				if (i.equals(j)) {
					exist = true;
					break;
				}
			if (!exist)
				notr.add(i);
		}
		nextPlans = notr;

		// step 2.1
		if (nextPlans.size() < finalNum) {
			return nextPlans;
		}

		// step 2.2
		int s = 0;
		int t = nextPlans.size();
		nextPlans.sort(Comparator.comparing(HEFTScheduler::getMakespan));

		nextPlans.get(s).crowdDist = Double.MAX_VALUE;
		nextPlans.get(t - 1).crowdDist = Double.MAX_VALUE;

		HEFTScheduler first = nextPlans.get(s);
		HEFTScheduler lst = nextPlans.get(t - 1);
		double makespanDelta = Math.abs(first.makespan - lst.makespan);
		double costDelta = Math.abs(first.cost - lst.cost);

		// calc dist
		for (int i = s + 1; i < t - 1; i++) {
			HEFTScheduler lhs = nextPlans.get(i - 1);
			HEFTScheduler rhs = nextPlans.get(i + 1);
			nextPlans.get(i).crowdDist = -(Math.abs(lhs.makespan - rhs.makespan) / makespanDelta
					* Math.abs(lhs.cost - rhs.cost) / costDelta);
		}

		// sort toPrune by dist
		Collections.sort(nextPlans, Comparator.comparing(HEFTScheduler::getCrowdDist));

		return nextPlans.subList(0, finalNum);
	}

	public Solution executeHEFT(Comparator<HEFTScheduler> cmpr) throws JMException, ClassNotFoundException {
		// 0. Set ups
		VmsProblem problem = (VmsProblem) problem_;
		int n = ((Integer) getInputParameter("N")).intValue(); // maxSimultaneousIns
		List<Vm> avalVmTypes = INFRA.createVms();

		// 1. B-Rank
		Map<Task, Double> rank = bRank(problem);

		// 2. Sort cloudlets with b-rank
		List<Task> sortedCloudlets = new ArrayList<Task>();
		for (Task t : problem.tasks)
			sortedCloudlets.add(t);

		Collections.sort(sortedCloudlets, (Task one, Task other) -> rank.get(one).compareTo(rank.get(other)));

		HEFTScheduler sched = new HEFTScheduler(problem.getWorkflow(), n, sortedCloudlets);

		for (Task adding : sortedCloudlets) {
			int leftCNum = sortedCloudlets.size() - sortedCloudlets.indexOf(adding);
			if (leftCNum % 10 == 0)
				System.out.println("[HEFT] try to assign " + adding + " left# " + leftCNum);

			List<HEFTScheduler> nextPlans = new ArrayList<>();
			for (int i = 0; i < sched.usedVM; i++) { // reusing exist vm
				HEFTScheduler next = sched.clone();
				boolean succeed = next.appendToExistVM(adding, i,
						adding.getCloudletLength() / avalVmTypes.get(sched.vmTypes[i]).getMips());
				if (succeed) {
					// Solution tmp = next.translate(problem);
					// problem.evaluate(tmp);
					// next.setCurrentObj(tmp.getObjective(0),
					// tmp.getObjective(1));
					next.getCurrentObj();
					nextPlans.add(next);
				}
			}

			for (int v = 0; v < avalVmTypes.size(); v++) { // using new vm
				HEFTScheduler next = sched.clone();
				boolean succeed = next.useNewVm(adding, v, unitPrice(avalVmTypes.get(v)),
						adding.getCloudletLength() / avalVmTypes.get(v).getMips());
				if (succeed) {
					// Solution tmp = next.translate(problem);
					// problem.evaluate(tmp);
					// next.setCurrentObj(tmp.getObjective(0),
					// tmp.getObjective(1));
					next.getCurrentObj();
					nextPlans.add(next);
				}
			}

			Collections.sort(nextPlans, cmpr);
			sched = nextPlans.get(0);
		}

		Solution res = sched.translate(problem);
		problem.evaluate(res);

		return res;
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// 0. Set ups
		int k = ((Integer) getInputParameter("K")).intValue(); // tradeOffSolNum
		int n = ((Integer) getInputParameter("N")).intValue(); // maxSimultaneousIns
		List<Vm> avalVmTypes = INFRA.createVms();

		List<HEFTScheduler> frontier = new ArrayList<HEFTScheduler>();
		VmsProblem problem = (VmsProblem) problem_;

		// 1. B-Rank
		Map<Task, Double> rank = bRank(problem);

		// 2. Sort cloudlets with b-rank
		List<Task> sortedCloudlets = new ArrayList<Task>();
		for (Task t : problem.tasks)
			sortedCloudlets.add(t);

		Collections.sort(sortedCloudlets, (Task one, Task other) -> rank.get(one).compareTo(rank.get(other)));

		for (int i = 0; i < k; i++)
			frontier.add(new HEFTScheduler(problem.getWorkflow(), n, sortedCloudlets));

		for (Task adding : sortedCloudlets) {
			int leftCNum = sortedCloudlets.size() - sortedCloudlets.indexOf(adding);
			if (leftCNum % 10 == 0)
				System.out.println("[MOHEFT] try to assign " + adding + " left# " + leftCNum);

			List<HEFTScheduler> nextPlans = new ArrayList<>();
			for (HEFTScheduler f : frontier) {
				for (int i = 0; i < f.usedVM; i++) { // reusing exist vm
					HEFTScheduler next = f.clone();
					boolean succeed = next.appendToExistVM(adding, i,
							adding.getCloudletLength() / avalVmTypes.get(f.vmTypes[i]).getMips());
					if (succeed) {
						Solution tmp = next.translate(problem);
						problem.evaluate(tmp);
						// next.getCurrentObj();
						next.setCurrentObj(tmp.getObjective(0), tmp.getObjective(1));
						nextPlans.add(next);
					}
				}

				for (int v = 0; v < avalVmTypes.size(); v++) { // using new vm
					HEFTScheduler next = f.clone();
					boolean succeed = next.useNewVm(adding, v, unitPrice(avalVmTypes.get(v)),
							adding.getCloudletLength() / avalVmTypes.get(v).getMips());
					if (succeed) {
						Solution tmp = next.translate(problem);
						problem.evaluate(tmp);
						// next.getCurrentObj();
						next.setCurrentObj(tmp.getObjective(0), tmp.getObjective(1));
						nextPlans.add(next);
					}
				}
			} // for f in frontier

			nextPlans = this.pruneNextPlans(nextPlans, k);
			frontier.clear();
			frontier.addAll(nextPlans);

		} // for adding

		// SolutionSet finalOptRes = new SolutionSet(k);
		SolutionSet finalOptRes = new NonDominatedSolutionList();

		for (HEFTScheduler f : frontier) {
			Solution x = f.translate(problem);
			problem.evaluate(x);
			finalOptRes.add(x);
		}

		return finalOptRes;
	}
}

public class MOHEFT {
	/* overloading */
	public SolutionSet execMOHEFT(HashMap<String, Object> para) throws ClassNotFoundException, JMException {
		return execMOHEFT((String) para.get("dataset"), //
				(int) para.get("tradeOffSolNum"), //
				(int) para.get("maxSimultaneousIns"), //
				(long) para.get("seed"));
	}

	/* overloading */
	public SolutionSet execHEFTMinExtremes(HashMap<String, Object> para) throws ClassNotFoundException, JMException {
		return execHEFTMinExtremes((String) para.get("dataset"), //
				(int) para.get("maxSimultaneousIns"), //
				(long) para.get("seed"));
	}

	public SolutionSet execMOHEFT(String dataset, int tradeOffSolNum, int maxSimultaneousIns, long seed)
			throws ClassNotFoundException, JMException {
		VmsProblem problem_ = new VmsProblem(dataset, new Random(seed));
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		Algorithm alg = new MOHEFTcore(problem_);

		alg.setInputParameter("K", tradeOffSolNum);
		alg.setInputParameter("N", Math.min(maxSimultaneousIns, problem_.tasksNum));
		if (tradeOffSolNum * maxSimultaneousIns * problem_.tasksNum > 110000) {
			System.err.println("[MOHEFT] K * N too large. Not suitable for MOHEFT algorithm!");
			return new SolutionSet(1);
		}

		SolutionSet p = alg.execute();

		return p;
	}

	public SolutionSet execHEFTMinExtremes(String dataset, int maxSimultaneousIns, long seed)
			throws ClassNotFoundException, JMException {
		VmsProblem problem_ = new VmsProblem(dataset, new Random(seed));
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		MOHEFTcore alg = new MOHEFTcore(problem_);

		alg.setInputParameter("N", Math.min(maxSimultaneousIns, problem_.tasksNum));

		SolutionSet res = new SolutionSet(2);

		Solution fastest = alg.executeHEFT(Comparator.comparing(HEFTScheduler::getMakespan));
		Solution cheapest = alg.executeHEFT(Comparator.comparing(HEFTScheduler::getCost));
		res.add(fastest);
		res.add(cheapest);

		return res;
	}

	/* demo */
	public static void main(String[] args) throws ClassNotFoundException, JMException {
		HashMap<String, Object> paras = new HashMap<String, Object>();
		paras.put("dataset", "eprotein");
		paras.put("seed", System.currentTimeMillis()); // ATTENTION: THIS ALG IS
														// DETERMINISTIC
		paras.put("tradeOffSolNum", 3);
		paras.put("maxSimultaneousIns", 3);

		MOHEFT runner = new MOHEFT();
		SolutionSet x = runner.execHEFTMinExtremes(paras);
		System.out.println(x.get(0));
		System.out.println(x.get(1));
		System.exit(0);

		// SolutionSet res = runner.execMOHEFT(paras);
		//
		// NonDominatedSolutionList r = new NonDominatedSolutionList();
		//
		// for (int i = 0; i < res.size(); i++) {
		// r.add(res.get(i));
		// System.out.printf("%.2f $%.3f\n", res.get(i).getObjective(0),
		// res.get(i).getObjective(1));
		// }
		//
		// System.out.println("---");
		// for (int i = 0; i < r.size(); i++) {
		// System.out.printf("%.2f $%.3f\n", r.get(i).getObjective(0),
		// r.get(i).getObjective(1));
		// }
	}
}