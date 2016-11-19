/*
 * Copyright Alex Chulkin (c) 2016
 */

public class Task1 extends Task {
	public static void main(String[] args) {
		Client task = new Client(Helper.argsCheck(args, 1));
		task.search();
		task.outputResults(null, null);
	}
}
