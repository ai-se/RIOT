package edu.ncsu.experiments;

import java.io.IOException;
import java.util.HashMap;

import org.cloudbus.cloudsim.Log;

import edu.ncsu.algorithms.EMSC;
import edu.ncsu.algorithms.SWAY;
import edu.ncsu.wls.INFRA;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

public class ExpDynamicCmpr {

	private static SolutionSet MOEA_Static(String dataset) throws ClassNotFoundException, JMException {
		INFRA.staticMode = true;
		
		HashMap<String, Object> exp_para = new HashMap<String, Object>();
		exp_para.put("popSize", 50);
		exp_para.put("maxEval", 50 * 1000);
		exp_para.put("cxProb", 1.0);
		exp_para.put("cxRandChangeProb", 0.6);
		exp_para.put("mutProb", 0.6);
		exp_para.put("bitMutProb", 1.0);
		exp_para.put("algorithm", "MOEAD");
		exp_para.put("seed", System.currentTimeMillis());
		exp_para.put("algorithm", "NSGAII");
		exp_para.put("dataset", dataset);

		SolutionSet res = new EMSC().execute(exp_para);

		return res;
	}

	private static SolutionSet SWAY_Static(String dataset) throws ClassNotFoundException, JMException {
		INFRA.staticMode = true;
		
		HashMap<String, Object> exp_para = new HashMap<String, Object>();
		exp_para.put("seed", System.currentTimeMillis());
		exp_para.put("dataset", dataset);
		
		SolutionSet res = new SWAY().executeSWAY(exp_para);
		return res;
	}
	
	
	
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

		// File file = new File("dynamic.txt");

		for (int repeat = 0; repeat < repeats; repeat++) {
			for (String s : models) {
				SolutionSet moea = MOEA_Static(s);
				SolutionSet our_S = SWAY_Static(s);
				
				// print out compared results
				
			}

		}
	}
}
