/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Task2 extends Task {
	public static void main(String[] args) {
		Client task = new Client(Helper.argsCheck(args, 2));
		ExecutorService browserExec = Executors.newSingleThreadExecutor();
		ExecutorService printerExec = Executors.newSingleThreadExecutor();
		task.setBrowserExec(browserExec).setPrinterExec(printerExec);
		task.search();
		task.outputResults(null, null);
		browserExec.shutdown();
		printerExec.shutdown();
	}
}
