package edu.ncsu.algorithms;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;

import edu.ncsu.wls.Infrastructure;
import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.ObjectiveComparator;

/***
 * Algorithm kernel of ACO algorithms
 * 
 * @author jianfeng
 *
 */
class AlgACO extends Algorithm {
	private static final long serialVersionUID = -8558428517880302006L;

	public AlgACO(Problem problem) {
		super(problem);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		int antSize;
		int maxIterations;
		int maxEvaluations;
		double tau0;
		double q0;

		int evaluations;
		int iterations;

		SolutionSet population;

		int[][] DAG;
		double[][] TAU;

		Comparator comparator;
		comparator = new ObjectiveComparator(0); // single objective comparator

		// Read the params
		antSize = ((Integer) this.getInputParameter("antSize")).intValue();
		maxIterations = ((Integer) this.getInputParameter("maxIterations")).intValue();
		maxEvaluations = ((Integer) this.getInputParameter("maxEvaluations")).intValue();
		tau0 = ((Double) this.getInputParameter("tau0")).doubleValue();
		q0 = ((Double) this.getInputParameter("q0")).doubleValue();
		DAG = (int[][]) this.getInputParameter("DAG");

		// Initialize the variables
		population = new SolutionSet(antSize);
		evaluations = 0;
		iterations = 0;
		TAU = new double[problem_.getNumberOfVariables()][problem_.getNumberOfVariables()];
		for (int i = 0; i < problem_.getNumberOfVariables(); i++)
			for (int j = 0; j < problem_.getNumberOfVariables(); j++)
				TAU[i][j] = tau0;

		// // Create the initial population
		// Solution newIndividual;
		// for (int i = 0; i < antSize; i++) {
		// newIndividual = new Solution(problem_);
		// problem_.evaluate(newIndividual);
		// evaluations++;
		// population.add(newIndividual);
		// } // for
		while (evaluations < maxEvaluations && iterations < maxIterations) {
			for (int ant = 0; ant < antSize; ant++) {
				// ACO requires to construct the solution step-by-step

			}
			iterations++;
		}
		return null;
	}

}

public class ACO {
	private VmsProblem problem;

	public ACO(String dataset, long seed) {
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		problem = new VmsProblem(dataset, new Random(seed));
	}

	public void execACO() {
		Algorithm algorithm;

		// HashMap<String, Object> parameters;

		algorithm = new AlgACO(problem);

		algorithm.setInputParameter("antSize", 10); // TODO enable customization
		algorithm.setInputParameter("maxIterations", 5); // TODO
		algorithm.setInputParameter("maxEvaluations", 100); // TODO
		algorithm.setInputParameter("tau0", 0.32); // TODO
		algorithm.setInputParameter("q0", 0.9);
		/* creating the DAG */
		int[][] DAG = new int[problem.getNumberOfVariables()][problem.getNumberOfVariables()];
		HashMap<Cloudlet, List<Cloudlet>> graph = problem.getWorkflow().getRequiring();
		for (Cloudlet from : graph.keySet()) {
			for (Cloudlet to : graph.get(from))
				DAG[from.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT][to.getCloudletId()
						- Infrastructure.CLOUDLET_ID_SHIFT] = 1;
		}
		algorithm.setInputParameter("DAG", DAG);

		try {
			algorithm.execute();
		} catch (ClassNotFoundException | JMException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		new ACO("j30", 18769787387L).execACO();
	}
}
