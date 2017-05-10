package edu.ncsu.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.cloudbus.cloudsim.Log;

import edu.ncsu.strategies.SA;

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
		String[] models = new String[] { "fmri", "eprotein", "j30", "j60", "j90" };
		for (String s : models) {
			System.out.println("Running in " + s);
			double[] res = new SA(s, 1000, 10, 0.9, System.currentTimeMillis()).execSA();
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
