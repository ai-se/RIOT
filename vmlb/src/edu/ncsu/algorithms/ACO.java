package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.Cloudlet;

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

class AlgACO extends Algorithm {
	private static final long serialVersionUID = -8558428517880302006L;

	public AlgACO(Problem problem) {
		super(problem);
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
	@SuppressWarnings({ "unchecked" })
	private List<Integer>[] batchRandomTopo(int[][] DAG, int[][] rDAG, int number, long seed) {
		Random rnd = new Random(seed);
		int n = DAG.length;

		List<Integer>[] res = new List[number];
		for (int i = 0; i < number; i++)
			res[i] = new ArrayList<Integer>();

		int[] rsum_DAG = new int[n];
		int[] rsum_rDAG = new int[n];
		for (int i = 0; i < n; i++) {
			rsum_DAG[i] = IntStream.of(DAG[i]).sum();
			rsum_rDAG[i] = IntStream.of(rDAG[i]).sum();
		}

		List<Integer> currentGroup = new ArrayList<Integer>();
		int checked = 0;
		while (checked < n) {
			currentGroup.clear();
			for (int i = 0; i < n; i++) {
				if (rsum_rDAG[i] == 0) {
					currentGroup.add(i);
				}

			}
			for (int c = 0; c < number; c++) {
				Collections.shuffle(currentGroup, rnd);
				res[c].addAll(currentGroup);
			}
			for (int x : currentGroup) {
				for (int y : DAG[x])
					rsum_rDAG[y] = rsum_rDAG[y] - 1;
			}
			checked += currentGroup.size();
		}

		return res;
	}

	/**
	 * ATTENTION: this function cannot handle any problem specific info
	 * Task-service mapping are given by tauMatrix
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		int antSize;
		int maxIterations;
		int maxEvaluations;
		long seed;
		double q0;
		double beta;

		int evaluations;
		int iterations;

		int[][] DAG;
		int[][] rDAG;
		double[][] tauMatrix;
		Random rnd;

		Comparator comparator;
		comparator = new ObjectiveComparator(0); // single objective comparator

		Operator heuristicOperator;

		// Read the params
		antSize = ((Integer) this.getInputParameter("antSize")).intValue();
		maxIterations = ((Integer) this.getInputParameter("maxIterations")).intValue();
		maxEvaluations = ((Integer) this.getInputParameter("maxEvaluations")).intValue();
		q0 = ((Double) this.getInputParameter("q0")).doubleValue();
		beta = ((Double) this.getInputParameter("beta")).doubleValue();
		seed = ((Long) this.getInputParameter("seed")).longValue();
		DAG = (int[][]) this.getInputParameter("DAG");
		tauMatrix = (double[][]) this.getInputParameter("tauMatrix");

		// Read Operators
		heuristicOperator = this.operators_.get("heuristic");

		// Initialize the variables
		evaluations = 0;
		iterations = 0;

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

		while (evaluations < maxEvaluations && iterations < maxIterations) {
			for (int ant = 0; ant < antSize; ant++) {
				// ACO requires to construct the solution step-by-step
				Solution newInd = new Solution(problem_);
				Variable[] decs = new Variable[problem_.getNumberOfVariables()];

				for (int node : sortedPool[evaluations % totalAvlPool]) {
					if (PseudoRandom.randDouble() < q0) { // Max tau.beta^eta
						double maxS = tauMatrix[node][0];
						Collections.shuffle(M, rnd);
						for (int j : M) {
							double calc = tauMatrix[node][j] * Math.pow(beta, 1);
							if (calc > maxS) {
								maxS = calc;
								// TODO to continue...
								// decs[node] = j;
							}
						}
					} else { // RWS

					}
				}
			}
			iterations++;
		}
		return null;
	}

}

public class ACO {
	private VmsProblem problem_;
	private int antSize, maxIterations, maxEvaluations;
	private double q0, beta;
	private long seed;

	public ACO(HashMap<String, Object> parameters) {
		seed = (long) parameters.get("seed");
		String dataset = (String) parameters.get("dataset");
		antSize = (int) parameters.get("antSize");
		maxIterations = (int) parameters.get("maxIterations");
		maxEvaluations = (int) parameters.get("maxEvaluations");
		q0 = (double) parameters.get("q0");
		beta = (double) parameters.get("beta");

		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		problem_ = new VmsProblem(dataset, new Random(seed));
	}

	public void execACO() {
		Algorithm algorithm = new AlgACO(problem_);
		HashMap<String, Object> parameters = new HashMap<String, Object>();

		/* Creating the DAG */
		int[][] DAG = new int[problem_.getNumberOfVariables()][problem_.getNumberOfVariables()];
		HashMap<Cloudlet, List<Cloudlet>> graph = problem_.getWorkflow().getRequiring();
		for (Cloudlet from : graph.keySet()) {
			for (Cloudlet to : graph.get(from))
				DAG[from.getCloudletId() - Infrastructure.CLOUDLET_ID_SHIFT][to.getCloudletId()
						- Infrastructure.CLOUDLET_ID_SHIFT] = 1;
		}

		/*
		 * Construct tauMatrix Each row of matrix representing one cloudlet Each
		 * element in one row representing available service Here we're assuming
		 * all VMs are open to any cloudlet
		 */
		double[][] tauMatrix = new double[problem_.getNumberOfVariables()][problem_.getVmids().length];
		double tau0 = 0.33; // TODO
		for (int i = 0; i < tauMatrix.length; i++)
			for (int j = 0; j < tauMatrix[0].length; j++)
				tauMatrix[i][j] = tau0;

		algorithm.setInputParameter("antSize", antSize);
		algorithm.setInputParameter("seed", seed);
		algorithm.setInputParameter("maxIterations", maxIterations);
		algorithm.setInputParameter("maxEvaluations", maxEvaluations);
		algorithm.setInputParameter("q0", q0);
		algorithm.setInputParameter("beta", beta);
		algorithm.setInputParameter("DAG", DAG);
		algorithm.setInputParameter("tauMatrix", tauMatrix);

		parameters.clear();
		algorithm.addOperator("heuristic", new TrivalHeuristic(parameters));

		try {
			algorithm.execute();
		} catch (ClassNotFoundException | JMException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		HashMap<String, Object> exp1_para = new HashMap<String, Object>();
		exp1_para.put("dataset", "j30");
		exp1_para.put("seed", 18769787387L);
		exp1_para.put("antSize", 10);
		exp1_para.put("maxIterations", 5);
		exp1_para.put("maxEvaluations", 50);
		exp1_para.put("q0", 0.9);
		exp1_para.put("beta", 1.2);
		new ACO(exp1_para).execACO();
	}
}
