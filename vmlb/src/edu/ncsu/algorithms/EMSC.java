package edu.ncsu.algorithms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import jmetal.core.Algorithm;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.metaheuristics.nsgaII.NSGAII;
import jmetal.operators.crossover.Crossover;
import jmetal.operators.mutation.Mutation;
import jmetal.operators.selection.Selection;
import jmetal.operators.selection.SelectionFactory;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

/**
 * This file implements the following paper Zhu et al. Evolutionary
 * multi-objective workflow scheduling in cloud (IEEE Tran on Parallel and
 * Distributed Systems 2016)
 * 
 * @author jianfeng
 *
 */

class ZhuCrossover extends Crossover {
	private static final long serialVersionUID = 8147831803149141403L;
	private double crossoverProbability_;
	private double randomChangeProbability_;

	public ZhuCrossover(HashMap<String, Object> parameters) {
		super(parameters);
		crossoverProbability_ = (Double) parameters.get("probability");
		randomChangeProbability_ = (Double) parameters.get("randomChangeProbability");
	}

	private void debugs(Solution s) {
		Variable[] decs = s.getDecisionVariables();
		int varLength = decs.length;

		int[] order = new int[varLength];
		int[] task2ins = new int[varLength];
		int[] ins2type = new int[varLength];

		for (int v = 0; v < varLength; v++) {
			VmEncoding var = (VmEncoding) decs[v];
			order[v] = var.getOrder();
			task2ins[v] = var.getTask2ins();
			ins2type[v] = var.getIns2type();
		}
		System.out.println(Arrays.toString(order));
		System.out.println(Arrays.toString(task2ins));
		System.out.println(Arrays.toString(ins2type));
		System.out.println("------");
		System.out.println();

	}

	/*
	 * Implements sect 4.3.1 in Zhu's paper ATTENTION: in this code repo, the
	 * order can be randomly shuffle, regardless of the workflow-- the
	 * underlining implement will reorder s.t. it can fit the workflow
	 * automatically
	 * 
	 * @see jmetal.core.Operator#execute(java.lang.Object)
	 */
	@Override
	public Object execute(Object object) throws JMException {
		Solution[] parents = (Solution[]) object;
		Solution[] offspring = new Solution[2];

		offspring[0] = new Solution(parents[0]);
		offspring[1] = new Solution(parents[1]);
		// debugs(parents[0]);
		// debugs(offspring[0]);
		// debugs(offspring[1]);

		if (PseudoRandom.randDouble() < crossoverProbability_) {
			int varLength = parents[0].getDecisionVariables().length;
			int cxPoint = PseudoRandom.randInt(1, varLength - 2);

			// 1st step, common single point crossover
			for (int var = 0; var <= cxPoint; var++) {
				swap((VmEncoding) offspring[0].getDecisionVariables()[var],
						(VmEncoding) offspring[1].getDecisionVariables()[var]);
			}
			// debugs(offspring[0]);
			// debugs(offspring[1]);
			// 2nd, handle conflict ins2type. See fig 6 in Zhu's paper
			VmEncoding[] var0 = new VmEncoding[varLength];
			VmEncoding[] var1 = new VmEncoding[varLength];
			int[] instance0 = new int[varLength];
			int[] instance1 = new int[varLength];
			int[] otypes0 = new int[varLength];
			int[] otypes1 = new int[varLength];

			for (int i = 0; i < varLength; i++) {
				var0[i] = (VmEncoding) offspring[0].getDecisionVariables()[i];
				var1[i] = (VmEncoding) offspring[1].getDecisionVariables()[i];

				instance0[i] = var0[i].getTask2ins();
				instance1[i] = var1[i].getTask2ins();

				otypes0[i] = var0[i].getIns2type();
				otypes1[i] = var1[i].getIns2type();
			}

			// debugs(offspring[0]);
			// debugs(offspring[1]);

			// 2.1 DecideType for offspring[0]
			for (int var = 0; var <= cxPoint; var++) {
				int nins = instance0[var];
				int i = contains(instance0, cxPoint + 1, nins);
				if (i == -1) { // just switch
					if (PseudoRandom.randDouble() > randomChangeProbability_)
						var0[nins].setIns2Type(otypes1[nins]);
					else // random choice
						var0[nins].setIns2Type(otypes1[PseudoRandom.randInt(0, varLength - 1)]);
				} else { // random choice
					int s = PseudoRandom.randDouble() < 0.5 ? otypes0[nins] : otypes1[nins];
					var0[nins].setIns2Type(s);
				}
			}

			// debugs(offspring[0]);
			// debugs(offspring[1]);

			// 2.2 DecideType for offspring[1]
			for (int var = 0; var <= cxPoint; var++) {
				int nins = instance1[var];
				int i = contains(instance1, cxPoint + 1, nins);
				if (i == -1) {
					if (PseudoRandom.randDouble() > randomChangeProbability_)
						var1[nins].setIns2Type(otypes0[nins]);
					else // set type randomly
						var1[nins].setIns2Type(otypes0[PseudoRandom.randInt(0, varLength - 1)]);
				} else { // random choice
					int s = PseudoRandom.randDouble() < 0.5 ? otypes1[nins] : otypes0[nins];
					var1[nins].setIns2Type(s);
				} // if i== -1
			} // for 2.2

			// debugs(offspring[0]);
			// debugs(offspring[1]);
		} // if rand double
		return offspring;
	}

