package edu.ncsu.datasets;

import java.util.Random;

public class Utils {
	public static int sampleOne(int[] l, Random rand){
		return l[rand.nextInt(l.length)];
	}
}
