package edu.ncsu.algorithms;

public class Chromosome {
	private int[] mapping;
	private double makespan;
	private boolean isEval;

	public Chromosome(int[] mapping) {
		this.mapping = mapping;
		this.isEval = false;
	}

	public double getMakespan() {
		if (!isEval)
			return Double.MAX_VALUE;
		return makespan;
	}

	public void setMakespan(double makespan) {
		this.makespan = makespan;
		this.isEval = true;
	}

	public void resetMapping(int[] newmap) {
		this.mapping = newmap;
		this.isEval = false;
	}
	
	public int[] getMapping(){
		return mapping;
	}
	
	public boolean isEvaluated(){
		return isEval;
	}
}


