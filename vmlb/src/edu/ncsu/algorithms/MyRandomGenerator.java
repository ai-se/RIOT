package edu.ncsu.algorithms;

import java.util.Random;

import jmetal.util.IRandomGenerator;

/**
 * This code has been taken from deb NSGA-II implementation The code is
 * available to download from http://www.iitk.ac.in/kangal/codes.shtml
 *
 */

/**
 * Extended with an additional user-specific seed
 * @author jianfeng
 *
 */
public class MyRandomGenerator implements IRandomGenerator {

	/* Definition of random number generation routines */
	double seed;
	double[] oldrand = new double[55];
	int jrand;

	/**
	 * Constructor
	 */
	public MyRandomGenerator() {
		this.seed = (new Random(System.nanoTime())).nextDouble();
		this.randomize();
	} // RandomGenerator

	public MyRandomGenerator(long seed) {
		this.seed = (new Random(seed)).nextDouble();
		this.randomize();
	}

	/* Get seed number for random and start it up */
	void randomize() {
		int j1;
		for (j1 = 0; j1 <= 54; j1++) {
			oldrand[j1] = 0.0;
		}
		jrand = 0;
		warmup_random(seed);
	} // randomize

	/* Get randomize off and running */
	void warmup_random(double seed) {
		int j1, ii;
		double new_random, prev_random;
		oldrand[54] = seed;
		new_random = 0.000000001;
		prev_random = seed;
		for (j1 = 1; j1 <= 54; j1++) {
			ii = (21 * j1) % 54;
			oldrand[ii] = new_random;
			new_random = prev_random - new_random;
			if (new_random < 0.0) {
				new_random += 1.0;
			}
			prev_random = oldrand[ii];
		}
		advance_random();
		advance_random();
		advance_random();
		jrand = 0;
	} // warmup_random

	/* Create next batch of 55 random numbers */
	void advance_random() {
		int j1;
		double new_random;
		for (j1 = 0; j1 < 24; j1++) {
			new_random = oldrand[j1] - oldrand[j1 + 31];
			if (new_random < 0.0) {
				new_random = new_random + 1.0;
			}
			oldrand[j1] = new_random;
		}
		for (j1 = 24; j1 < 55; j1++) {
			new_random = oldrand[j1] - oldrand[j1 - 24];
			if (new_random < 0.0) {
				new_random = new_random + 1.0;
			}
			oldrand[j1] = new_random;
		}
	} // advance_ramdom

	/* Fetch a single random number between 0.0 and 1.0 */
	double randomperc() {
		jrand++;
		if (jrand >= 55) {
			jrand = 1;
			advance_random();
		}
		return ((double) oldrand[jrand]);
	} // randomPerc

	/* Fetch a single integer between 0 and upperbound */
	synchronized public int nextInt(int upperBound) {
		return rndInt(0, upperBound);
	}

	/* Fetch a single double between 0.0 and 1.0 */
	synchronized public double nextDouble() {
		return randomperc();
	}

	/*
	 * Fetch a single random integer between low and high including the bounds
	 */
	synchronized public int rndInt(int low, int high) {
		int res;
		if (low >= high) {
			res = low;
		} else {
			res = low + (int) (randomperc() * (high - low + 1));
			if (res > high) {
				res = high;
			}
		}
		return (res);
	} // rnd

	/* Fetch a single random real number between low and high including the */
	/* bounds */
	synchronized public double rndReal(double low, double high) {
		return (low + (high - low) * randomperc());
	} // rndreal
} // RandomGenerator
