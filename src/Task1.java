/*
 * Copyright Alex Chulkin (c) 2016
 */

public class Task1 extends ProtoTask {
	public static void main(String[] args) {
		Client client = new Client(Helper.clientArgsCheck(args, 1));
		client.search();
		client.output(null, null);
	}
}
