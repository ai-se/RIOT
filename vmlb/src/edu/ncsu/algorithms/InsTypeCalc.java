package edu.ncsu.algorithms;

import java.util.Arrays;
import java.util.Random;

import com.google.common.primitives.Ints;

import edu.ncsu.model.INFRA;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.NonDominatedSolutionList;
import jmetal.util.Ranking;
import jmetal.util.comparators.DominanceComparator;

/**
 * 
 * @author jianfeng
 *
 */

public class InsTypeCalc {
	/**
	 * Strategy1:: RIOT paper proposed. similar to sway.
	 */
	public static SolutionSet objGuessing(Random rand_, VmsProblem problem_, SolutionSet frame)
			throws ClassNotFoundException {
		SolutionSet res = new NonDominatedSolutionList();
		SolutionSet anchors = new SolutionSet(40);
		SolutionSet randoms = new SolutionSet(500);
		int vms = INFRA.getAvalVmTypeNum();

		for (int diaI = 0; diaI < frame.size(); diaI++) {
			anchors.clear();
			randoms.clear();

			Solution org = frame.get(diaI);
			int[] order = VmsProblem.fetchSolDecs(org).taskInOrder;
			int[] task2ins = VmsProblem.fetchSolDecs(org).task2ins;

			// case 1 random assignment
			for (int i = 0; i < 30; i++) {
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

			// creating random
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
					int dd = dist(getVector(AC[j], myc));
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
				int[] NP = getVector(shortest, rnd);
				int[] NF = getVector(shortest, furthest);
				int d_NF = dist(NF);
				float fact = dotProduct(NP, NF) / (d_NF * d_NF);

				for (int o : new int[] { 0, 1 }) {
					double o_N = shortest.getObjective(o);
					double o_F = furthest.getObjective(o);
					rnd.setObjective(o, o_N - fact * (o_N - o_F));
				}

			} // for i in random

			Ranking rnk = new Ranking(randoms.union(anchors));
			SolutionSet ests = rnk.getSubfront(0);
			for (int i = 0; i < ests.size(); i++) {
				problem_.evaluate(ests.get(i));
				res.add(ests.get(i));
			}

		}

		return res;
	}

	private static int[] getVector(int[] A, int B[]) {
		int l = Math.min(A.length, B.length);
		int[] res = Arrays.copyOf(B, l);
		for (int i = 0; i < res.length; i++)
			res[i] -= A[i];
		return res;
	}

	private static int[] getVector(Solution sol1, Solution sol2) {
		int[] A = VmsProblem.fetchSolDecs(sol1).ins2type;
		int[] B = VmsProblem.fetchSolDecs(sol2).ins2type;
		int[] res = Arrays.copyOf(B, B.length);
		for (int i = 0; i < res.length; i++)
			res[i] -= A[i];
		return res;
	}

	/*
	 * Using Euclidean distance
	 */
	private static int dist(int[] a) {
		int res = 0;
		for (int i : a)
			res += i * i;
		return (int) Math.sqrt(res);
	}

	/*
	 * Dot Product of two vector
	 */
	private static int dotProduct(int[] a, int[] b) {
		int res = 0;
		for (int i = 0; i < a.length; i++)
			res += a[i] * b[i];
		return res;
	}

	/**
	 * Strategy 2: Hill Climbing
	 */
	public static SolutionSet hillClimb(Random rand_, VmsProblem problem_, SolutionSet frame)
			throws ClassNotFoundException {
		DominanceComparator cmpr = new DominanceComparator();
		SolutionSet res = new NonDominatedSolutionList();
		int aval_vms = INFRA.getAvalVmTypeNum();

		for (int diaI = 0; diaI < frame.size(); diaI++) {
			Solution best = problem_.deepCopySol(frame.get(diaI));
			Solution candidate = problem_.deepCopySol(best);
			problem_.evaluate(best);
			int vms = Ints.max(VmsProblem.fetchSolDecs(best).task2ins);

			int iter = 0;
			int torr = 0;
			while (iter < 50 && torr < 15) {
				// Randomly create new neighbor
				VmsProblem.fetchSolDecs(candidate).ins2type[rand_.nextInt(vms + 1)] = rand_.nextInt(aval_vms);
				problem_.evaluate(candidate);

				if (cmpr.compare(candidate, best) == -1) {
					problem_.setSolIns2Type(best, VmsProblem.fetchSolDecs(candidate).ins2type);
					best.setObjective(0, candidate.getObjective(0));
					best.setObjective(1, candidate.getObjective(1));
				} else
					torr += 1;
				iter += 1;
			}
			res.add(best);
		}
		return res;
	}
}
