package edu.ncsu.strategies;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Vm;

import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudlet;

/**
 * Simulated annealing algorithm
 * 
 * @author jianfeng
 *
 */
public class SA {
	private String dataset;
	// private int pop;
	private int iterNum;
	private int cloudletNum;
	private int[] vmid;
	private int temp;
	private double tempRR;
	private double fm = Double.MAX_VALUE;
	private double fM = -Double.MAX_VALUE;
	private Random rand;

	@SuppressWarnings("unchecked")
	public SA(String dataset, int iterNum, int temperature, double temperatureReduceRate, long seed) {
		rand = new Random(seed);
		this.dataset = dataset;
		// this.pop = pop;
		this.iterNum = iterNum;
		this.temp = temperature;
		this.tempRR = temperatureReduceRate;

		List<Vm> vms = Infrastructure.createVms(-1);
		vmid = new int[vms.size()];
		for (int i = 0; i < vms.size(); i++)
			vmid[i] = vms.get(i).getId();

		cloudletNum = ((List<MyCloudlet>) (Infrastructure.getCaseCloudlets(dataset, -1)[0])).size();
		this.selfTune();
	}

	/**
	 * initial population - randomly assigned
	 * 
	 * @return
	 */
	private Chromosome randChromosome() {
		int[] res = new int[cloudletNum];
		for (int i = 0; i < cloudletNum; i++)
			res[i] = vmid[rand.nextInt(vmid.length)];

		return new Chromosome(res);
	}

	/**
	 * The purpose for this method is to get the Energy normaization factor
	 */
	private void selfTune() {
		for (int i = 0; i < 50; i++) {
			Chromosome d = randChromosome();
			CoreEval.eval(dataset, d);
			double dd = d.getMakespan();
			fm = Math.min(dd, fm);
			fM = Math.max(dd, fM);
		}
		fm -= (fM - fm) * 0.2;
		fM += (fM - fm) * 0.15;

	}

	private double getEnergy(Chromosome c) {
		CoreEval.eval(dataset, c);
		return (c.getMakespan() - fm) / (fM - fm);
	}

	private Chromosome neighbor(Chromosome c) {
		int[] mapping = c.getMapping();
		int[] newmap = mapping;
		newmap[rand.nextInt(mapping.length)] = vmid[rand.nextInt(vmid.length)];

		return new Chromosome(newmap);
	}

	public double[] execSA() {
		double[] res = new double[this.iterNum];
		Chromosome best, neighbor, current;

//		int[] initm = new int[this.cloudletNum];
//		Arrays.fill(initm, vmid[0]);
		current = randChromosome();
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
			res[i] = current.getMakespan();
		}
		return res;
	}

	public static void main(String[] args) {
		SA sarunner = new SA("eprotein", 1000, 10, 0.9, System.currentTimeMillis());
		double[] res = sarunner.execSA();
		System.out.println(Arrays.toString(res));
	}
}
