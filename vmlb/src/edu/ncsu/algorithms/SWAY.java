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

import com.google.common.primitives.Ints;

import edu.ncsu.wls.DAG;
import edu.ncsu.wls.Task;
import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.NonDominatedSolutionList;
import jmetal.util.PseudoRandom;
import jmetal.util.Ranking;

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
		return decomposite();
		// return null;
	}

	private SolutionSet tinyNSGAII() throws JMException, ClassNotFoundException {
		HashMap<String, Object> paras = new HashMap<String, Object>();
		paras.put("algorithm", "NSGAII");
		paras.put("dataset", dataset_);
		paras.put("seed", rand_.nextLong());
		paras.put("popSize", 50);
		paras.put("maxEval", 250);
		paras.put("cxProb", 1.0);
		paras.put("cxRandChangeProb", 0.6);
		paras.put("mutProb", 0.6);
		paras.put("bitMutProb", 1.0);
		EMSC runner = new EMSC();
		return runner.execute(paras);
	}

	private SolutionSet decomposite() throws ClassNotFoundException {
		DAG graph = problem_.getWorkflow();
		Iterator<Solution> iter;

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

		// 2. idea 102
		Set<Integer> cache = new HashSet<Integer>();
		SolutionSet frame = new SolutionSet(100000);
		// SolutionSet frame = new NonDominatedSolutionList();
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

			SolutionSet small = new NonDominatedSolutionList();
			small.clear();

			for (int px = 0; px < 8; px++) {
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
				small.add(sol);
			}

			// System.out.println(small.size() + " " + Ints.max(task2ins));
			// small.printObjectives();
//			boolean updated = true;
//			while (updated) {
//				updated = false;
				for (int rep = 0; rep < 30; rep++) {
					Solution r1 = small.get(rand_.nextInt(small.size()));
					Solution r2 = small.get(rand_.nextInt(small.size()));
					Solution sol = new Solution(problem_);
					problem_.setSolTask2Ins(sol, task2ins);
					problem_.setSolIns2Type(sol, hybrid(r1, r2));
					problem_.setSolTaskInOrder(sol, taskInOrder);
					problem_.evaluate(sol);
					// tips.add(Ints.max(task2ins) + " :@2@");
					frame.add(sol);
					small.add(sol);
//				}
			}

			// System.exit(-1);
			// for (int px = 0; px < 500; px++) {
			// int[] ins2type = new int[problem_.tasksNum];
			// Solution sol = new Solution(problem_);
			// problem_.setSolTask2Ins(sol, task2ins);
			// for (int j = 0; j < ins2type.length; j++)
			// ins2type[j] = rand_.nextInt(8);
			// problem_.setSolIns2Type(sol, ins2type);
			// problem_.setSolTaskInOrder(sol, taskInOrder);
			// problem_.evaluate(sol);
			// tips.add(Ints.max(task2ins) + " :@2@");
			// frame.add(sol);
			// }
		} // for p0

		System.out.println("====");

		Ranking ranks = new Ranking(frame);
		SolutionSet eep = ranks.getSubfront(0);
		eep.sort((s1, s2) -> (int) (((Solution) s1).getObjective(0) - ((Solution) s2).getObjective(0)));

		// for (int j = 0; j < eep.size(); j++) {
		// for (int i = 0; i < frame.size(); i++) {
		// if (eep.get(j) == frame.get(i))
		// System.out.println(tips.get(i));
		// }
		// }

		System.out.println("====");
		eep.printObjectives();

		return eep;
	}

	private int[] hybrid(Solution s1, Solution s2) {
		int[] a1 = VmsProblem.fetchSolDecs(s1).ins2type;
		int[] a2 = VmsProblem.fetchSolDecs(s2).ins2type;
		int[] res = new int[a1.length];

		for (int i = 0; i < res.length; i++) {
			res[i] = rand_.nextDouble() < 0.5 ? a1[i] : a2[i];
		}

		return res;
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