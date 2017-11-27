package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import edu.ncsu.model.DAG;
import edu.ncsu.model.Task;
import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

/**
 * 
 * @author jianfeng
 *
 */

class AlgRiot extends Algorithm {
	private static final long serialVersionUID = 7491441804538378626L;
	private VmsProblem problem_;
	private Random rand_;

	public AlgRiot(Problem problem) {
		super(problem);
	}

	private List<Task> findCritical() {
		DAG graph = problem_.getDAG();

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
		DAG graph = problem_.getDAG();
		List<Task> criticalTasks = findCritical();
		List<Task> ref = graph.randTopo(problem_.tasks, rand_);

		int[] taskInOrder = bRankOrder();

		Set<Integer> cache = new HashSet<Integer>();
		SolutionSet frame = new SolutionSet(100000);
		Map<Task, Double> p = new HashMap<Task, Double>();

		for (double p0 = 0.0; p0 <= 1.0; p0 += 0.05) {
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

			for (int px = 0; px < 1; px++) {
				int[] ins2type = new int[problem_.tasksNum];
				Solution sol = new Solution(problem_);
				problem_.setSolTask2Ins(sol, task2ins);
				for (int j = 0; j < ins2type.length; j++)
					ins2type[j] = px;
				problem_.setSolIns2Type(sol, ins2type);
				problem_.setSolTaskInOrder(sol, taskInOrder);
				problem_.evaluate(sol);
				frame.add(sol);
			}
		} // for p0
		return frame;
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		problem_ = (VmsProblem) this.getInputParameter("VmsProblem");
		rand_ = new Random((Long) this.getInputParameter("seed"));
		SolutionSet frame = this.decomposite();
		SolutionSet p = null;
		switch ((String) this.getInputParameter("variant")) {
		case "org": // method proposed in our paper
			p = InsTypeCalc.objGuessing(rand_, problem_, frame);
			break;
		case "hc": // hill climbing
			p = InsTypeCalc.hillClimb(rand_, problem_, frame);
			break;
		default:
			System.err.println("Invalid input " + this.getInputParameter("variant"));
			System.exit(-1);
		}
		p.printObjectives();
		return p;
	}
}

public class RIOT {
	private VmsProblem problem_;

	public SolutionSet executeRIOT(HashMap<String, Object> paras) throws ClassNotFoundException, JMException {
		return executeRIOT((String) paras.get("dataset"), //
				(long) paras.get("seed"), //
				(String) paras.get("variant"));
	}

	public SolutionSet executeRIOT(String dataset, long seed, String variant)
			throws ClassNotFoundException, JMException {
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		this.problem_ = new VmsProblem(dataset, new Random(seed));
		Algorithm alg = new AlgRiot(problem_);
		alg.setInputParameter("VmsProblem", problem_);
		alg.setInputParameter("dataset", dataset);
		alg.setInputParameter("seed", seed);
		alg.setInputParameter("variant", variant);
		SolutionSet p = alg.execute();
		return p;
	}

	/*
	 * This is a DEMOSTRATION of EMSC algorithm. To do experiments, go to
	 * edu.ncsu.experiments
	 */
	public static void main(String[] args) throws JMException, ClassNotFoundException {
		for (String model : new String[] { "sci_CyberShake_100" }) {
			HashMap<String, Object> paras = new HashMap<String, Object>();
			paras.put("dataset", model);
			paras.put("seed", System.currentTimeMillis());
			paras.put("variant", "org");
			long start_time = System.currentTimeMillis();
			RIOT runner = new RIOT();
			runner.executeRIOT(paras);
			System.out.println("EXEC TIME = " + (System.currentTimeMillis() - start_time) / 1000);
		}
	}
}