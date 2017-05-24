package edu.ncsu.algorithms;

class Dad {
	private static final String me = "dad";

	protected String getMe() {
		return me;
	}

	public void printMe() {
		System.out.println(getMe());
	}
}

class Son extends Dad {
	private static final String me = "son";

//	@Override
//	protected String getMe() {
//		return me;
//	}

}

public class Test {

	public static void main(String[] args) {
		new Son().printMe(); // Prints "son"
	}

}
