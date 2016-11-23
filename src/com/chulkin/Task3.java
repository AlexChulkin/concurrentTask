
/*
 * Copyright Alex Chulkin (c) 2016
 */

package com.chulkin;

import java.util.concurrent.Executors;

public class Task3 extends ProtoTask {
	public static void main(String[] args) {
		Server server = new Server(Helper.serverArgsCheck(args), Executors.newSingleThreadExecutor());
		server.setDispatcherExec(Executors.newCachedThreadPool()).setBrowserExec(Executors.newSingleThreadExecutor());
		server.setPrinterExec(Executors.newCachedThreadPool());
		server.execute();
		Helper.tryToSleep(Helper.MAXIMAL_WAIT);
		System.out.println(Helper.SERVER_STOPPING);
		server.stop();
	}
}
