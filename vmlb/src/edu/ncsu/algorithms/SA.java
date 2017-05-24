package edu.ncsu.algorithms;

import java.util.Arrays;
import java.util.Random;

import edu.ncsu.datasets.Utils;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.util.JMException;

/**
 * Simulated annealing algorithm
 * 
 * @author jianfeng
 *
 */
public class SA {
	// private int pop;
	private int iterNum;
	private int temp;
	private double tempRR;
	private double fm = Double.MAX_VALUE;
	private double fM = -Double.MAX_VALUE;
	private VmsProblem problem_;

	public SA(String dataset, int iterNum, int temperature, double temperatureReduceRate, long seed) {
		this.problem_ = new VmsProblem(dataset, new Random(seed));
		this.iterNum = iterNum;
		this.temp = temperature;
		this.tempRR = temperatureReduceRate;

		try {
			this.selfTune();
		} catch (ClassNotFoundException | JMException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The purpose for this method is to get the Energy normalization factor
	 * 
	 * @throws ClassNotFoundException
	 * @throws JMException
	 */
	private void selfTune() throws ClassNotFoundException, JMException {
		Solution randSolution;
		for (int i = 0; i < 50; i++) {
			randSolution = new Solution(problem_);
			problem_.evaluate(randSolution);
			double dd = randSolution.getObjective(0);
			fm = Math.min(dd, fm);
			fM = Math.max(dd, fM);
		}
		fm -= (fM - fm) * 0.2;
		fM += (fM - fm) * 0.15;
	}

	private double getEnergy(Solution c) {
		try {
			problem_.evaluate(c);
		} catch (JMException e) {
			e.printStackTrace();
		}
		return (c.getObjective(0) - fm) / (fM - fm);
	}

	private Solution neighbor(Solution c) {
		Variable[] newmap = c.getDecisionVariables();
		newmap[problem_.randInt(problem_.getNumberOfVariables())] = new VMLoc(
				Utils.sampleOne(problem_.getVmids(), problem_.rand));

		Solution res = null;
		try {
			res = new Solution(problem_);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		res.setDecisionVariables(newmap);
		return res;
	}

	@SuppressWarnings("unused") // Solution best
	public double[] execSA() {
		double[] recorder = new double[this.iterNum];
		Solution best, neighbor, current = null;

		try {
			current = new Solution(problem_);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		best = current;

		double eb, en, ec;
		ec = getEnergy(current);
		eb = ec;

		double temp = this.temp;
		for (int i = 0; i < iterNum; i++) {
			if (i % 50 == 0)
				System.out.println("Iteration" + i);
			neighbor = this.neighbor(current);
			en = getEnergy(neighbor);
			if (en < eb) {
				best = neighbor;
				eb = en;
			}
			if (en < ec) {
				current = neighbor;
				ec = en;
			} else if (Math.exp((ec - en) / temp) > Math.random()) {
				current = neighbor;
				ec = en;
			}
			temp *= tempRR;
			recorder[i] = current.getObjective(0);
		}
		return recorder;
	}

	public static void main(String[] args) {
		SA sarunner = new SA("eprotein", 1000, 10, 0.9, System.currentTimeMillis());
		double[] res = sarunner.execSA();
		System.out.println(Arrays.toString(res));
	}
}
