package edu.ncsu.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.cloudbus.cloudsim.Log;

import edu.ncsu.algorithms.RIOT;
import edu.ncsu.model.INFRA;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * Experiment. for each model, run ENSC-SPEA2 algorithm. record the hall-of-fame
 * during the iteration Repeats = 1
 * 
 * @author jianfeng
 *
 */

public class ExpRIOT {
	public static void main(String[] args) throws IOException, ClassNotFoundException, JMException {
		int repeats = 1;
		String[] models;

		if (args.length > 0)
			repeats = Integer.parseInt(args[0]);
		if (args.length > 1) {
			if (args[1].equals("small"))
				models = INFRA.smallmodels;
			else
				models = new String[] { args[1] };
		} else
			models = INFRA.models;

		Log.disable();
		File file = new File("riot.txt");

		HashMap<String, Object> exp_para = new HashMap<String, Object>();
		for (int repeat = 0; repeat < repeats; repeat++) {
			exp_para.put("seed", System.currentTimeMillis() + (long) repeat);
			for (String s : models) {
				System.out.println("Running in " + s);
				exp_para.put("dataset", s);

				long startTime = System.currentTimeMillis();
				SolutionSet res = new RIOT().executeSWAY(exp_para);

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