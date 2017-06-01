package edu.ncsu.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.cloudbus.cloudsim.Log;

import edu.ncsu.algorithms.GA;
import edu.ncsu.wls.Infrastructure;

/**
 * Experiment. for each model, run simulated algorithm algorithm. record the
 * hall-of-fame during the iteration Repeats = 1
 * 
 * @author jianfeng
 *
 */
public class ExpGA {
	public static void main(String[] args) throws IOException {
		int repeats = 1;
		if (args.length > 0)
			repeats = Integer.parseInt(args[0]);

		Log.disable();
		File file = new File("ga.csv");

		HashMap<String, Object> exp_para = new HashMap<String, Object>();
		exp_para.put("popSize", 50);
		exp_para.put("maxIterations", 500);
		exp_para.put("maxEvaluations", Integer.MAX_VALUE);
		exp_para.put("cxRate", 0.95);
		exp_para.put("muRate", 0.95);

		String[] models = Infrastructure.models;
		for (int repeat = 0; repeat < repeats; repeat++) {
			exp_para.put("seed", System.currentTimeMillis() + (long) repeat);
			for (String s : models) {
				System.out.println("Running in " + s);
				exp_para.remove("dataset");
				exp_para.put("dataset", s);

				double[] res = new GA(exp_para).execGA();
				String output = "";
				for (int i = 0; i < res.length; i++) {
					output += (s + ","); // dataset name
					output += (i + ","); // iteration
					output += (res[i] + "\n"); // hall-of-fame makespan
				}
				output += ("*==...\n"); // tag for ending one model
				BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
				out.write(output);
				out.flush();
				out.close();
			} // for model s
		} // for repeat
	}
}