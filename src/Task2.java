/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.*;

public class Task2 extends Task1 {
	private BlockingQueue<String> resultsBlockingQueue = new LinkedBlockingQueue<>();
	private boolean finished = false;

	private Task2(Pair<Pair<String, String>, Integer> checkedArgs, ExecutorService execSearcher) {
		super(checkedArgs.getFirst().getFirst(), checkedArgs.getFirst().getSecond(), checkedArgs.getSecond());
		Thread.setDefaultUncaughtExceptionHandler(new LocalUncaughtExceptionHandler());
		execSearcher.execute(this::search);
	}

	private Task2(Pair<Pair<String, String>, Integer> checkedArgs, ExecutorService execSearcher,
				  ExecutorService execPrinter) {
		this(checkedArgs, execSearcher);
        execPrinter.execute(() -> outputResults(null, null));
		execSearcher.shutdown();
		execPrinter.shutdown();
	}

    Task2(Pair<Pair<String, String>, Integer> checkedArgs, ExecutorService execSearcher, int id, PrintWriter pw) {
        this(checkedArgs, execSearcher);
        execSearcher.execute(() -> outputResults(id, pw));
    }

	public static void main(String[] args) {
		new Task2(argsCheck(args, 2), Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
	}

	@Override
	public void search() {
		try {
			super.search();
		} catch (IOException e) {
			throw new RuntimeException(Helper.IO_ERROR, e);
		}
		try {
			stopPrinter();
		} catch (InterruptedException e) {
			throw new RuntimeException(Helper.INTERRUPTED_IN_BLOCKING_QUEUE, e);
		}
	}

	private void outputResults(Integer id, PrintWriter pw) {
		boolean empty = true;
		try {
			while (!Thread.interrupted()) {
				String res = resultsBlockingQueue.poll(Helper.MAXIMAL_WAIT,
						TimeUnit.SECONDS);
				if (res == null) {
					throw new RuntimeException(Helper.APPLICATION_IS_DEADLOCKED);
				}

				if (!res.equals(Helper.END_FLAG)) {
					if (empty) {
						println(Helper.RESULTS, pw);
						serverOutput(id, Helper.RESULTS);
						empty = false;
					}
					println(res, pw);
					serverOutput(id, res);

				} else {

					if (empty){
						println(Helper.NO_RESULTS, pw);
						serverOutput(id, Helper.NO_RESULTS);
					}
					break;
				}
			}
			finished = true;
		} catch (InterruptedException e) {
			throw new RuntimeException(Helper.INTERRUPTED_IN_BLOCKING_QUEUE, e);
		}
	}

	private void serverOutput(Integer id,String s){
		if(id==null)
			return;
		System.out.format(Helper.CLIENT_SOCKET_OUTPUT, id, s);
	}

	@Override
	void addPathToResults(String filename) {
		try {
			resultsBlockingQueue.put(filename);
		} catch (InterruptedException e) {
			throw new RuntimeException(Helper.INTERRUPTED_IN_BLOCKING_QUEUE, e);
		}
	}

	private void stopPrinter() throws InterruptedException {
		resultsBlockingQueue.put(Helper.END_FLAG);
	}

	boolean isFinished() {
		return finished;
	}

	private static class LocalUncaughtExceptionHandler implements
			Thread.UncaughtExceptionHandler {

		public void uncaughtException(Thread t, Throwable e) {
			System.err.println(Helper.EXCEPTION + e.getMessage() + Helper.THROWN_IN_THREAD + t + "\n");
			e.printStackTrace();
		}
	}
}
