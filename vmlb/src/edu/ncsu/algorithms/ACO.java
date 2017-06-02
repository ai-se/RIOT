package edu.ncsu.algorithms;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.Cloudlet;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;

import edu.ncsu.wls.Infrastructure;
import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.ObjectiveComparator;

/***
 * Algorithm kernel of ACO algorithms
 * 
 * @author jianfeng
 *
 */

class TrivalHeuristic extends Operator {
	private static final long serialVersionUID = -1142114185865335395L;

	public TrivalHeuristic(HashMap<String, Object> parameters) {
		super(parameters);
	}

	@Override
	public Object execute(Object object) throws JMException {
		return 1.0;
	}

}

/**
 * 
 * @author jianfeng
 *
 */
class AlgACO extends Algorithm {
	private static final long serialVersionUID = -8558428517880302006L;

	public AlgACO(Problem problem) {
		super(problem);
	}

	/**
	 * Randomly select one item which value is 0. Returning the index
	 * 
	 * @param list
	 * @param rand
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private int randomSampleZero(int[] list, List N, Random rnd) {
		Collections.shuffle(N, rnd);
		for (int i = 0; i < N.size(); i++)
			if (list[i] == 0)
				return i;
		return -1;
	}

	/**
	 * 
	 * @param DAG
	 * @param rDAG
	 * @param number
	 *            How many random topo sorting to get
	 * @param seed
	 *            Controlling random process
	 * @return *number*s List. Each one contains a topo sorting
	 */
	@SuppressWarnings("unchecked")
	private List<Integer>[] batchRandomTopo(int[][] DAG, int[][] rDAG, int number, long seed) {
		Random rnd = new Random(seed);
		int n = DAG.length;

		List<Integer>[] res = new ArrayList[number];
		for (int i = 0; i < number; i++)
			res[i] = new ArrayList<Integer>();

		// enumerations {0..n-1}
		ArrayList<Integer> N = new ArrayList<Integer>();
		for (int i = 0; i < n; i++)
			N.add(i);

		for (int x = 0; x < number; x++) {
			int[] outDegree = new int[n];
			int[] inDegree = new int[n];
			for (int i = 0; i < n; i++) {
				outDegree[i] = IntStream.of(DAG[i]).sum();
				inDegree[i] = IntStream.of(rDAG[i]).sum();
			} // for i in n

			for (int nodes = 0; nodes < n; nodes++) {
				int toadd = randomSampleZero(inDegree, N, rnd);
				res[x].add(toadd);
				inDegree[toadd] = -1;
				for (int to = 0; to < n; to++)
					if (DAG[toadd][to] == 1)
						inDegree[to] -= 1;
			} // for adding a node
		} // create one res instance

		/*
		 * The following algorithm is buggy 06-01-2017 (all previous results
		 * removed)
		 */
		// List<Integer> currentGroup = new ArrayList<Integer>();
		// int checked = 0;
		// while (checked < n) {
		// currentGroup.clear();
		// for (int i = 0; i < n; i++) {
		// if (inDegree[i] == 0) {
		// currentGroup.add(i);
		// inDegree[i] = -1;
		// }
		// }
		//
		// for (int c = 0; c < number; c++) {
		// Collections.shuffle(currentGroup, rnd);
		// res[c].addAll(currentGroup);
		// }
		// for (int from : currentGroup) {
		// for (int to = 0; to < n; to++)
		// if (DAG[from][to] == 1)
		// inDegree[to] -= 1;
		// }
		// checked += currentGroup.size();
		// }

		return res;
	}

	/**
	 * ATTENTION: this function cannot handle any problem specific info
	 * Task-service mapping are given by tauMatrix
	 */
	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		int antSize;
		int maxIterations;
		int maxEvaluations;
		long seed;
		double q0;
		double beta;
		double tau0;
		double rho;

		int evaluations;
		int iterations;

		int[][] DAG;
		int[][] rDAG;
		double[][] tauMatrix;
		Random rnd;

		Comparator<Solution> comparator;
		comparator = new ObjectiveComparator(0); // single objective comparator

		Operator heuristicOperator;

		// Read the params
		antSize = ((Integer) this.getInputParameter("antSize")).intValue();
		maxIterations = ((Integer) this.getInputParameter("maxIterations")).intValue();
		maxEvaluations = ((Integer) this.getInputParameter("maxEvaluations")).intValue();
		q0 = ((Double) this.getInputParameter("q0")).doubleValue();
		beta = ((Double) this.getInputParameter("beta")).doubleValue();
		rho = ((Double) this.getInputParameter("rho")).doubleValue();
		seed = ((Long) this.getInputParameter("seed")).longValue();
		DAG = (int[][]) this.getInputParameter("DAG");
		tauMatrix = (double[][]) this.getInputParameter("tauMatrix");

		// Read Operators
		heuristicOperator = this.operators_.get("heuristic");

		// Initialize the variables
		evaluations = 0;
		iterations = 0;
		tau0 = Doubles.max(tauMatrix[0]);

		rDAG = new int[DAG[0].length][DAG.length];
		for (int i = 0; i < DAG.length; i++)
			for (int j = 0; j < DAG[0].length; j++)
				rDAG[j][i] = DAG[i][j];

		int totalAvlPool = Math.min(maxEvaluations, antSize * maxIterations);
		List<Integer>[] sortedPool = this.batchRandomTopo(DAG, rDAG, totalAvlPool, seed);

		Solution best_so_far = new Solution(problem_);
		problem_.evaluate(best_so_far);
		evaluations++;

		List<Integer> M = new ArrayList<Integer>();
		for (int j = 0; j < tauMatrix[0].length; j++)
			M.add(j);
		rnd = new Random(seed);

