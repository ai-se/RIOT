package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.cloudbus.cloudsim.Vm;

import com.google.common.primitives.Ints;

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
import jmetal.util.Ranking;
import jmetal.util.comparators.DominanceComparator;

/**
 * 
 * @author jianfeng
 *
 */

class AlgSway extends Algorithm {
	private static final long serialVersionUID = 7491441804538378626L;

	public AlgSway(Problem problem) {
		super(problem);
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		System.out.println("Framework works");
		for (int i = 0; i < 100; i++)
			;
		return null;
	}

}

public class SWAY {
	private VmsProblem problem_;
	private Random rand_;
	private String dataset_;

	public SolutionSet executeSWAY(HashMap<String, Object> paras) throws ClassNotFoundException, JMException {
		return executeSWAY((String) paras.get("dataset"), //
				(long) paras.get("seed"));
	}

	public SolutionSet executeSWAY(String dataset, long seed) throws ClassNotFoundException, JMException {
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		this.dataset_ = (String) dataset;
		this.problem_ = new VmsProblem(dataset, new Random(seed));
		this.rand_ = new Random(seed);
		Algorithm alg = new AlgSway(problem_);

		// TODO SWAY algorithm configurations
		// SolutionSet p = alg.execute();
		SolutionSet firstStage = decomposite();

		return better(firstStage);
	}

	private SolutionSet neighbors(Solution s, int genCount) throws ClassNotFoundException {
		SolutionSet neighbors = new SolutionSet(genCount);
		int[] orgConfig = VmsProblem.fetchSolDecs(s).ins2type;
		int varN = Ints.max(VmsProblem.fetchSolDecs(s).task2ins) + 1;

		for (int repeat = 0; repeat < genCount; repeat++) {
			Solution x = problem_.deepCopySol(s);
			int[] c1 = VmsProblem.fetchSolDecs(x).ins2type;

			for (int var = 0; var < varN; var++) {
				c1[var] = orgConfig[var] + rand_.nextInt(3) - 1;
				c1[var] = Math.max(0, Math.min(7, c1[var]));
			}
			problem_.evaluate(x);
			neighbors.add(x);
		} // for repeat

		return neighbors;
	}

	private int[] tillEnd(Solution org, Solution s) {
		int varN = Ints.max(VmsProblem.fetchSolDecs(org).task2ins) + 1;
		int[] res = new int[problem_.tasksNum];
		int[] delta = new int[varN];
		int[] orgc = VmsProblem.fetchSolDecs(org).ins2type;
		int[] sc = VmsProblem.fetchSolDecs(s).ins2type;

		for (int i = 0; i < varN; i++)
			delta[i] = sc[i] - orgc[i];

		System.arraycopy(orgc, 0, res, 0, varN);

		int factor = 0;
		while (true) {
			boolean success = true;
			for (int i = 0; i < varN; i++)
				if (delta[i] * factor + orgc[i] > 7 || delta[i] * factor + orgc[i] < 0) {
					success = false;
					break;
				}

			if (!success)
				break;

			factor += 1;
		}

		if (factor != 0)
			for (int i = 0; i < varN; i++)
				res[i] = delta[i] * (factor - 1) + orgc[i];

		return res;
	}

	private int[] medianVec(int[] left, int[] right, int vn) {
		int[] res = new int[left.length];
		boolean same = true;
		for (int i = 0; i < vn; i++) {
			res[i] = (left[i] + right[i]) / 2;
			if (res[i] != left[i])
				same = false;
		}

		if (same) {
			return null;
		}
		return res;
	}

	private List<Task> findCritical() {
		DAG graph = problem_.getWorkflow();

		// 1. find critical tasks
		Set<Integer> ins = new TreeSet<Integer>();
		Set<Integer> outs = new TreeSet<Integer>();

		for (int i = 0; i < problem_.tasksNum; i++) {
			ins.add(graph.meRequires(problem_.tasks.get(i)).size());
			outs.add(graph.meContributeTo(problem_.tasks.get(i)).size());
		}

		int in_t = (int) ins.toArray()[ins.size() / 3 * 2 + 1];
		int out_t = (int) outs.toArray()[outs.size() / 3 * 2 + 1];
		// System.exit(-1);
		List<Task> criticalTasks = new ArrayList<Task>();
		for (Task t : problem_.tasks) {
			if (graph.meContributeTo(t).size() >= out_t || graph.meRequires(t).size() >= in_t
					|| graph.meRequires(t).size() == 0)
				criticalTasks.add(t);
		}

		List<Task> ref = graph.randTopo(problem_.tasks, rand_);
		criticalTasks.sort((i, j) -> ref.indexOf(i) - ref.indexOf(j));

		return criticalTasks;
	}

