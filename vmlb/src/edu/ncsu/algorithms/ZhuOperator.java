package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.ncsu.model.DAG;
import edu.ncsu.model.INFRA;
import edu.ncsu.model.Task;
import jmetal.core.Solution;
import jmetal.operators.crossover.Crossover;
import jmetal.operators.mutation.Mutation;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

class ZhuCrossover extends Crossover {
	private static final long serialVersionUID = 8147831803149141403L;
	private double crossoverProbability_;
	private double randomChangeProbability_;
	private VmsProblem problem_;

	public ZhuCrossover(HashMap<String, Object> parameters) {
		super(parameters);
		crossoverProbability_ = (Double) parameters.get("probability");
		randomChangeProbability_ = (Double) parameters.get("randomChangeProbability");
		problem_ = (VmsProblem) parameters.get("problem");
	}

	/*
	 * Implements sect 4.3.1 in Zhu's paper
	 * 
	 * @see jmetal.core.Operator#execute(java.lang.Object)
	 */
	@Override
	public Object execute(Object object) throws JMException {
		Solution[] parents = (Solution[]) object;
		Solution[] o = new Solution[2];

		try {
			o[0] = new Solution(problem_);
			o[1] = new Solution(problem_);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		if (PseudoRandom.randDouble() < crossoverProbability_) {
			int varLength = ((VmEncoding) parents[0].getDecisionVariables()[0]).ins2type.length;
			int cxPoint = PseudoRandom.randInt(1, varLength - 2);

			// 1, common single point crossover
			VmEncoding left = (VmEncoding) parents[0].getDecisionVariables()[0];
			VmEncoding right = (VmEncoding) parents[0].getDecisionVariables()[0];
			VmEncoding o0 = ((VmEncoding) o[0].getDecisionVariables()[0]);
			VmEncoding o1 = ((VmEncoding) o[0].getDecisionVariables()[0]);

			// 2, crossover order
			List<Integer> aa = new ArrayList<Integer>();
			List<Integer> bb = new ArrayList<Integer>();

			for (int i = 0; i < cxPoint; i++) {
				aa.add(right.taskInOrder[i]);
				bb.add(left.taskInOrder[i]);
			}

			for (int i : left.taskInOrder)
				if (!aa.contains(i))
					aa.add(i);

			for (int i : right.taskInOrder)
				if (!bb.contains(i))
					bb.add(i);
			for (int i = 0; i < varLength; i++) {
				o0.taskInOrder[i] = aa.get(i);
				o1.taskInOrder[i] = bb.get(i);
			}

			// 3, swap task2ins
			for (int i = 0; i <= cxPoint; i++) {
				o0.task2ins[i] = right.task2ins[i];
				o1.task2ins[i] = left.task2ins[i];
			}

			for (int i = cxPoint + 1; i < varLength; i++) {
				o0.task2ins[i] = left.task2ins[i];
				o1.task2ins[i] = right.task2ins[i];
			}

			// 4.0 Copy ins2type
			for (int i = 0; i < varLength; i++) {
				o0.ins2type[i] = left.ins2type[i];
				o1.ins2type[i] = right.ins2type[i];
			}
			// 4.1 DecideType for offspring[0]
			for (int var = 0; var <= cxPoint; var++) {
				int nins = right.task2ins[var];
				int i = contains(left.task2ins, cxPoint + 1, nins);
				if (i == -1) { // just switch
					if (PseudoRandom.randDouble() > randomChangeProbability_)
						o0.ins2type[nins] = right.ins2type[nins];
					else // random choice
						o0.ins2type[nins] = PseudoRandom.randInt(0, INFRA.getAvalVmTypeNum() - 1);
				} else { // random choice
					int s = PseudoRandom.randDouble() < 0.5 ? left.ins2type[nins] : right.ins2type[nins];
					o0.ins2type[nins] = s;
				}
			}

			// 4.2 DecideType for offspring[1]
			for (int var = 0; var <= cxPoint; var++) {
				int nins = left.task2ins[var];
				int i = contains(right.task2ins, cxPoint + 1, nins);
				if (i == -1) {
					if (PseudoRandom.randDouble() > randomChangeProbability_)
						o1.ins2type[nins] = left.ins2type[nins];
					else // set type randomly
						o1.ins2type[nins] = PseudoRandom.randInt(0, INFRA.getAvalVmTypeNum() - 1);
				} else { // random choice
					int s = PseudoRandom.randDouble() < 0.5 ? left.ins2type[nins] : right.ins2type[nins];
					o1.ins2type[nins] = s;
				} // if i== -1
			} // for 4.2
		} // if rand double
		return o;
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
	private VmsProblem problem_;

	public ZhuMutation(HashMap<String, Object> parameters) {
		super(parameters);
		mutationProbability_ = (double) parameters.get("probability");
		bitMutationProbability_ = (double) parameters.get("bitMutationProbability");
		problem_ = (VmsProblem) parameters.get("problem");
	}

	@Override
	public Object execute(Object object) throws JMException {
		Solution solution = (Solution) object;
		VmEncoding code = (VmEncoding) solution.getDecisionVariables()[0];
		int varLength = code.taskInOrder.length;
		List<Task> tasks = problem_.tasks;
		DAG dag = problem_.getDAG();

		for (int pos = 0; pos < varLength; pos++) {
			if (PseudoRandom.randDouble() < mutationProbability_)
				continue;
			// 1, fetch configurations 
			int[] order = code.taskInOrder;
			// 2, mutate the orders.
			// int pos = PseudoRandom.randInt(0, varLength - 1);
			int start = -1;
			int end = varLength;

			for (int i = 0; i < problem_.tasksNum; i++) {
				if (dag.isEdge(tasks.get(order[i]), tasks.get(order[pos])))
					start = Integer.max(start, i);
				if (dag.isEdge(tasks.get(order[i]), tasks.get(order[pos])))
					end = Integer.min(end, i);
			}

			start += 1;
			end -= 1;
			int to = pos;

			if (start < end)
				to = PseudoRandom.randInt(start, end);

			if (to < pos) {
				int t = order[pos];
				for (int i = pos; i > to; i--)
					code.taskInOrder[i] = order[i - 1];
				code.taskInOrder[to] = t;
			} else if (to > pos) {
				int t = order[pos];
				for (int i = pos; i < to; i++)
					code.taskInOrder[i] = order[i + 1];
				code.taskInOrder[to] = t;
			}

			// 3, mutate task2ins and ins2type
			int maxIns = varLength - 1;
			int maxType = INFRA.getAvalVmTypeNum() - 1;
			for (int v = 0; v < varLength; v++) {
				if (PseudoRandom.randDouble() < bitMutationProbability_)
					code.task2ins[pos] = PseudoRandom.randInt(0, maxIns);
				if (PseudoRandom.randDouble() < bitMutationProbability_)
					code.ins2type[pos] = PseudoRandom.randInt(0, maxType);
			}
			// }
		}

		// solution.setDecisionVariables(new Variable[] { code });
		return solution;
	}
}

public class ZhuOperator {
	// THIS CLASS WAS INTENTIONALLY LEFT BLANK.
}