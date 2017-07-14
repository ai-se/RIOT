package edu.ncsu.algorithms;

import java.util.HashMap;

import com.google.common.primitives.Ints;

import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.operators.crossover.Crossover;
import jmetal.operators.mutation.Mutation;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

class ZhuCrossover extends Crossover {
	private static final long serialVersionUID = 8147831803149141403L;
	private double crossoverProbability_;
	private double randomChangeProbability_;

	public ZhuCrossover(HashMap<String, Object> parameters) {
		super(parameters);
		crossoverProbability_ = (Double) parameters.get("probability");
		randomChangeProbability_ = (Double) parameters.get("randomChangeProbability");
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

		if (PseudoRandom.randDouble() < crossoverProbability_) {
			int varLength = ((VmEncoding) parents[0].getDecisionVariables()[0]).ins2type.length;
			int cxPoint = PseudoRandom.randInt(1, varLength - 2);

			// 1st step, common single point crossover
			VmEncoding left = (VmEncoding) offspring[0].getDecisionVariables()[0];
			VmEncoding right = (VmEncoding) offspring[0].getDecisionVariables()[0];

			for (int var = 0; var <= cxPoint; var++) {
				swap(left, right, var);
			}

			// 2nd, handle conflict ins2type. See fig 6 in Zhu's paper
			int[] instance0 = new int[varLength];
			int[] instance1 = new int[varLength];
			int[] otypes0 = new int[varLength];
			int[] otypes1 = new int[varLength];

			System.arraycopy(left.task2ins, 0, instance0, 0, instance0.length);
			System.arraycopy(right.task2ins, 0, instance1, 0, instance1.length);
			System.arraycopy(left.ins2type, 0, otypes0, 0, otypes0.length);
			System.arraycopy(right.ins2type, 0, otypes1, 0, otypes1.length);

			// 2.1 DecideType for offspring[0]
			for (int var = 0; var <= cxPoint; var++) {
				int nins = instance0[var];
				int i = contains(instance0, cxPoint + 1, nins);
				if (i == -1) { // just switch
					if (PseudoRandom.randDouble() > randomChangeProbability_)
						left.ins2type[nins] = otypes1[nins];
					else // random choice
						left.ins2type[nins] = otypes1[PseudoRandom.randInt(0, varLength - 1)];
				} else { // random choice
					int s = PseudoRandom.randDouble() < 0.5 ? otypes0[nins] : otypes1[nins];
					left.ins2type[nins] = s;
				}
			}

			// 2.2 DecideType for offspring[1]
			for (int var = 0; var <= cxPoint; var++) {
				int nins = instance1[var];
				int i = contains(instance1, cxPoint + 1, nins);
				if (i == -1) {
					if (PseudoRandom.randDouble() > randomChangeProbability_)
						right.ins2type[nins] = otypes0[nins];
					else // set type randomly
						right.ins2type[nins] = otypes0[PseudoRandom.randInt(0, varLength - 1)];
				} else { // random choice
					int s = PseudoRandom.randDouble() < 0.5 ? otypes1[nins] : otypes0[nins];
					right.ins2type[nins] = s;
				} // if i== -1
			} // for 2.2

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
	private void swap(VmEncoding s, VmEncoding t, int index) {
		// int tmp_s_order = s.getOrder();
		int tmp_s_task2ins = s.task2ins[index];

		// s.setOrder(t.getOrder());
		s.task2ins[index] = t.task2ins[index];

		// t.setOrder(tmp_s_order);
		t.task2ins[index] = tmp_s_task2ins;
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
	private double mutationProbability_;
	private double bitMutationProbability_;

	public ZhuMutation(HashMap<String, Object> parameters) {
		super(parameters);
		mutationProbability_ = (double) parameters.get("probability");
		bitMutationProbability_ = (double) parameters.get("bitMutationProbability");
	}

	@Override
	public Object execute(Object object) throws JMException {
		Solution solution = (Solution) object;
		if (PseudoRandom.randDouble() < mutationProbability_) {
			// 1, fetch configurations
			VmEncoding code = (VmEncoding) solution.getDecisionVariables()[0];
			int[] order = code.taskInOrder;

			// 2, mutate the orders.
			// pick two locations, swap them
			int varLength = order.length;
			int i = PseudoRandom.randInt(0, varLength - 1);
			int j = PseudoRandom.randInt(0, varLength - 1);

			int tmp = order[i];
			code.taskInOrder[i] = order[j];
			code.taskInOrder[j] = tmp;

			// 3, mutate task2ins and ins2type
			int maxIns = Ints.max(code.task2ins);
			int maxType = Ints.max(code.ins2type);
			for (int v = 0; v < varLength; v++) {
				if (PseudoRandom.randDouble() < bitMutationProbability_)
					code.task2ins[i] = PseudoRandom.randInt(0, maxIns);
				if (PseudoRandom.randDouble() < bitMutationProbability_)
					code.ins2type[i] = PseudoRandom.randInt(0, maxType);
			}
		}
		// debugs(solution);
		return solution;
	}
}

public class ZhuOperator {

}