	private int[] bRankOrder() {
		// 1.5 make order first. basing on b-rank
		Map<Task, Double> rank = MOHEFTcore.bRank(problem_);
		List<Task> sortedCloudlets = new ArrayList<Task>();
		for (Task t : problem_.tasks)
			sortedCloudlets.add(t);
		Collections.shuffle(sortedCloudlets, rand_);
		Collections.sort(sortedCloudlets, (a, b) -> rank.get(a).compareTo(rank.get(b)));
		int[] taskInOrder = new int[problem_.tasksNum];
		for (int i = 0; i < problem_.tasksNum; i++)
			taskInOrder[i] = sortedCloudlets.indexOf(problem_.tasks.get(i));

		return taskInOrder;
	}

	private SolutionSet decomposite() throws ClassNotFoundException {
		DAG graph = problem_.getWorkflow();
		List<Task> criticalTasks = findCritical();
		List<Task> ref = graph.randTopo(problem_.tasks, rand_);

		int[] taskInOrder = bRankOrder();

		Set<Integer> cache = new HashSet<Integer>();
		SolutionSet frame = new SolutionSet(100000);
		Map<Task, Double> p = new HashMap<Task, Double>();

		List<String> tips = new ArrayList<String>();

		for (double p0 = 0.0; p0 <= 1.1; p0 += 0.05) {
			p.clear();
			for (Task ct : criticalTasks)
				p.put(ct, 1.0);
			for (Task c : ref) {
				if (p.containsKey(c)) // CT
					continue;
				else { // average of prec tasks
					List<Task> preds = graph.meRequires(c);
					double s = 0.0;
					for (Task i : preds)
						s += p.get(i);
					p.put(c, s / preds.size() * p0);
				}
			}

			// 3 assignment...
			int assingedIns = 0;
			Map<Task, Integer> toIns = new HashMap<Task, Integer>();
			for (Task c : ref) {
				double tmd = rand_.nextDouble();
				if (tmd < p.get(c)) { // CT/~CT p:new
					toIns.put(c, assingedIns++);
				} else if (criticalTasks.contains(c)) { // CT (1-p):use old
					toIns.put(c, rand_.nextInt(assingedIns + 1));
				} else { // ~CT (1-p): inherit from parents
					List<Task> tmp = graph.meRequires(c);
					Task inheritFrom = tmp.get(rand_.nextInt(tmp.size()));
					toIns.put(c, toIns.get(inheritFrom));
				}
			}

			int[] task2ins = new int[problem_.tasksNum];
			int i = 0;
			for (Task c : problem_.tasks)
				task2ins[i++] = toIns.get(c);

			if (!cache.add(Arrays.hashCode(task2ins))) { // if repeated
				continue;
			}

			// System.err.println(Ints.max(task2ins));
			for (int px = 0; px < 1; px++) {
				int[] ins2type = new int[problem_.tasksNum];
				Solution sol = new Solution(problem_);
				problem_.setSolTask2Ins(sol, task2ins);
				for (int j = 0; j < ins2type.length; j++)
					ins2type[j] = px;
				problem_.setSolIns2Type(sol, ins2type);
				problem_.setSolTaskInOrder(sol, taskInOrder);
				tips.add(Ints.max(task2ins) + " :@0@" + px);
				problem_.evaluate(sol);
				frame.add(sol);
			}
		} // for p0

		return frame;
	}

