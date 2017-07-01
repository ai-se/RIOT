package edu.ncsu.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.print.attribute.standard.Finishings;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import edu.ncsu.wls.CloudletDAG;
import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudlet;
import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

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

class VmMock {
	public int typeIndex;
	public double finalTime;
	public long mips;
	public double dollarPerHr;

	public VmMock(int typeIndex, long mips, double dollarPerHr) {
		this.typeIndex = typeIndex;
		this.mips = mips;
		this.dollarPerHr = dollarPerHr;
		this.finalTime = 0;
	}

	public void extendFinalTime(double extended) {
		this.finalTime = finalTime + extended;
	}
}

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
	private Map<Cloudlet, Integer> bRank(VmsProblem p) {
		List<Cloudlet> cloudlets = p.getCloudletList2();
		CloudletDAG cp = p.getWorkflow();

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

		return upwardRank;
	}

	private double unitPrice(Vm v) {
		List<Vm> tmp = new ArrayList<Vm>();
		tmp.add(v);
		return Infrastructure.getUnitPrice(tmp);
	}

	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// 0. Set ups
		int k = ((Integer) getInputParameter("K")).intValue();
		int n = ((Integer) getInputParameter("N")).intValue();
		SolutionSet SD = new SolutionSet(50000);
		List<Vm> avalVmTypes = Infrastructure.createVms(0);
		@SuppressWarnings("unchecked")
		List<VmMock>[] currentVms = new List[k];

		// 1. B-Rank
		Map<Cloudlet, Integer> rank = this.bRank((VmsProblem) problem_);

		// 2. Initial k tradeoff solutions
		SolutionSet population = new SolutionSet(k);
		for (int i = 0; i < k; i++) {
			Solution empty = new Solution(problem_);
			empty.setObjective(0, 0.01); // makespan
			empty.setObjective(1, 0.01); // cost
			population.add(empty);
			currentVms[i] = new ArrayList<VmMock>();
		}

		// 3. Sort cloudlets with b-rabk
		List<MyCloudlet> sortedCloudlets = ((VmsProblem) problem_).getCloudletList();
		Collections.shuffle(sortedCloudlets, new Random(PseudoRandom.randInt()));
		Collections.sort(sortedCloudlets, (MyCloudlet one, MyCloudlet other) -> {
			return rank.get((Cloudlet) one).compareTo(rank.get((Cloudlet) other));
		});

		int var = 0;

		for (MyCloudlet adding : sortedCloudlets) { // 4 For each cloudlet
			int varI = ((VmsProblem) problem_).getCloudletList().indexOf(adding);
			for (int i = 0; i < k; i++) // filling order
				((VmEncoding) (population.get(i).getDecisionVariables()[varI])).setOrder(var);
			var++;

			
			SD.clear();

			for (int i = 0; i < k; i++) {
				Solution froniter = population.get(i);
				// case I: reusing currentVms[i]
				for (VmMock vml : currentVms[i]) {
					Solution x = new Solution(problem_);
					((VmEncoding) x.getDecisionVariables()[varI]).setTask2ins(currentVms[i].indexOf(vml));
					vml.extendFinalTime(adding.getCloudletLength() / vml.mips);
					double delta = vml.finalTime - froniter.getObjective(0);
					if (delta > 0) {
						x.setObjective(0, vml.finalTime);
						x.setObjective(1, froniter.getObjective(1) / froniter.getObjective(0) * vml.finalTime);
					} else {
						x.setObjective(0, froniter.getObjective(0));
						x.setObjective(1, froniter.getObjective(1));
					}
					SD.add(x);
				}
				// case II: assign to new vm
				if (currentVms[i].size() >= n)
					continue;
				for (Vm newAssigned : avalVmTypes) {
					int insIndex = currentVms[i].size() + 1;
					int typeIndex = avalVmTypes.indexOf(newAssigned);

					Solution x = new Solution(problem_);
					((VmEncoding) x.getDecisionVariables()[varI]).setTask2ins(insIndex);
					((VmEncoding) x.getDecisionVariables()[insIndex]).setIns2Type(typeIndex);

					VmMock newMock = new VmMock(typeIndex, (long) newAssigned.getMips(), unitPrice(newAssigned));
					currentVms[i].add(newMock);
					newMock.extendFinalTime(froniter.getObjective(0) + adding.getCloudletLength() / newMock.mips);

					x.setObjective(0, newMock.finalTime);
					x.setObjective(1, (froniter.getObjective(1) / froniter.getObjective(0) + newMock.dollarPerHr / 3600)
							* newMock.finalTime);

					SD.add(x);
				} // for newAssigned
			} // for i
			
			// step 5 sort crowd SD
			System.out.println(3);
			
		} // for step 4
		
		

		return null;
	}

}

public class MOHEFT {
	public void execMOHEFT(String dataset, int tradeOffSolNum, int maxSimultaneousIns, long seed)
			throws ClassNotFoundException, JMException {
		VmsProblem problem_ = new VmsProblem(dataset, new Random(seed));
		PseudoRandom.setRandomGenerator(new MyRandomGenerator(seed));
		Algorithm alg = new MOHEFTcore(problem_);

		alg.setInputParameter("K", tradeOffSolNum);
		alg.setInputParameter("N", maxSimultaneousIns);
		alg.execute();
		// TODO add any problems
	}

	public static void main(String[] args) throws ClassNotFoundException, JMException {
		MOHEFT testrun = new MOHEFT();
		testrun.execMOHEFT("eprotein", 10, 10, 1860);
	}
}
