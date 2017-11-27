package edu.ncsu.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.cloudbus.cloudsim.Log;

import edu.ncsu.algorithms.EMSC;
import edu.ncsu.model.INFRA;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * This is the Entrance of Experiments to run EMSC algorithm 
 * [repeat] [model] [alg]
 * 
 * @author jianfeng
 */

public class ExpEMSC {
	public static HashMap<String, Object> general_para = new HashMap<String, Object>();

	public static HashMap<String, Object> moead_para = new HashMap<String, Object>();
	public static HashMap<String, Object> nsgaii_para = new HashMap<String, Object>();
	public static HashMap<String, Object> spea2_para = new HashMap<String, Object>();

	static {
		general_para.put("popSize", 50);
		general_para.put("maxEval", 50 * 1000);
		general_para.put("cxProb", 1.0);
		general_para.put("cxRandChangeProb", 0.6);
		general_para.put("mutProb", 0.6);
		general_para.put("bitMutProb", 1.0);

		moead_para.put("algorithm", "MOEAD");
		nsgaii_para.put("algorithm", "NSGAII");
		spea2_para.put("arxvSize", 10);
		spea2_para.put("algorithm", "SPEA2");
	}

	public static void main(String[] args) throws ClassNotFoundException, JMException, IOException {
		int repeats;
		String[] models;
		String alg;

		if (args.length > 0)
			repeats = Integer.parseInt(args[0]);
		else
			repeats = 1;

		if (args.length > 1) {
			if (args[1].equals("small"))
				models = INFRA.smallmodels;
			else if (args[1].equals("all"))
				models = INFRA.models;
			else
				models = new String[] { args[1] };
		} else
			models = INFRA.models;

		if (args.length > 2) {
			alg = args[2];
		} else
			alg = "moead";

		Log.disable();
		File file = new File("emsc-" + alg + ".txt");

		HashMap<String, Object> exp_para = new HashMap<String, Object>();
		exp_para.putAll(general_para);
		switch (alg) {
		case "moead":
			exp_para.putAll(moead_para);
			break;
		case "nsgaii":
			exp_para.putAll(nsgaii_para);
			break;
		case "spea2":
			exp_para.putAll(spea2_para);
			break;
		}

		for (int repeat = 0; repeat < repeats; repeat++) {
			exp_para.put("seed", System.currentTimeMillis() + (long) repeat);
			for (String s : models) {
				System.out.println("Running in " + s);
				exp_para.put("dataset", s);

				long startTime = System.currentTimeMillis();
				SolutionSet res = new EMSC().execute(exp_para);

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
