package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import edu.ncsu.wls.CloudletDAG;
import edu.ncsu.wls.Infrastructure;
import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
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
	private CloudletDAG dag;
	private int maxSimuIns;
	public int[] vmTypes;
	private double[] unitPrice;
	private double[] vmFinishTime;
	public int usedVM;
	private Map<Cloudlet, Integer> assingedTo;
	private Map<Cloudlet, Double> singleCloudletFinishedTime;
	public double makespan, cost;

	public HEFTScheduler(CloudletDAG dag, int maxSimultaneousIns) {
		this.dag = dag;
		this.maxSimuIns = maxSimultaneousIns;
		usedVM = 0;
		assingedTo = new HashMap<Cloudlet, Integer>();
		singleCloudletFinishedTime = new HashMap<Cloudlet, Double>();
		makespan = -1;
		cost = -1;

		vmTypes = new int[maxSimultaneousIns];
		vmFinishTime = new double[maxSimultaneousIns];
		unitPrice = new double[maxSimultaneousIns];

		for (int i = 0; i < maxSimultaneousIns; i++) {
			vmTypes[i] = -1;
			vmFinishTime[i] = 0.0;
			unitPrice[i] = 0.0;
		}
	}

	/**
	 * Must be completely assigned
	 * 
	 * @param problem
	 * @param orderedCloudlets
	 * @return
	 */
	public Solution translate(VmsProblem problem, List<Cloudlet> orderedCloudlets) {
		Solution sol = null;
		try {
			sol = new Solution(problem);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// set orders
		List<Cloudlet> problem_cloudlets = problem.getCloudletList2();
		for (int i = 0; i < problem_cloudlets.size(); i++) {
			((VmEncoding) (sol.getDecisionVariables()[i])).setOrder(orderedCloudlets.indexOf(problem_cloudlets.get(i)));
		}

		// set task2ins
		for (int i = 0; i < problem_cloudlets.size(); i++) {
			((VmEncoding) (sol.getDecisionVariables()[i])).setTask2ins(assingedTo.get(problem_cloudlets.get(i)));
		}

		// set ins2type
		for (int i = 0; i < vmTypes.length; i++) {
			((VmEncoding) (sol.getDecisionVariables()[i])).setIns2Type(vmTypes[i]);
		}

		return sol;
	}

	public boolean appendToExistVM(Cloudlet cl, int assignVmI, double expExecTime) {
		if (assignVmI >= usedVM)
			return false;

		assingedTo.put(cl, assignVmI);

		double earliestStart = 0;
		for (Cloudlet requirement : dag.meRequires(cl)) {
			earliestStart = Double.max(earliestStart, singleCloudletFinishedTime.get(requirement));
		}

		earliestStart = Double.max(earliestStart, vmFinishTime[assignVmI]);

		singleCloudletFinishedTime.put(cl, earliestStart + expExecTime);
		vmFinishTime[assignVmI] = earliestStart + expExecTime;

		makespan = -1;
		cost = -1;

		return true;
	}

	public boolean useNewVm(Cloudlet cl, int type, double vmUnitPrice, double expExecTime) {
		if (usedVM >= maxSimuIns)
			return false;

		usedVM += 1;
		vmTypes[usedVM - 1] = type;
		unitPrice[usedVM - 1] = vmUnitPrice;

		assingedTo.put(cl, usedVM - 1);

		double earliestStart = 0;
		for (Cloudlet requirement : dag.meRequires(cl)) {
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
		this.cost = makespan * totalUnitPrice / 3600;

		return new double[] { this.makespan, this.cost };
	}

	public double getMakespan() {
		return this.makespan;
	}

	public String toString() {
		return this.makespan + " " + this.cost;
	}

	public HEFTScheduler clone() {
		HEFTScheduler res = new HEFTScheduler(dag, maxSimuIns);
		res.vmTypes = this.vmTypes.clone();
		res.unitPrice = this.unitPrice.clone();
		res.usedVM = this.usedVM;
		res.vmFinishTime = this.vmFinishTime.clone();
		res.assingedTo = new HashMap<>();
		res.makespan = makespan;
		res.cost = cost;
		res.assingedTo = new HashMap<>();
		res.singleCloudletFinishedTime = new HashMap<>();

		for (Cloudlet cl : this.singleCloudletFinishedTime.keySet()) {
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
	private Map<Cloudlet, Integer> bRank(VmsProblem p) {
		List<Cloudlet> cloudlets = p.getCloudletList2();
		CloudletDAG cp = p.getWorkflow();

		Map<Cloudlet, Integer> upwardRank = new HashMap<Cloudlet, Integer>();

		int dep = 0;
		int seted = 0;

		for (Cloudlet c : cloudlets) {
			if (!cp.hasPred(c)) {
				upwardRank.put(c, 0);
				seted += 1;
			} else
				upwardRank.put(c, -1);
		}

		while (seted < cp.totalCloudletNum) {
			dep += 1;
			for (Cloudlet c : cloudlets) {
				if (upwardRank.get(c) != -1)
					continue;
				boolean ready = true;
				for (Cloudlet r : cp.getRequiring().get(c)) {
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

		return upwardRank;
	}

	private double unitPrice(Vm v) {
		List<Vm> tmp = new ArrayList<Vm>();
		tmp.add(v);
		return Infrastructure.getUnitPrice(tmp);
	}

	private List<HEFTScheduler> pruneNextPlans(List<HEFTScheduler> nextPlans, int finalNum) {
		List<HEFTScheduler> res = new ArrayList<HEFTScheduler>();
		List<HEFTScheduler> left = new ArrayList<HEFTScheduler>();

		int prevSize = 0;
		while (res.size() < finalNum) {
			prevSize = res.size();
			for (HEFTScheduler i : nextPlans) {
				for (HEFTScheduler j : nextPlans) {
					if (j.makespan < i.makespan && j.cost < i.cost) {
						left.add(i);
					} // if dominated
				} // for j
			} // for i

			nextPlans.removeAll(left);
			res.addAll(nextPlans);
			nextPlans.clear();
			nextPlans.addAll(left);
			left.clear();
		}

		while (res.size() > finalNum) { // crowd sorting
			List<HEFTScheduler> toPrune = res.subList(prevSize, res.size());
			toPrune.sort(Comparator.comparing(HEFTScheduler::getMakespan));

			double dist[] = new double[toPrune.size()];
			dist[0] = Double.MAX_VALUE;
			dist[toPrune.size() - 1] = Double.MAX_VALUE;

			double makespanDelta = Math.abs(toPrune.get(0).makespan - toPrune.get(toPrune.size() - 1).makespan);
			double costDelta = Math.abs(toPrune.get(toPrune.size() - 1).cost - toPrune.get(0).cost);

			// calc dist
			for (int i = 1; i < dist.length - 1; i++) {
				HEFTScheduler lhs = toPrune.get(i - 1);
				HEFTScheduler rhs = toPrune.get(i + 1);
				dist[i] = (Math.abs(lhs.makespan - rhs.makespan) / makespanDelta * Math.abs(lhs.cost - rhs.cost)
						/ costDelta);
			}

			// sort toPrune by dist
			Object[] toPruneObj = toPrune.toArray();
			Integer[] indices = new Integer[dist.length];
			for (int i = 0; i < dist.length; i++)
				indices[i] = i;

			Arrays.sort(indices, new Comparator<Integer>() {
				@Override
				public int compare(final Integer o1, final Integer o2) {
					return -Double.compare(dist[o1], dist[o2]);
				}
			});

			// removing uncessary res
			int pp = finalNum - prevSize;
			res.removeAll(toPrune);
			for (int i = 0; i < pp; i++) {
				res.add((HEFTScheduler) toPruneObj[indices[i]]);
			}
		}

		return res;
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// 0. Set ups
		int k = ((Integer) getInputParameter("K")).intValue(); // tradeOffSolNum
		int n = ((Integer) getInputParameter("N")).intValue(); // maxSimultaneousIns
		List<Vm> avalVmTypes = Infrastructure.createVms(0);

		HEFTScheduler[] frontier = new HEFTScheduler[k];
		VmsProblem problem = (VmsProblem) problem_;

		for (int i = 0; i < k; i++)
			frontier[i] = new HEFTScheduler(problem.getWorkflow(), n);

		// 1. Calc lookup table
		double[][] expTime = new double[problem_.getNumberOfVariables()][avalVmTypes.size()];
		for (int c = 0; c < problem_.getNumberOfVariables(); c++)
			for (int v = 0; v < avalVmTypes.size(); v++) {
				expTime[c][v] = problem.getCloudletList().get(c).getCloudletLength() / avalVmTypes.get(v).getMips();

			}

		// 2. B-Rank
		Map<Cloudlet, Integer> rank = this.bRank(problem);

		// 3. Sort cloudlets with b-rabk
		List<Cloudlet> unsortedCloudlets = problem.getCloudletList2();
		List<Cloudlet> sortedCloudlets = problem.getCloudletList2();
		Collections.shuffle(sortedCloudlets, new Random(PseudoRandom.randInt()));
		Collections.sort(sortedCloudlets, (Cloudlet one, Cloudlet other) -> {
			return rank.get(one).compareTo(rank.get(other));
		});

		for (Cloudlet adding : sortedCloudlets) {
			int leftCNum = sortedCloudlets.size() - sortedCloudlets.indexOf(adding);
			if (leftCNum % 10 == 0)
				System.out.println("try to assign " + adding + " left# " + leftCNum);
			List<HEFTScheduler> nextPlans = new ArrayList<>();
			for (HEFTScheduler f : frontier) {
				for (int i = 0; i < f.usedVM; i++) { // reusing exist vm
					HEFTScheduler next = f.clone();
					boolean succeed = next.appendToExistVM(adding, i,
							expTime[unsortedCloudlets.indexOf(adding)][f.vmTypes[i]]);
					if (succeed) {
						next.getCurrentObj();
						nextPlans.add(next);
					}
				}

				for (int v = 0; v < avalVmTypes.size(); v++) { // using new vm
					HEFTScheduler next = f.clone();
					boolean succeed = next.useNewVm(adding, v, unitPrice(avalVmTypes.get(v)),
							expTime[unsortedCloudlets.indexOf(adding)][v]);
					if (succeed) {
						next.getCurrentObj();
						nextPlans.add(next);
					}
				}
			} // for f in frontier

			nextPlans = this.pruneNextPlans(nextPlans, k);
			for (int i = 0; i < k; i++)
				frontier[i] = nextPlans.get(i);

		}

		SolutionSet finalOptRes = new SolutionSet(k);

		for (HEFTScheduler f : frontier) {
			Solution x = f.translate(problem, sortedCloudlets);
			problem.evaluate(x);
			finalOptRes.add(x);
		}

		return finalOptRes;
	}

}

public class MOHEFT {
	public SolutionSet execMOHEFT(HashMap<String, Object> para) throws ClassNotFoundException, JMException {
		return execMOHEFT((String) para.get("dataset"), //
				(int) para.get("tradeOffSolNum"), //
				(int) para.get("maxSimultaneousIns"), //
				(long) para.get("seed"));
	}

	public SolutionSet execMOHEFT(String dataset, int tradeOffSolNum, int maxSimultaneousIns, long seed)
			throws ClassNotFoundException, JMException {
		VmsProblem problem_ = new VmsProblem(dataset, new Random(seed));
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		Algorithm alg = new MOHEFTcore(problem_);

		alg.setInputParameter("K", tradeOffSolNum);
		alg.setInputParameter("N", maxSimultaneousIns);
		SolutionSet p = alg.execute();

		// for (int v = 0; v < p.size(); v++) {
		// System.out.println(p.get(v).getObjective(0) + " " +
		// p.get(v).getObjective(1));
		// problem_.printSolution(p.get(v));
		// }

		return p;
	}

	public static void main(String[] args) throws ClassNotFoundException, JMException {
		HashMap<String, Object> paras = new HashMap<String, Object>();
		paras.put("dataset", "sci_Inspiral_30");
		paras.put("seed", System.currentTimeMillis());
		paras.put("tradeOffSolNum", 10);
		paras.put("maxSimultaneousIns", 10);

		MOHEFT runner = new MOHEFT();
		runner.execMOHEFT(paras);
	}
}