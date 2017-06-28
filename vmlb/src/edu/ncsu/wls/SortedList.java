package edu.ncsu.wls;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * 
 * @author jianfeng
 *
 */
public class SortedList<T extends Comparable<T>> {
	private ArrayList<T> content;

	public SortedList() {
		content = new ArrayList<T>();
	}

	/**
	 * inserting, keeping the order of content.
	 * 
	 * if value already exist. then do nothing!!!
	 * 
	 * @param value
	 */
	public void add(T value) {
		System.out.println(content);
		int i = 0;
		int j = content.size() - 1;
		int mid = (i + j) / 2;
		while (i < j) {
			if (value.equals(content.get(mid))) {
				return;
			}

			if (value.compareTo(content.get(mid)) < 0)
				i = mid + 1;
			else
				j = mid - 1;
		}
		content.add(i, value);
	}

	public int getSize() {
		return this.content.size();
	}

	public String toString() {
		String res = "[";
		for (T c : content)
			res += c.toString() + ", ";
		res = res.substring(0, res.length() - 2);
		res += "]";

		return res;
	}

	/**
	 * Demonstration and testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// SortedList<Integer> t = new SortedList<Integer>();
		// t.add(3);
		// t.add(4);
		// t.add(3);
		// t.add(1);
		// t.add(0);
		// System.out.println(t);
		TreeSet<Integer> t = new TreeSet<Integer>();
		t.add(3);
		t.add(4);
		t.add(3);
		t.add(1);
		t.add(0);
		
		System.out.println(t.higher(2));
	}
}
