package edu.ncsu.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.cloudbus.cloudsim.Log;

import edu.ncsu.algorithms.SA;

/**
 * Experiment. for each model, run simulated algorithm algorithm. record the
 * hall-of-fame during the iteration Repeats = 1
 * 
 * @author jianfeng
 *
 */
public class ExpSA {
	public static void main(String[] args) throws IOException {
		Log.disable();
		File file = new File("lst.csv");
		// file.createNewFile();
		BufferedWriter out = new BufferedWriter(new FileWriter(file));

		HashMap<String, Object> exp_para = new HashMap<String, Object>();
		exp_para.put("seed", System.currentTimeMillis());
		exp_para.put("maxIterations", 1000);
		exp_para.put("temperature", 10);
		exp_para.put("temperatureReduceRate", 0.9);

		String[] models = new String[] { "fmri", "eprotein", "j30", "j60", "j90" };
		for (String s : models) {
			System.out.println("Running in " + s);
			exp_para.remove("dataset");
			exp_para.put("dataset", s);

			double[] res = new SA(exp_para).execSA();
			for (int i = 0; i < res.length; i++) {
				out.write(s + ","); // dataset name
				out.write(i + ","); // iteration
				out.write(res[i] + "\n"); // hall-of-fame makespan
			}
			out.flush();
		}
		out.close();
	}
}
