package edu.ncsu.algorithms;

import java.util.HashMap;
import java.util.Random;

import com.google.common.primitives.Ints;

import jmetal.core.Algorithm;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
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
			int varLength = parents[0].getDecisionVariables().length;
			int cxPoint = PseudoRandom.randInt(1, varLength - 2);

			// 1st step, common single point crossover
			for (int var = 0; var <= cxPoint; var++) {
				swap((VmEncoding) offspring[0].getDecisionVariables()[var],
						(VmEncoding) offspring[1].getDecisionVariables()[var]);
			}
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
			Variable[] decs = solution.getDecisionVariables();
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

			// 2, mutate the orders.
			// pick two locations, swap them
			int i = PseudoRandom.randInt(0, varLength - 1);
			int j = PseudoRandom.randInt(0, varLength - 1);

			int tmp = ((VmEncoding) decs[i]).getOrder();
			((VmEncoding) decs[i]).setOrder(((VmEncoding) decs[j]).getOrder());
			((VmEncoding) decs[j]).setOrder(tmp);

			// 3, mutate task2ins and ins2type
			int maxIns = Ints.max(task2ins);
			int maxType = Ints.max(ins2type);
			for (int v = 0; v < varLength; v++) {
				VmEncoding var = (VmEncoding) decs[v];
				if (PseudoRandom.randDouble() < bitMutationProbability_)
					var.setTask2ins(PseudoRandom.randInt(0, maxIns));
				if (PseudoRandom.randDouble() < bitMutationProbability_)
					var.setIns2Type(PseudoRandom.randInt(0, maxType));
			}
		}
		// debugs(solution);
		return solution;
	}

}

public class EMSC {
	public SolutionSet execNSGAII(HashMap<String, Object> para) throws ClassNotFoundException, JMException {
		return execNSGAII((String) para.get("dataset"), //
				(int) para.get("popSize"), //
				(int) para.get("maxEval"), //
				(double) para.get("cxProb"), //
				(double) para.get("cxRandChangeProb"), //
				(double) para.get("mutProb"), //
				(double) para.get("bitMutProb"), //
				(long) para.get("seed"));
	}

	public SolutionSet execSPEA2(HashMap<String, Object> para) throws ClassNotFoundException, JMException {
		return execSPEA2((String) para.get("dataset"), //
				(int) para.get("popSize"), //
				(int) para.get("maxEval"), //
				(int) para.get("arxvSize"), //
				(double) para.get("cxProb"), //
				(double) para.get("cxRandChangeProb"), //
				(double) para.get("mutProb"), //
				(double) para.get("bitMutProb"), //
				(long) para.get("seed"));
	}

	public SolutionSet execNSGAII(String dataset, int popSize, int maxEval, double cxProb, double cxRandChangeProb,
			double mutProb, double bitMutProb, long seed) throws JMException, ClassNotFoundException {
		VmsProblem problem_ = new VmsProblem(dataset, new Random(seed));
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		Algorithm alg = new NSGAII(problem_);
		HashMap<String, Object> parameters = new HashMap<String, Object>();

		/** for all MOEA */
		alg.setInputParameter("populationSize", popSize); // WARNING MUST BE
															// EVEN
		alg.setInputParameter("maxEvaluations", maxEval);

		// add init seeds -- cheapeast and fastest schedule generated by HEFT
		// algorithm
		parameters.clear();
		parameters.put("dataset", dataset);
		parameters.put("seed", seed);
		parameters.put("maxSimultaneousIns", 10); // from moheft paper
		MOHEFT heft = new MOHEFT();
		alg.setInputParameter("seedInitPopulation", heft.execHEFTMinExtremes(parameters));

		parameters.clear();
		parameters.put("probability", cxProb);
		parameters.put("randomChangeProbability", cxRandChangeProb);
		Crossover crossover = new ZhuCrossover(parameters);

		parameters.clear();
		parameters.put("probability", mutProb);
		parameters.put("bitMutationProbability", bitMutProb);
		Mutation mutation = new ZhuMutation(parameters);

		parameters.clear();
		Selection selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);

		alg.addOperator("crossover", crossover);
		alg.addOperator("mutation", mutation);
		alg.addOperator("selection", selection);

		SolutionSet p = alg.execute();
		for (int v = 0; v < p.size(); v++) {
			System.out.println(p.get(v));
		}

		return p;
	}

	public SolutionSet execSPEA2(String dataset, int popSize, int maxEval, int arxvSzie, double cxProb,
			double cxRandChangeProb, double mutProb, double bitMutProb, long seed)
			throws JMException, ClassNotFoundException {
		VmsProblem problem_ = new VmsProblem(dataset, new Random(seed));
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		Algorithm alg = new SPEA2(problem_);
		HashMap<String, Object> parameters = new HashMap<String, Object>();

		/** for all MOEA */
		alg.setInputParameter("populationSize", popSize); // WARNING MUST BE
															// EVEN
		alg.setInputParameter("maxEvaluations", maxEval);
		/** for spea2 only */
		alg.setInputParameter("archiveSize", arxvSzie); // for spea2

		// add init seeds -- cheapeast and fastest schedule generated by HEFT
		// algorithm
		parameters.clear();
		parameters.put("dataset", dataset);
		parameters.put("seed", seed);
		parameters.put("maxSimultaneousIns", 10); // from moheft paper
		MOHEFT heft = new MOHEFT();
		alg.setInputParameter("seedInitPopulation", heft.execHEFTMinExtremes(parameters));

		parameters.clear();
		parameters.put("probability", cxProb);
		parameters.put("randomChangeProbability", cxRandChangeProb);
		Crossover crossover = new ZhuCrossover(parameters);

		parameters.clear();
		parameters.put("probability", mutProb);
		parameters.put("bitMutationProbability", bitMutProb);
		Mutation mutation = new ZhuMutation(parameters);

		parameters.clear();
		Selection selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);

		alg.addOperator("crossover", crossover);
		alg.addOperator("mutation", mutation);
		alg.addOperator("selection", selection);

		SolutionSet p = alg.execute();
		for (int v = 0; v < p.size(); v++) {
			System.out.println(p.get(v));
		}

		return p;
	}

	/**
	 * This is a DEMOSTRATION of EMSC algorithm. To do experiments, go to
	 * edu.ncsu.experiments
	 */
	public static void main(String[] args) throws JMException, ClassNotFoundException {
		// for (String model : Infrastructure.models) {
		for (String model : new String[] { "j30" }) {
			System.out.println("Starting : " + model);
			HashMap<String, Object> paras = new HashMap<String, Object>();
			paras.put("dataset", model);
			paras.put("seed", System.currentTimeMillis());
			paras.put("popSize", 50);
			paras.put("maxEval", 100);
			paras.put("arxvSize", 10); // spea2 used only
			paras.put("cxProb", 0.6);
			paras.put("cxRandChangeProb", 0.1);
			paras.put("mutProb", 0.8);
			paras.put("bitMutProb", 0.4);
			long start_time = System.currentTimeMillis();
			EMSC runner = new EMSC();
			runner.execSPEA2(paras);
			System.out.println("EXEC TIME = " + (System.currentTimeMillis() - start_time) / 1000);
		}
	}
}
