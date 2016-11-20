
/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.util.concurrent.Executors;

public class Task3 extends ProtoTask {
	public static void main(String[] args) {
		Server server = new Server(Helper.serverArgsCheck(args), Executors.newSingleThreadExecutor());
		server.setDispatcherExec(Executors.newCachedThreadPool()).setBrowserExec(Executors.newSingleThreadExecutor());
		server.setPrinterExec(Executors.newCachedThreadPool());
		server.execute();
		try {
			Thread.sleep(Helper.MAXIMAL_WAIT);
		} catch (InterruptedException e) {
			throw new RuntimeException(Helper.INTERRUPTED_DURING_SLEEP, e);
		}
		System.out.println(Helper.SERVER_STOPPING);
		server.stop();
	}
}
