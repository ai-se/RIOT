package edu.ncsu.algorithms;

import java.util.HashMap;
import java.util.Random;

import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

/**
 * 
 * @author jianfeng
 *
 */

class AlgSway extends Algorithm {
	private static final long serialVersionUID = 7491441804538378626L;

	public AlgSway(Problem problem) {
		super(problem);
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		System.out.println("Framework works");
		for (int i = 0; i < 100; i++)
			;
		return null;
	}

}

public class SWAY {
	private VmsProblem problem;

	public SolutionSet executeSWAY(HashMap<String, Object> paras) throws ClassNotFoundException, JMException {
		return executeSWAY((String) paras.get("dataset"), //
				(long) paras.get("seed"));
	}

	public SolutionSet executeSWAY(String dataset, long seed) throws ClassNotFoundException, JMException {
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		this.problem = new VmsProblem(dataset, new Random(seed));
		Algorithm alg = new AlgSway(problem);

		// TODO SWAY algorithm configurations
		SolutionSet p = alg.execute();

		return p;
	}

	/**
	 * This is a DEMOSTRATION of EMSC algorithm. To do experiments, go to
	 * edu.ncsu.experiments
	 */
	public static void main(String[] args) throws JMException, ClassNotFoundException {
		// for (String model : Infrastructure.models) {
		for (String model : new String[] { "eprotein" }) {
			HashMap<String, Object> paras = new HashMap<String, Object>();
			paras.put("dataset", model);
			paras.put("seed", 12345L);
			long start_time = System.currentTimeMillis();
			SWAY runner = new SWAY();
			runner.executeSWAY(paras);
			System.out.println("EXEC TIME = " + (System.currentTimeMillis() - start_time) / 1000);
		}

	}
}
