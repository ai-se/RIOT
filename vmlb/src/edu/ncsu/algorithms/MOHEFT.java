package edu.ncsu.algorithms;

import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * This file implements the following paper Juan J. Durillo and Radu Prodan
 * Multi-objective workflow scheduling in Amazon EC2 Cluster computing 17.2
 * (2014): 169-189.
 * 
 * MOHEFT- MultiObjective Heterogeneous Earliest Finish Time
 *
 * MOHEFT is a heuristic algorithm. time complexity is O(NMK). not suitable for
 * extremely large problems. For details, see the original paper
 * 
 * @author jianfeng
 *
 */

class MOHEFTcore extends Algorithm {
	private static final long serialVersionUID = 6855363312229721720L;

	public MOHEFTcore(Problem problem) {
		super(problem);
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// TODO here
		return null;
	}

}

public class MOHEFT {
	public static void main(String[] args) {
		System.out.println("test");
	}
}
