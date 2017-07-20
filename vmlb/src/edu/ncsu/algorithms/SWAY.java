package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import edu.ncsu.wls.DAG;
import edu.ncsu.wls.Task;
import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
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
		decomposite(problem_);
		return null;
	}

	private void decomposite(VmsProblem problem) {
		DAG graph = problem.getWorkflow();

		// 1. find critical tasks
		Set<Integer> ins = new TreeSet<Integer>();
		Set<Integer> outs = new TreeSet<Integer>();

		for (int i = 0; i < problem.tasksNum; i++) {
			ins.add(graph.meRequires(problem.getTasks().get(i)).size());
			outs.add(graph.meContributeTo(problem.getTasks().get(i)).size());
		}

		int in_t = (int) ins.toArray()[ins.size() / 2 + 1];
		int out_t = (int) outs.toArray()[outs.size() / 2 + 1];

		List<Task> criticalTasks = new ArrayList<Task>();
		for (Task t : problem.getTasks()) {
			if (graph.meContributeTo(t).size() >= out_t || graph.meRequires(t).size() >= in_t)
				criticalTasks.add(t);
		}

		List<Task> ref = graph.randTopo(problem.getTasks(), rand_);
		criticalTasks.sort((i, j) -> ref.indexOf(i) - ref.indexOf(j));

		// 2. grouping
		
	}

	/**
	 * This is a DEMOSTRATION of EMSC algorithm. To do experiments, go to
	 * edu.ncsu.experiments
	 */
	public static void main(String[] args) throws JMException, ClassNotFoundException {
		// for (String model : INFRA.models) {
		// System.out.println(model);
		for (String model : new String[] { "sci_Sipht_100" }) {
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
