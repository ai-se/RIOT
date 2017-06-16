package edu.ncsu.algorithms;

import java.util.Random;

import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

class AlgSway extends Algorithm {
	private static final long serialVersionUID = 7491441804538378626L;

	public AlgSway(Problem problem) {
		super(problem);
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}

public class Sway {
	private VmsProblem problem;

	public Sway(String dataset, long seed) {
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		this.problem = new VmsProblem(dataset, new Random(seed));
	}

	public double[] execSway() {
		// TODO
		return null;
	}
}
