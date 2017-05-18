package edu.ncsu.algorithms;

import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Vm;

import edu.ncsu.wls.Infrastructure;
import edu.ncsu.wls.MyCloudlet;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.util.JMException;

class VMS extends Problem {
	private String dataset;
	public int[] vmid;
	private double fm = Double.MAX_VALUE;
	private double fM = -Double.MAX_VALUE;
	private Random rand;

	@SuppressWarnings("unchecked")
	public VMS(String dataset, long seed) {
		rand = new Random(seed);

		this.dataset = dataset;
		this.numberOfVariables_ = ((List<MyCloudlet>) (Infrastructure.getCaseCloudlets(dataset, -1)[0])).size();
		this.numberOfObjectives_ = 1;

		List<Vm> vms = Infrastructure.createVms(-1);
		vmid = new int[vms.size()];
		for (int i = 0; i < vms.size(); i++)
			vmid[i] = vms.get(i).getId();

		// self-tuning, setting for objective normalization factor, in case we
		// need that
		selfTune();
	}

	private void selfTune() {
		for (int x = 0; x < 50; x++) { // Tune 50 times
			int[] r = new int[this.numberOfVariables_];
			for (int i = 0; i < this.numberOfVariables_; i++)
				r[i] = vmid[rand.nextInt(vmid.length)];
			Chromosome d = new Chromosome(r);
			double dd = d.getMakespan();
			fm = Math.min(dd, fm);
			fM = Math.max(dd, fM);
		}
		fm -= (fM - fm) * 0.2;
		fM += (fM - fm) * 0.15;
	}

	@Override
	public void evaluate(Solution solution) throws JMException {
		// TODO Auto-generated method stub

	}

}