/**
 * Second task.
 * Uses two threads: one for the search, another for the printing of the results
 * @author Alex Chulkin
 * 
 */
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Task2 extends Task1 {
	/*
	 * The group of string constants
	 */
	static final String APPLICATION_IS_DEADLOCKED = "New sockets are not connecting for a long time "
			+ "or the application is deadlocked";
	static final Long MAXIMAL_WAIT = 150L;
	static final String INTERRUPTED_IN_BLOCKING_QUEUE = "Interrupted in blocking queue";
	private static final String END_FLAG = "";
	private static final String EXCEPTION = "Exception ";
	private static final String THROWN_IN_THREAD = " thrown in thread ";
	private static final String CLIENT_SOCKET_OUTPUT = "Client socket #%d prints: %s\n";

	/*
	 * The blocking queue of results, used instead of arraylist in Task1
	 */
	private BlockingQueue<String> resultsBlockingQueue = new LinkedBlockingQueue<String>();
	/*
	 * Flag to show if we have finished to output the results
	 */
	private boolean finished = false;

	/*
	 * Local thread uncaught exceptions handler
	 */
	static class LocalUncaughtExceptionHandler implements
			Thread.UncaughtExceptionHandler {
		/*
		 * Specifies the reaction to the given uncaught exception in the given
		 * thread
		 * 
		 * @param t the given thread
		 * 
		 * @param e the given exception
		 */
		public void uncaughtException(Thread t, Throwable e) {
			System.err.println(EXCEPTION + e.getMessage() + THROWN_IN_THREAD
					+ t + "\n");
			e.printStackTrace();
		}
	}


    Task2(Pair<Pair<String, String>, Integer> checkedArgs, ExecutorService execSearcher) {
        super(checkedArgs.getFirst().getFirst(), checkedArgs.getFirst().getSecond(), checkedArgs.getSecond());
        Thread.setDefaultUncaughtExceptionHandler(new LocalUncaughtExceptionHandler());
        execSearcher.execute(() -> search());
    }
	/*
	 * The Task2's constructor, see the {@link Task1#Task1(String,String,int)
	 * Task1#Task1(String,String,int)} also sets the uncaught exception handler
	 * for all the threads and executors run in this class and its descendants
	 */
	Task2(Pair<Pair<String, String>, Integer> checkedArgs, ExecutorService execSearcher, ExecutorService execPrinter) {
		this(checkedArgs, execSearcher);
        execPrinter.execute(() -> outputResults(null, null));
	}

    Task2(Pair<Pair<String, String>, Integer> checkedArgs, ExecutorService execSearcher, int id, PrintWriter pw) {
        this(checkedArgs, execSearcher);
        execSearcher.execute(() -> outputResults(id, pw));
    }


	/*
	 * The search method, used by the {@link #run() run()} method. Overrides the
	 * parent's search method and catches the io exception. Also calls the
	 * {@link #stopPrinter() stopPrinter()} method.
	 */
	@Override
	void search() {
		try {
			super.search();
		} catch (IOException e) {
			throw new RuntimeException(IO_ERROR, e);
		}
		try {
			stopPrinter();

		} catch (InterruptedException e) {
			throw new RuntimeException(INTERRUPTED_IN_BLOCKING_QUEUE, e);
		}
	}

	/*
	 * The overloaded version of the {@link Task1#outputResults(PrintWriter)
	 * Task1#outputResults(PrintWriter)} method. Works with the blocking queue
	 * instead of the arraylist and the flag signalling that there will be no
	 * new results, see the {@link #stopPrinter() stopPrinter()} method
	 */
	void outputResults(Integer id, PrintWriter pw) {
		boolean empty = true;

		try {
			while (!Thread.interrupted()) {
				String res = resultsBlockingQueue.poll(MAXIMAL_WAIT,
						TimeUnit.SECONDS);
				if (res == null) {
					throw new RuntimeException(APPLICATION_IS_DEADLOCKED);
				}

				if (!res.equals(END_FLAG)) {
					if (empty) {
						println(RESULTS, pw);
						serverOutput(id,RESULTS);
						empty = false;
					}
					println(res, pw);
					serverOutput(id, res);

				} else {

					if (empty){
						println(NO_RESULTS, pw);
						serverOutput(id,NO_RESULTS);
					}	
					break;
				}
			}
			finished = true;
		} catch (InterruptedException e) {
			throw new RuntimeException(INTERRUPTED_IN_BLOCKING_QUEUE, e);
		}
	}
	
	private void serverOutput(Integer id,String s){
		if(id==null)
			return;
		System.out.format(CLIENT_SOCKET_OUTPUT,id,s);
		
	}

	/*
	 * The updated version of the {@link Task1#addPathToResults(String)
	 * Task1#addPathToResults(String)} method. Works with the blocking queue
	 * instead of the arraylist
	 */
	@Override
	void addPathToResults(String filename) {
		try {

			resultsBlockingQueue.put(filename);

		} catch (InterruptedException e) {
			throw new RuntimeException(INTERRUPTED_IN_BLOCKING_QUEUE, e);
		}
	}

	/*
	 * The method used by the {@link #search() search()} method. Sends the empty
	 * string to the results blocking queue, thus signalling that there will be
	 * no more results
	 * 
	 * @throws java.lang.InterruptedException
	 */
	void stopPrinter() throws InterruptedException {
		resultsBlockingQueue.put(END_FLAG);
	}

	/*
	 * Returns the finished's value
	 * 
	 * @return the value of finished
	 */
	boolean isFinished() {
		return finished;
	}

	/*
	 * The getter of the results blocking queue
	 * 
	 * @ return resultsBlockingQueue
	 */
	BlockingQueue<String> getResultsBlockingQueue() {
		return resultsBlockingQueue;
	}

	/*
	 * Main method, checks the arguments using the {@link
	 * Task1#argsCheck(String[],int) Task1#argsCheck(String[],int)}, creates the
	 * class's instance and runs the two executors: one searching for the
	 * results, another printing them.
	 */
	public static void main(String[] args) {
        Pair<Pair<String, String>, Integer> checkedArgs = argsCheck(args, 2);

		ExecutorService execSearcher = Executors.newSingleThreadExecutor();
		ExecutorService execPrinter = Executors.newSingleThreadExecutor();

		new Task2(checkedArgs, execSearcher, execPrinter);

		execSearcher.shutdown();
		execPrinter.shutdown();
	}
}
