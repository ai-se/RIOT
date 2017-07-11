package edu.ncsu.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.cloudbus.cloudsim.Log;

import edu.ncsu.algorithms.EMSC;
import edu.ncsu.wls.Infrastructure;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * Experiment. for each model, run EMSC-NSGAII algorithm. record the
 * hall-of-fame during the iteration Repeats = 1
 * 
 * @author jianfeng
 *
 */

public class ExpEMSCN {
	public static void main(String[] args) throws IOException, ClassNotFoundException, JMException {
		int repeats = 1;
		String[] models;

		if (args.length > 0)
			repeats = Integer.parseInt(args[0]);
		if (args.length > 1 && !args[1].equals("small"))
			models = new String[] { args[1] };
		if (args.length > 1 && args[1].equals("small"))
			models = Infrastructure.smallmodels;
		else
			models = Infrastructure.models;

		Log.disable();
		File file = new File("emsc-nsgaii.txt");

		HashMap<String, Object> exp_para = new HashMap<String, Object>();
		exp_para.put("popSize", 50);
		exp_para.put("maxEval", 50 * 1000);
		exp_para.put("cxProb", 0.6);
		exp_para.put("cxRandChangeProb", 0.1);
		exp_para.put("mutProb", 0.8);
		exp_para.put("bitMutProb", 0.4);

		for (int repeat = 0; repeat < repeats; repeat++) {
			exp_para.put("seed", System.currentTimeMillis() + (long) repeat);
			for (String s : models) {
				System.out.println("Running in " + s);
				exp_para.put("dataset", s);

				long startTime = System.currentTimeMillis();
				SolutionSet res = new EMSC().execNSGAII(exp_para);

				String output = "";
				output += ("* " + s + " " + (System.currentTimeMillis() - startTime) / 1000 + "\n");
				for (int v = 0; v < res.size(); v++) {
					output += (res.get(v).getObjective(0) + " " + res.get(v).getObjective(1));
					output += "\n";
				}

				output += ("#\n"); // tag for execution
				BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
				out.write(output);
				out.flush();
				out.close();
			} // for model s
		} // for repeat
	}
}