		while (evaluations < maxEvaluations && iterations <= maxIterations) {
			System.out.println(evaluations + ": " + best_so_far.getObjective(0));
			Class K = null;
			Method setValue = null;
			try {
				K = new Solution(problem_).getDecisionVariables()[0].getVariableType();
				setValue = K.getMethod("setValue", int.class);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}

			for (int ant = 0; ant < antSize; ant++) {
				// ACO requires to construct the solution step-by-step
				Solution newInd = new Solution(problem_);
				Variable[] decs = newInd.getDecisionVariables();

				for (int node : sortedPool[evaluations % totalAvlPool]) {
					int selected = 0;
					if (PseudoRandom.randDouble() < q0) { // Max tau.beta^eta
						double maxS = tauMatrix[node][selected];
						Collections.shuffle(M, rnd);
						for (int j : M) {
							// TODO extend heuristic operator if needed
							double calc = tauMatrix[node][j] * Math.pow(beta, (double) heuristicOperator.execute(null));
							if (calc > maxS) {
								maxS = calc;
								selected = j;
							}
						}
					} else { // RWS
						selected = rwsSimu(tauMatrix[node], rnd);
					}

					try {
						setValue.invoke(decs[node], selected);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					} // try-catch

					// Local Pheromone updating
					// TODO extended if needed
					tauMatrix[node][selected] = (1 - rho) * tauMatrix[node][selected] + rho * tau0;
				} // for node

				// Evaluations
				problem_.evaluate(newInd);
				evaluations++;

				// update hall-of-fame & Global Pheromone updating
				if (comparator.compare(best_so_far, newInd) > 0) {
					best_so_far = newInd;
					// TODO extend following process if needed
					for (int var = 0; var < problem_.getNumberOfVariables(); var++) {
						int selected = (int) newInd.getDecisionVariables()[var].getValue();
						tauMatrix[var][selected] = (1 - rho) * tauMatrix[var][selected] + rho * 2; // TODO
																									// 2
					} // for
				} // if
			} // for ant

			iterations++;
		}

		SolutionSet resultPopulation = new SolutionSet(1);
		resultPopulation.add(best_so_far);
		return resultPopulation;
	}

	/**
	 * Roulette wheel scheme simulated
	 * 
	 * @param tau
	 * @param rnd
	 * @return
	 */
	private int rwsSimu(double[] tau, Random rnd) {
		int n = tau.length;
		double[] accum = new double[n];
		for (int i = 0; i < n; i++) {
			if (i == 0)
				accum[0] = tau[0];
			else
				accum[i] = accum[i - 1] + tau[i];
		} // for

		double sample = rnd.nextDouble() * accum[n - 1];

		for (int i = 0; i < accum.length; i++)
			if (accum[i] >= sample)
				return i;

		return -1;
	}
}

public class ACO {
	private VmsProblem problem_;
	private int antSize, maxIterations, maxEvaluations;
	private double q0, beta, rho;
	private long seed;

	public ACO(HashMap<String, Object> parameters) {
		seed = (long) parameters.get("seed");
		String dataset = (String) parameters.get("dataset");
		antSize = (int) parameters.get("antSize");
		maxIterations = (int) parameters.get("maxIterations");
		maxEvaluations = (int) parameters.get("maxEvaluations");
		q0 = (double) parameters.get("q0");
		beta = (double) parameters.get("beta");
		rho = (double) parameters.get("rho");

		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		problem_ = new VmsProblem(dataset, new Random(seed));
	}

	@SuppressWarnings("unused")
	public double[] execACO() {
		Algorithm algorithm = new AlgACO(problem_);
		HashMap<String, Object> parameters = new HashMap<String, Object>();

		/* Creating the DAG */
		int[][] DAG = new int[problem_.getNumberOfVariables()][problem_.getNumberOfVariables()];
		HashMap<Cloudlet, List<Cloudlet>> graph = problem_.getWorkflow().getRequiring();
		for (Cloudlet to : graph.keySet()) {
			for (Cloudlet from : graph.get(to))
				DAG[from.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT][to.getCloudletId()
						- Infrastructure.CLOUDLET_ID_SHIFT] = 1;
		}

		/*
		 * Construct tauMatrix Each row of matrix representing one cloudlet Each
		 * element in one row representing available service Here we're assuming
		 * all VMs are open to any cloudlet
		 */
		double[][] tauMatrix = new double[problem_.getNumberOfVariables()][problem_.getVmids().length];
		double tau0 = 0.111; // TODO
		for (int i = 0; i < tauMatrix.length; i++)
			for (int j = 0; j < tauMatrix[0].length; j++)
				tauMatrix[i][j] = tau0;

		algorithm.setInputParameter("antSize", antSize);
		algorithm.setInputParameter("seed", seed);
		algorithm.setInputParameter("maxIterations", maxIterations);
		algorithm.setInputParameter("maxEvaluations", maxEvaluations);
		algorithm.setInputParameter("q0", q0);
		algorithm.setInputParameter("beta", beta);
		algorithm.setInputParameter("rho", rho);
		algorithm.setInputParameter("DAG", DAG);
		algorithm.setInputParameter("tauMatrix", tauMatrix);

		parameters.clear();
		algorithm.addOperator("heuristic", new TrivalHeuristic(parameters));

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
		HashMap<String, Object> exp1_para = new HashMap<String, Object>();
		exp1_para.put("dataset", "j60");
		exp1_para.put("seed", 18769787387L);
		// exp1_para.put("seed", System.currentTimeMillis());
		exp1_para.put("antSize", 50);
		exp1_para.put("maxIterations", 50);
		exp1_para.put("maxEvaluations", 5000);
		exp1_para.put("q0", 0.9);
		exp1_para.put("rho", 0.1);
		exp1_para.put("beta", 1.2);
		new ACO(exp1_para).execACO();
	}
}
