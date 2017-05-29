package edu.ncsu.algorithms;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.operators.crossover.Crossover;
import jmetal.operators.mutation.Mutation;
import jmetal.operators.selection.SelectionFactory;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.ObjectiveComparator;

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

/**
 * Class implementing a generational genetic algorithm
 * 
 * Copy and modify from
 * jmetal.metaheuristics.singleObjective.geneticAlgorithm.gGA
 */
class gGA extends Algorithm {
	private static final long serialVersionUID = -7337994929814606290L;

	/**
	 *
	 * Constructor Create a new GGA instance.
	 * 
	 * @param problem
	 *            Problem to solve.
	 */
	public gGA(Problem problem) {
		super(problem);
	} // GGA

	/**
	 * Execute the GGA algorithm
	 * 
	 * @throws JMException
	 */
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		int populationSize;
		int maxEvaluations;
		int maxIterations;

		int evaluations;
		int iterations;

		SolutionSet population;
		SolutionSet offspringPopulation;

		Operator mutationOperator;
		Operator crossoverOperator;
		Operator selectionOperator;

		Comparator comparator;
		comparator = new ObjectiveComparator(0); // Single objective comparator

		// Read the params
		populationSize = ((Integer) this.getInputParameter("populationSize")).intValue();
		if (populationSize % 2 != 0)
			populationSize += 1; // fixing a bug
		maxEvaluations = ((Integer) this.getInputParameter("maxEvaluations")).intValue();
		maxIterations = ((Integer) this.getInputParameter("maxIterations")).intValue();
		// Initialize the variables
		population = new SolutionSet(populationSize);
		offspringPopulation = new SolutionSet(populationSize);

		evaluations = 0;
		iterations = 0;

		// Read the operators
		mutationOperator = this.operators_.get("mutation");
		crossoverOperator = this.operators_.get("crossover");
		selectionOperator = this.operators_.get("selection");

		// Create the initial population
		Solution newIndividual;
		for (int i = 0; i < populationSize; i++) {
			newIndividual = new Solution(problem_);
			problem_.evaluate(newIndividual);
			evaluations++;
			population.add(newIndividual);
		} // for

		// Sort population
		population.sort(comparator);
		while (evaluations < maxEvaluations && iterations < maxIterations) {
			iterations++;
			// if ((evaluations % 10) == 0) {
			System.out.println(evaluations + ": " + population.get(0).getObjective(0));
			// } //

			// Copy the best two individuals to the offspring population
			offspringPopulation.add(new Solution(population.get(0)));
			offspringPopulation.add(new Solution(population.get(1)));

			// Reproductive cycle
			for (int i = 0; i < (populationSize / 2 - 1); i++) {
				// Selection
				Solution[] parents = new Solution[2];

				parents[0] = (Solution) selectionOperator.execute(population);
				parents[1] = (Solution) selectionOperator.execute(population);

				// Crossover
				Solution[] offspring = (Solution[]) crossoverOperator.execute(parents);

				// Mutation
				mutationOperator.execute(offspring[0]);
				mutationOperator.execute(offspring[1]);

				// Evaluation of the new individual
				problem_.evaluate(offspring[0]);
				problem_.evaluate(offspring[1]);

				evaluations += 2;

				// Replacement: the two new individuals are inserted in the
				// offspring
				// population
				offspringPopulation.add(offspring[0]);
				offspringPopulation.add(offspring[1]);
			} // for

			// The offspring population becomes the new current population
			population.clear();
			for (int i = 0; i < populationSize; i++) {
				population.add(offspringPopulation.get(i));
			}
			offspringPopulation.clear();
			population.sort(comparator);
		} // while

		// Return a population with the best individual
		SolutionSet resultPopulation = new SolutionSet(1);
		resultPopulation.add(population.get(0));

		System.out.println("Evaluations: " + evaluations);
		return resultPopulation;
	} // execute
} // gGA

public class GA {
	private VmsProblem problem;
	private int popSize, maxEval, maxIterations;
	private double cxRate, muRate;

	public GA(String dataset, int popSize, int maxIterations, int maxEval, double cxRate, double muRate, long seed) {
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		problem = new VmsProblem(dataset, new Random(seed));
		this.popSize = popSize;
		this.maxEval = maxEval;
		this.cxRate = cxRate;
		this.muRate = muRate;
		this.maxIterations = maxIterations;
	}

	public double[] execGA() {
		Algorithm algorithm;
		Operator crossover;
		Operator mutation;
		Operator selection = null;

		HashMap<String, Object> parameters;

		algorithm = new gGA(problem);

		algorithm.setInputParameter("populationSize", popSize);
		algorithm.setInputParameter("maxEvaluations", maxEval);
		algorithm.setInputParameter("maxIterations", maxIterations);

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

		// redirect system.out to save the process
		System.out.println("Started...running...");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		PrintStream old = System.out;
		System.setOut(ps);

		// Running...
		try {
			population = algorithm.execute();
			System.out.flush();
		} catch (ClassNotFoundException | JMException e) {
			e.printStackTrace();
		}
		System.setOut(old);
		System.out.println("Done!");

		// Handling outputs
		System.out.println(baos.toString());
		List<Double> res = new ArrayList<Double>();
		String R = baos.toString();
		for (String s : R.split("\n")) {
			if (s.matches("\\d+: .*")) {
				double obj = Double.parseDouble(s.substring(s.indexOf(':') + 2));
				res.add(obj);
			}
		}

		double[] resarray = new double[res.size()];
		for (int i = 0; i < res.size(); i++)
			resarray[i] = res.get(i);
		return resarray;
	}

	public static void main(String[] args) {
		GA garunner = new GA("j30", 50, 5, 50 * 1000, 0.95, 0.95, 10086L);
		double[] res = garunner.execGA();
		System.out.println(Arrays.toString(res));
	}
}
