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

import com.google.common.primitives.Ints;

import edu.ncsu.model.DAG;
import edu.ncsu.model.INFRA;
import edu.ncsu.model.Task;
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

class AlgRiot extends Algorithm {
	private static final long serialVersionUID = 7491441804538378626L;

	public AlgRiot(Problem problem) {
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

public class RIOT {
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
		Algorithm alg = new AlgRiot(problem_);

		// TODO SWAY algorithm configurations
		// SolutionSet p = alg.execute();
		SolutionSet firstStage = decomposite();

		return better(firstStage);
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

		for (int diaI = 0; diaI < frame.size(); diaI++) {
			anchors.clear();
			randoms.clear();

			// creating anchors
			Solution org = frame.get(diaI);
			int[] order = VmsProblem.fetchSolDecs(org).taskInOrder;
			int[] task2ins = VmsProblem.fetchSolDecs(org).task2ins;

			// case 1 iso
			for (int inst = 0; inst < INFRA.getAvalVmTypeNum(); inst++) {
				Solution iso = new Solution(problem_);
				problem_.setSolTask2Ins(iso, task2ins);
				problem_.setSolTaskInOrder(iso, order);
				int[] ins2type = new int[problem_.tasksNum];
				for (int tmpi = 0; tmpi <= Ints.max(task2ins); tmpi++)
					ins2type[tmpi] = inst;
				problem_.setSolIns2Type(iso, ins2type);

				anchors.add(iso);
			}

			// case 2 random assignment
			for (int i = 0; i < 20; i++) {
				Solution rnd = new Solution(problem_);
				problem_.setSolTask2Ins(rnd, task2ins);
				problem_.setSolTaskInOrder(rnd, order);
				int[] ins2type = new int[problem_.tasksNum];
				for (int tmpi = 0; tmpi <= Ints.max(task2ins); tmpi++)
					ins2type[tmpi] = rand_.nextInt(vms);
				problem_.setSolIns2Type(rnd, ins2type);

				anchors.add(rnd);
			}

			// evaluating anchors
			for (int i = 0; i < anchors.size(); i++)
				problem_.evaluate(anchors.get(i));

			// creating randoms
			for (int i = 0; i < 200; i++) {
				Solution rnd = new Solution(problem_);
				problem_.setSolTask2Ins(rnd, task2ins);
				problem_.setSolTaskInOrder(rnd, order);
				int[] ins2type = new int[problem_.tasksNum];
				for (int tmpi = 0; tmpi <= Ints.max(task2ins); tmpi++)
					ins2type[tmpi] = rand_.nextInt(vms);
				problem_.setSolIns2Type(rnd, ins2type);

				randoms.add(rnd);
			}

			// convenient. save anchors settings
			int[][] AC = new int[anchors.size()][Ints.max(task2ins) + 1];
			for (int i = 0; i < anchors.size(); i++) {
				int[] tmp = VmsProblem.fetchSolDecs(anchors.get(i)).ins2type;
				for (int j = 0; j < Ints.max(task2ins) + 1; j++) {
					AC[i][j] = tmp[j];
				}
			}

			// estimation
			for (int i = 0; i < randoms.size(); i++) {
				Solution rnd = randoms.get(i);
				Solution shortest = null, furthest = null;
				int s = Integer.MAX_VALUE;
				int S = -1;

				int[] myc = VmsProblem.fetchSolDecs(rnd).ins2type;
				for (int j = 0; j < anchors.size(); j++) {
					int dd = dist(AC[j], myc);
					if (dd < s) {
						s = dd;
						shortest = anchors.get(j);
					}
					if (dd > S) {
						S = dd;
						furthest = anchors.get(j);
					}
				}

				// fast estimate objectives
				float fact = dist(VmsProblem.fetchSolDecs(shortest).ins2type, myc)
						/ dist(VmsProblem.fetchSolDecs(furthest).ins2type, myc);
				if (!sign(myc, shortest, furthest))
					fact *= -1;

				rnd.setObjective(0,
						shortest.getObjective(0) + fact * (furthest.getObjective(0) - shortest.getObjective(0)));
				rnd.setObjective(1,
						shortest.getObjective(1) + fact * (furthest.getObjective(1) - shortest.getObjective(1)));
			} // for i in randoms

			Ranking rnk = new Ranking(randoms.union(anchors));
			SolutionSet ests = rnk.getSubfront(0);
			for (int i = 0; i < ests.size(); i++) {
				problem_.evaluate(ests.get(i));
				res.add(ests.get(i));
			}
		}

		return res;
	}

	private boolean sign(int[] C, Solution n, Solution f) {
		int[] nf = new int[C.length];
		int[] nc = new int[C.length];

		int[] n_confg = VmsProblem.fetchSolDecs(n).ins2type;
		int[] f_confg = VmsProblem.fetchSolDecs(f).ins2type;

		for (int i = 0; i < C.length; i++) {
			nf[i] = f_confg[i] - n_confg[i];
			nc[i] = C[i] - n_confg[i];
		}

		int r = 0;
		for (int i = 0; i < C.length; i++)
			r += nf[i] * nc[i];

		return r >= 0;
	}

	private int dist(int[] a, int[] b) {
		int res = 0;
		for (int i = 0; i < Ints.min(a.length, b.length); i++)
			res += Math.abs(a[i] - b[i]);
		return res;
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
			paras.put("seed", System.currentTimeMillis());
			long start_time = System.currentTimeMillis();
			RIOT runner = new RIOT();
			runner.executeSWAY(paras);
			System.out.println("EXEC TIME = " + (System.currentTimeMillis() - start_time) / 1000);
		}

	}
}