	/**
	 * Swap the VALUES or two VmEncoding object. including --order,-- task2ins
	 * (NO ins2type!!)
	 * 
	 * @param s
	 * @param t
	 */
	private void swap(VmEncoding s, VmEncoding t) {
		// int tmp_s_order = s.getOrder();
		int tmp_s_task2ins = s.getTask2ins();

		// s.setOrder(t.getOrder());
		s.setTask2ins(t.getTask2ins());

		// t.setOrder(tmp_s_order);
		t.setTask2ins(tmp_s_task2ins);
	}

	/**
	 * Find target in list l[startSearchAt:end]
	 * 
	 * @param l
	 * @param startSearchAt
	 * @param target
	 * @return Index of target if found, -1 if not found
	 */
	private int contains(int[] l, int startSearchAt, int target) {
		for (int i = startSearchAt; i < l.length; i++)
			if (l[i] == target)
				return i;
		return -1;
	}
}

class ZhuMutation extends Mutation {
	private static final long serialVersionUID = 3956833626877416282L;
	private Double mutationProbability_ = null;

	public ZhuMutation(HashMap<String, Object> parameters) {
		super(parameters);
		mutationProbability_ = (Double) parameters.get("probability");
	}

	private void debugs(Solution s) {
		Variable[] decs = s.getDecisionVariables();
		int varLength = decs.length;

		int[] order = new int[varLength];
		int[] task2ins = new int[varLength];
		int[] ins2type = new int[varLength];

		for (int v = 0; v < varLength; v++) {
			VmEncoding var = (VmEncoding) decs[v];
			order[v] = var.getOrder();
			task2ins[v] = var.getTask2ins();
			ins2type[v] = var.getIns2type();
		}
		System.out.println(Arrays.toString(order));
		System.out.println(Arrays.toString(task2ins));
		System.out.println(Arrays.toString(ins2type));
		System.out.println("------");
		System.out.println();

	}

	@Override
	public Object execute(Object object) throws JMException {
		Solution solution = (Solution) object;
		// if (PseudoRandom.randDouble() < mutationProbability_) {
		// // 1, fetch configurations
		// Variable[] decs = solution.getDecisionVariables();
		// int varLength = decs.length;
		//
		// int[] order = new int[varLength];
		// int[] task2ins = new int[varLength];
		// int[] ins2type = new int[varLength];
		//
		// for (int v = 0; v < varLength; v++) {
		// VmEncoding var = (VmEncoding) decs[v];
		// order[v] = var.getOrder();
		// task2ins[v] = var.getTask2ins();
		// ins2type[v] = var.getIns2type();
		// }
		//
		// // 2, mutate the orders
		//// System.out.println(Arrays.toString(order));
		// }

		debugs(solution);
		return solution;
	}

}

public class EMSC {
	public static void main(String[] args) throws JMException, ClassNotFoundException {
		VmsProblem problem_ = new VmsProblem("j30", new Random(322));
		Algorithm alg = new NSGAII(problem_);
		HashMap<String, Object> parameters = new HashMap<String, Object>();

		alg.setInputParameter("populationSize", 16); //WARNING MUST BE EVEN
		alg.setInputParameter("maxEvaluations", 100);

		parameters.clear();
		parameters.put("probability", 0.6);
		parameters.put("randomChangeProbability", 0.1);
		Crossover crossover = new ZhuCrossover(parameters);

		parameters.clear();
		parameters.put("probability", 0.8);
		Mutation mutation = new ZhuMutation(parameters);

		parameters = null;
		Selection selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);

		alg.addOperator("crossover", crossover);
		alg.addOperator("mutation", mutation);
		alg.addOperator("selection", selection);

		SolutionSet p = alg.execute();
		for (int v = 0; v < p.size(); v++) {
			System.out.println(p.get(v).getObjective(0) + " " + p.get(v).getObjective(1));

		}
	}
}