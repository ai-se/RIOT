package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;

import edu.ncsu.wls.CloudletPassport;
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

	/**
	 * B-Rank One time execute. no need to optimize :)
	 * 
	 * @param p
	 * @return the rank. use pointers of cloudlets
	 */
	private List<Cloudlet> bRank(VmsProblem p) {
		List<Cloudlet> cloudlets = p.getCloudletList2();
		CloudletPassport cp = p.getWorkflow();

		Map<Cloudlet, Integer> upwardRank = new HashMap<Cloudlet, Integer>();

		List<Cloudlet> lstVisit = new ArrayList<Cloudlet>();
		int dep = 0;

		for (Cloudlet c : cloudlets)
			if (!cp.hasSucc(c)) {
				upwardRank.put(c, 0);
				lstVisit.add(c);
			}
		int prevLen0 = 0;

		while (true) {
			int prevLen1 = lstVisit.size();
			dep += 1;

			for (int i = prevLen0; i < prevLen1; i++) {
				Cloudlet lv = lstVisit.get(i);
				for (Cloudlet x : cp.meRequires(lv)) {
					if (!upwardRank.containsKey(x) || upwardRank.get(x) < dep) { // refresh
						upwardRank.put(x, dep);
						lstVisit.add(x);
					} // if
				} // for x
			} // for lv

			if (lstVisit.size() == prevLen1)
				break;
			prevLen0 = prevLen1;
		}

		
		
		System.out.println(upwardRank);
		return null;
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		System.out.println("here entered ..1");
		// 1. B-Rank
		this.bRank((VmsProblem) problem_);

		return null;
	}

}

public class MOHEFT {
	public void execMOHEFT(String dataset, long seed) throws ClassNotFoundException, JMException {
		VmsProblem problem_ = new VmsProblem(dataset, new Random(seed));
		Algorithm alg = new MOHEFTcore(problem_);
		alg.execute();
		// TODO add any problems
	}

	public static void main(String[] args) throws ClassNotFoundException, JMException {
		MOHEFT testrun = new MOHEFT();
		testrun.execMOHEFT("eprotein", 1860);
	}
}
