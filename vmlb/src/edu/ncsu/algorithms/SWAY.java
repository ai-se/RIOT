package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

	public SolutionSet executeSWAY(HashMap<String, Object> paras) throws ClassNotFoundException, JMException {
		return executeSWAY((String) paras.get("dataset"), //
				(long) paras.get("seed"));
	}

	public SolutionSet executeSWAY(String dataset, long seed) throws ClassNotFoundException, JMException {
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		this.problem_ = new VmsProblem(dataset, new Random(seed));
		this.rand_ = new Random(seed);
		Algorithm alg = new AlgSway(problem_);

		// TODO SWAY algorithm configurations
		// SolutionSet p = alg.execute();
		decomposite();
		return null;
	}

	private void decomposite() throws ClassNotFoundException {
		DAG graph = problem_.getWorkflow();
		Iterator<Solution> iter;

		// 1. find critical tasks
		Set<Integer> ins = new TreeSet<Integer>();
		Set<Integer> outs = new TreeSet<Integer>();

		for (int i = 0; i < problem_.tasksNum; i++) {
			ins.add(graph.meRequires(problem_.tasks.get(i)).size());
			outs.add(graph.meContributeTo(problem_.tasks.get(i)).size());
		}

		int in_t = (int) ins.toArray()[ins.size() / 2 + 1];
		int out_t = (int) outs.toArray()[outs.size() / 2 + 1];

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
		SolutionSet frame = new SolutionSet(1000);
		// SolutionSet frame = new NonDominatedSolutionList();
		for (double p0 = 0.1; p0 <= 0.5; p0 += 0.05) {
			Map<Task, Double> p = new HashMap<Task, Double>();
			p.clear();
			for (Task ct : criticalTasks)
				p.put(ct, p0);
			for (Task c : ref) { // CT
				if (p.containsKey(c))
					continue;
				else { // average of prec tasks
					List<Task> preds = graph.meRequires(c);
					double s = 0.0;
					for (Task i : preds)
						s += p.get(i);
					p.put(c, s / preds.size() * p0);
				}
			}

			SolutionSet smallSet = new NonDominatedSolutionList();

			for (int repeat = 0; repeat < 10; repeat++) {
				// 3 assignment...
				int assingedIns = 0;
				Map<Task, Integer> toIns = new HashMap<Task, Integer>();
				for (Task c : ref) {
					if (rand_.nextDouble() < p.get(c)) { // CT/~CT p:new
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
				int[] ins2type = new int[problem_.tasksNum];
				for (int j = 0; j < ins2type.length; j++) {
					ins2type[j] = 7;
				}
				int i = 0;
				for (Task c : problem_.tasks)
					task2ins[i++] = toIns.get(c);

				if (!cache.add(Arrays.hashCode(task2ins))) // just avoid repeats
					continue;
				Solution sol = new Solution(problem_);
				problem_.setSolTask2Ins(sol, task2ins);
				problem_.setSolIns2Type(sol, ins2type);
				problem_.setSolTaskInOrder(sol, taskInOrder);
				problem_.evaluate(sol);
				smallSet.add(sol);
			} // for repeat

			// add small pareto front to frame
			iter = smallSet.iterator();
			while (iter.hasNext())
				frame.add(iter.next());

		}

		System.err.println(frame.size());
		iter = frame.iterator();
		while (iter.hasNext()) {
			Solution c = iter.next();
			int[] t = VmsProblem.fetchSolDecs(c).task2ins;
			System.out.println(Ints.max(t) + 1 + " " + Arrays.toString(t));
		}

		// 3. most important. sway to set ins2type
		
	}

	/**
	 * This is a DEMOSTRATION of EMSC algorithm. To do experiments, go to
	 * edu.ncsu.experiments
	 */
	public static void main(String[] args) throws JMException, ClassNotFoundException {
		// for (String model : INFRA.models) {
		// System.out.println(model);
		for (String model : new String[] { "sci_CyberShake_30" }) {
			HashMap<String, Object> paras = new HashMap<String, Object>();
			paras.put("dataset", model);
			paras.put("seed", 12345L);
			long start_time = System.currentTimeMillis();
			SWAY runner = new SWAY();
			runner.executeSWAY(paras);
			System.out.println("EXEC TIME = " + (System.currentTimeMillis() - start_time) / 1000);
		}

	}
}
