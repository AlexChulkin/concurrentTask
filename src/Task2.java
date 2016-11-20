
/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Task2 extends ProtoTask {
	public static void main(String[] args) {
		Client client = new Client(Helper.clientArgsCheck(args, 2));
		ExecutorService browserExec = Executors.newSingleThreadExecutor();
		ExecutorService printerExec = Executors.newSingleThreadExecutor();
		client.setBrowserExec(browserExec).setPrinterExec(printerExec);
		client.search();
		client.output(null, null);
		browserExec.shutdown();
		printerExec.shutdown();
	}
}