	private SolutionSet better(SolutionSet frame) throws ClassNotFoundException {
		SolutionSet res = new NonDominatedSolutionList();
		SolutionSet anchors = new SolutionSet(40);
		SolutionSet randoms = new SolutionSet(500);
		int vms = INFRA.getAvalVmTypeNum();
		
		for(int diaI = 0; diaI < frame.size(); diaI++){
			anchors.clear();
			randoms.clear();
			// creating anchors
			Solution org = frame.get(diaI);
			int[] order= problem_.fetchSolDecs(org).taskInOrder;
			int[] task2ins = problem_.fetchSolDecs(org).task2ins;
			
			// case 1 iso
			for (int inst = 0; inst < INFRA.getAvalVmTypeNum(); inst++){
				Solution iso = new Solution(problem_);
				problem_.setSolTask2Ins(iso, task2ins);
				problem_.setSolTaskInOrder(iso, order);
				int[] ins2type = new int[problem_.tasksNum];
				for(int tmpi =0; tmpi <= Ints.max(task2ins); tmpi++)
					ins2type[tmpi] = inst;
				problem_.setSolIns2Type(iso, ins2type);
				
				anchors.add(iso);
			}
			
			// case 2 random assignment
			for (int i = 0;i < 20; i++){
				Solution rnd = new Solution(problem_);
				problem_.setSolTask2Ins(rnd, task2ins);
				problem_.setSolTaskInOrder(rnd, order);
				int[] ins2type = new int[problem_.tasksNum];
				for(int tmpi =0; tmpi <= Ints.max(task2ins); tmpi++)
					ins2type[tmpi] = rand_.nextInt(vms);
				problem_.setSolIns2Type(rnd, ins2type);
				
				anchors.add(rnd);
			}
		}
		
		
		return null;
		// System.err.println(frame.size());
		// DominanceComparator cmpr = new DominanceComparator();
		// SolutionSet betterFound = new SolutionSet(10000);
		//
		// for (int diaI = 0; diaI < frame.size(); diaI++) {
		// Solution org = frame.get(diaI);
		// betterFound.add(org);
		// SolutionSet neis = neighbors(org, 5);
		// // problem_.evaluateSet(neis);
		// Iterator<Solution> iter = neis.iterator();
		// int vn = Ints.max(VmsProblem.fetchSolDecs(org).task2ins) + 1;
		// while (iter.hasNext()) {
		// Solution tmps = iter.next();
		// if (cmpr.compare(org, tmps) > 0) {
		// // // binary searching
		// int[] right = tillEnd(org, tmps);
		// int[] left = VmsProblem.fetchSolDecs(org).ins2type;
		// while (true) {
		// int[] med = medianVec(left, right, vn);
		// if (med == null) {
		// break;
		// }
		// problem_.setSolIns2Type(tmps, med);
		// if (cmpr.compare(tmps, org) == -1) {
		// left = med;
		// // System.out.print("left->");
		// } else {
		// right = med;
		// // System.out.print("right->");
		// }
		// }
		// betterFound.add(tmps);
		// } // if cmpr
		// } // while iter
		// } // for diaI
		//
		// // System.err.println(betterFound.size());
		// // System.out.println("====");
		// // betterFound.printObjectives();
		// Ranking rnk = new Ranking(betterFound);
		// SolutionSet first = rnk.getSubfront(0);
		// int stillin = 0;
		// for (int i = 0; i < frame.size(); i++)
		// for(int j = 0; j < first.size(); j++)
		// if(frame.get(i) == first.get(j)){
		// stillin += 1;
		// break;
		// }
		// System.err.println("Frame size = " + frame.size());
		// System.err.println("Still in = " + stillin + "|| new found = " +
		// (first.size() - stillin));
		// return betterFound;
	}

	/**
	 * This is a DEMOSTRATION of EMSC algorithm. To do experiments, go to
	 * edu.ncsu.experiments
	 */
	public static void main(String[] args) throws JMException, ClassNotFoundException {
		// for (String model : INFRA.models) {
		// System.out.println(model);
		for (String model : new String[] { "sci_CyberShake_100" }) {
			HashMap<String, Object> paras = new HashMap<String, Object>();
			paras.put("dataset", model);
			paras.put("seed", System.currentTimeMillis());
			long start_time = System.currentTimeMillis();
			SWAY runner = new SWAY();
			runner.executeSWAY(paras);
			System.out.println("EXEC TIME = " + (System.currentTimeMillis() - start_time) / 1000);
		}

	}
}