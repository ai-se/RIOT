package edu.ncsu.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.cloudbus.cloudsim.Log;

import edu.ncsu.algorithms.SA;
import edu.ncsu.wls.Infrastructure;

/**
 * Experiment. for each model, run simulated algorithm algorithm. record the
 * hall-of-fame during the iteration Repeats = 1
 * 
 * @author jianfeng
 *
 */
public class ExpSA {
	public static void main(String[] args) throws IOException {
		int repeats = 1;
		if (args.length > 0)
			repeats = Integer.parseInt(args[0]);

		Log.disable();
		File file = new File("sa.csv");

		HashMap<String, Object> exp_para = new HashMap<String, Object>();
		exp_para.put("maxIterations", 1000);
		exp_para.put("temperature", 10);
		exp_para.put("temperatureReduceRate", 0.9);

		String[] models = Infrastructure.models;
		for (int repeat = 0; repeat < repeats; repeat++) {
			exp_para.put("seed", System.currentTimeMillis() + (long) repeat);
			for (String s : models) {
				System.out.println("Running in " + s);
				exp_para.remove("dataset");
				exp_para.put("dataset", s);

				double[] res = new SA(exp_para).execSA();
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