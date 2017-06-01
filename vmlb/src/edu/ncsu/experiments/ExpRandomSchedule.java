package edu.ncsu.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import edu.ncsu.algorithms.RandomSchedule;
import edu.ncsu.wls.Infrastructure;

/**
 * Experiment 1: for all dataset, repeat 30 times. Randomly assign the task,
 * return the makespan into lst.csv
 * 
 * @author jianfeng
 *
 */
public class ExpRandomSchedule {
	public static void main(String[] args) throws IOException {
		File file = new File("exp1_random.csv");
		// file.createNewFile();
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		String[] mockargs = new String[1];
		String[] models = Infrastructure.models;
		for (String s : models) {
			mockargs[0] = s;
			int repeat = 30;
			for (int i = 1; i <= repeat; i++) {
				Map<String, Object> res = RandomSchedule.core(mockargs);
				out.write(res.get("dataset").toString());
				out.write(",");
				out.write(res.get("makespan").toString());
				out.write(",");
				for (Integer tmp : (int[]) res.get("vmid"))
					out.write(tmp.toString() + "|");
				out.write("\n");
			}
			out.flush();
		}
		out.close();
	}
}
