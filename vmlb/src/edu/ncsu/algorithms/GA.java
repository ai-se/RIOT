package edu.ncsu.algorithms;

import java.util.HashMap;
import java.util.Random;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.gGA;
import jmetal.operators.crossover.Crossover;
import jmetal.operators.mutation.Mutation;
import jmetal.operators.selection.SelectionFactory;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

class VmsTwoPointCrossover extends Crossover {
	private static final long serialVersionUID = -7695502935986423562L;

	private double crossoverProbability_;

	public VmsTwoPointCrossover(HashMap<String, Object> parameters) {
		super(parameters);
		if (parameters.get("probability") != null)
			crossoverProbability_ = (Double) parameters.get("probability");
	}

	@Override
	public Object execute(Object object) throws JMException {
		Solution[] parents = (Solution[]) object;
		Solution parent1, parent2;
		Solution[] offspring = new Solution[2];
		parent1 = parents[0];
		parent2 = parents[1];

		offspring[0] = new Solution(parent1);
		offspring[1] = new Solution(parent2);
		if (PseudoRandom.randDouble() < crossoverProbability_) {
			int cxPoint1, cxPoint2;
			int varLength;

			varLength = parent1.getDecisionVariables().length;
			cxPoint1 = PseudoRandom.randInt(0, varLength - 1);
			cxPoint2 = PseudoRandom.randInt(0, varLength - 1);

			while (cxPoint1 == cxPoint2)
				cxPoint2 = PseudoRandom.randInt(0, varLength - 1);
			if (cxPoint1 > cxPoint2) {
				int swap = cxPoint2;
				cxPoint2 = cxPoint1;
				cxPoint1 = swap;
			}

			for (int var = cxPoint1; var <= cxPoint2; var++) {
				int tmp1 = (int) offspring[0].getDecisionVariables()[var].getValue();
				int tmp2 = (int) offspring[1].getDecisionVariables()[var].getValue();
				((VMLoc) offspring[0].getDecisionVariables()[var]).setValue(tmp2);
				((VMLoc) offspring[1].getDecisionVariables()[var]).setValue(tmp1);
			}
		}

		return offspring;
	}

}

class VmsMutation extends Mutation {
	private static final long serialVersionUID = -3567646421242555524L;

	private Double mutationProbability_ = null;

	public VmsMutation(HashMap<String, Object> parameters) {
		super(parameters);

		if (parameters.get("probability") != null)
			mutationProbability_ = (Double) parameters.get("probability");
	}

	@Override
	/**
	 * USING SWAP STATEGY HERE
	 */
	public Object execute(Object object) throws JMException {
		Solution solution = (Solution) object;
		if (PseudoRandom.randDouble() < mutationProbability_) {
			int index1 = PseudoRandom.randInt(0, solution.getDecisionVariables().length - 1);
			int index2 = PseudoRandom.randInt(0, solution.getDecisionVariables().length - 1);

			int tmp1 = (int) ((VMLoc) solution.getDecisionVariables()[index1]).getValue();
			int tmp2 = (int) ((VMLoc) solution.getDecisionVariables()[index2]).getValue();

			((VMLoc) solution.getDecisionVariables()[index1]).setValue(tmp2);
			((VMLoc) solution.getDecisionVariables()[index2]).setValue(tmp1);
		}
		return solution;
	}

}

public class GA {
	private VmsProblem problem;
	private int popSize, maxEval;
	private double cxRate, muRate;

	public GA(String dataset, int popSize, int maxEval, double cxRate, double muRate, long seed) {
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		problem = new VmsProblem(dataset, new Random(seed));
		this.popSize = popSize;
		this.maxEval = maxEval;
		this.cxRate = cxRate;
		this.muRate = muRate;
	}

	public void execGA() {
		Algorithm algorithm;
		Operator crossover;
		Operator mutation;
		Operator selection = null;

		HashMap<String, Object> parameters;

		algorithm = new gGA(problem);

		algorithm.setInputParameter("populationSize", popSize);
		algorithm.setInputParameter("maxEvaluations", maxEval);

		parameters = new HashMap<String, Object>();
		parameters.put("probability", cxRate);
		crossover = new VmsTwoPointCrossover(parameters);

		parameters.clear();
		parameters.put("probability", muRate);
		mutation = new VmsMutation(parameters);

		parameters.clear();
		try {
			selection = SelectionFactory.getSelectionOperator("BinaryTournament", parameters);
		} catch (JMException e) {
			e.printStackTrace();
		}

		algorithm.addOperator("crossover", crossover);
		algorithm.addOperator("mutation", mutation);
		algorithm.addOperator("selection", selection);
		SolutionSet population = null;
		try {
			population = algorithm.execute();
		} catch (ClassNotFoundException | JMException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		GA garunner = new GA("eprotein", 50, 1000, 0.95, 0.95, 12306L);
		garunner.execGA();
	}
}
