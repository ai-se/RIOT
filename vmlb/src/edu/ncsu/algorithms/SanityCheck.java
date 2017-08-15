package edu.ncsu.algorithms;

import java.util.HashMap;
import java.util.Random;

import edu.ncsu.wls.INFRA;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.NonDominatedSolutionList;
import jmetal.util.PseudoRandom;

public class SanityCheck {

	private SolutionSet execute(String dataset, int solNum, long seed) throws ClassNotFoundException, JMException {
		VmsProblem problem_ = new VmsProblem(dataset, new Random(seed));
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		Random rand = new Random(seed);

		SolutionSet res = new SolutionSet(solNum);
		// create some random scheduling for sanity check
		for (int i = 0; i < solNum; i++) {
			Solution x = new Solution(problem_);
			VmEncoding p = problem_.fetchSolDecs(x);
			for (int v = 0; v < problem_.tasksNum; v++) {
				p.task2ins[v] = rand.nextInt((int) (problem_.tasksNum));
				p.ins2type[v] = rand.nextInt(INFRA.getAvalVmTypeNum());
			}
			res.add(x);
		}

		// evaluations
		SolutionSet ress = new NonDominatedSolutionList();
		for (int i = 0; i < solNum;i++){
			problem_.evaluate(res.get(i));
			ress.add(res.get(i));
		}
		return ress;
	}

	public SolutionSet execute(HashMap<String, Object> para) throws ClassNotFoundException, JMException {
		return execute((String) para.get("dataset"), (int) para.get("N"), (long) para.get("seed"));
	}

	public static void main(String[] args) throws ClassNotFoundException, JMException {
		for (String model : new String[] { "sci_Montage_1000" }) {
			HashMap<String, Object> paras = new HashMap<String, Object>();
			paras.put("dataset", model);
			paras.put("seed", System.currentTimeMillis());
			paras.put("N", 20);
			long start_time = System.currentTimeMillis();
			SanityCheck runner = new SanityCheck();
			SolutionSet res = runner.execute(paras);
			System.out.println("EXEC TIME = " + (System.currentTimeMillis() - start_time) / 1000);
			
			res.printObjectives();
		}
	}

}
