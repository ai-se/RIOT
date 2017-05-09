package edu.ncsu.strategies;

import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Vm;

import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudlet;

public class SA {
	private String dataset;
	private int pop;
	private int iterNum;
	private int cloudletNum;
	private int[] vmid;
	private Random rand;

	@SuppressWarnings("unchecked")
	public SA(String dataset, int pop, int iterNum, long seed) {
		rand = new Random(seed);
		this.dataset = dataset;
		this.pop = pop;
		this.iterNum = iterNum;

		List<Vm> vms = Infrastructure.createVms(-1);
		vmid = new int[vms.size()];
		for (int i = 0; i < vms.size(); i++)
			vmid[i] = vms.get(i).getId();

		cloudletNum = ((List<MyCloudlet>) (Infrastructure.getCaseCloudlets(dataset, -1)[0])).size();
	}

	private Chromosome randomChromosome() {
		int[] res = new int[cloudletNum];
		for (int i = 0; i < cloudletNum; i++)
			res[i] = vmid[rand.nextInt(vmid.length)];

		return new Chromosome(res);
	}

	public double execSA() {
		Chromosome[] populations = new Chromosome[pop];
		for (int i = 0; i < pop; i++)
			populations[i] = randomChromosome();

		for (int iter = 0; iter < iterNum; iter++) {
			
		}
		return -1.0;
	}
}
