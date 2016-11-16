import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

/*
 * The telnet server class. Gets the rootpath and the port number and starts accepting the client sockets' instances.
 * The clients input the mask and the depth and get the results on their consoles.
 * The app is multithreaded. Only one thread works with the file system.
 * Also only one runs the server instance(for the easiness and consistency).
 * All the other work is multithreaded. App uses the numerous executor pools and blocking queues.
 */
public class Task3 {
	/*
	 * The string constants used
	 */
	private static final String ENCODING = "ISO-8859-1";
	private static final String CLIENT_ERROR = "Input error!";
	private static final String CLOSING_SERVER_ERROR = "Error closing server";
	private static final String OPEN_PORT_ERROR = "Cannot open port #";
	private static final String CLIENT_CONNECTION_ERROR = "Error accepting client connection";
	private static final String INTERRUPTED_DURING_SLEEP = "Interrupted during sleep";
	private static final String CLIENT_ENCOURAGE = "Please start the telnet clients!";
	private static final String CLIENT_USAGE = "The pattern should be the following: <depth>"
			+ " <mask>. Remember that <depth> should be a non-negative integer";
	private static final String SERVER_USAGE = "java Task3 <rootPath>"
			+ "  <portNumber>,\nrootPath should be a valid path";
	private static final String SERVER_STARTED = "Server started!";
	private static final String SERVER_STOPPING = "Server stopping...";
	private static final String SERVER_STOPPED = "Server stopped!";
	private static final String SOCKET_CONNECTED = "The client socket #%d has connected to the server!\n";

	/*
	 * The constant used for the convenience
	 */
	private static final Pair<Path, FiveTuple<Task2, Path, Path, Boolean, String>> finishingFiveParams = new Pair<Path, FiveTuple<Task2, Path, Path, Boolean, String>>(
			null, null);

	/*
	 * The blocking queues
	 */
	private final BlockingQueue<String> rootQueue;
	private final BlockingQueue<Path> rootPathQueue;
	private final BlockingQueue<Pair<Path, FiveTuple<Task2, Path, Path, Boolean, String>>> searcherQueue;
	private final BlockingQueue<Deque<Path>> pathListsQueue;
	private final BlockingQueue<Path> parentsQueue;
	private final ExecutorService mainExec;
	private final ExecutorService searcherExec;
	private final List<ExecutorService> execs = new ArrayList<ExecutorService>();

	private final Server server;

	/*
	 * The String representation of the root path
	 */
	private final String root;
	/*
	 * The rootPath itself
	 */
	private Path rootPath = null;

	/*
	 * The helper class implementing the Builder design pattern for the Task3's
	 * initialization
	 */
	private static class Builder {
		private ExecutorService mainExec;
		private ExecutorService searcherExec;

		private String root;

		private BlockingQueue<String> rootQueue;
		private BlockingQueue<Path> rootPathQueue;
		private BlockingQueue<Pair<Path, FiveTuple<Task2, Path, Path, Boolean, String>>> searcherQueue;
		private BlockingQueue<Deque<Path>> pathListsQueue;
		private BlockingQueue<Path> parentsQueue;

		private int portNumber;

		Builder mainExec(ExecutorService m) {
			mainExec = m;
			return this;
		}

		Builder searcherExec(ExecutorService exec) {
			searcherExec = exec;
			return this;
		}

		Builder root(String root) {
			this.root = root;
			return this;
		}

		Builder rootQ(BlockingQueue<String> rootQ) {
			rootQueue = rootQ;
			return this;
		}

		Builder rootPathQ(BlockingQueue<Path> rootPathQ) {
			rootPathQueue = rootPathQ;
			return this;
		}

		Builder searcherQ(BlockingQueue<Pair<Path, FiveTuple<Task2, Path, Path, Boolean, String>>> searcherQ) {
			searcherQueue = searcherQ;
			return this;
		}

		Builder pathListsQ(BlockingQueue<Deque<Path>> pathsListQ) {
			pathListsQueue = pathsListQ;
			return this;
		}

		Builder parentsQ(BlockingQueue<Path> parentsQ) {
			parentsQueue = parentsQ;
			return this;
		}

		Builder portNumber(int pn) {
			portNumber = pn;
			return this;
		}

		Task3 build() {
			return new Task3(this);
		}
	}

	private void search() {
		try {
			if (rootPath == null) {
				String root = rootQueue.poll(Task2.MAXIMAL_WAIT,
						TimeUnit.SECONDS);
				if (root == null)
					throw new RuntimeException(
							Task2.APPLICATION_IS_DEADLOCKED);

				rootPathQueue.put(Task2.getRootPath(root));
			}
			while (!Thread.interrupted()) {
				Pair<Path, FiveTuple<Task2, Path, Path, Boolean, String>> pathAndParams = searcherQueue
						.poll(Task2.MAXIMAL_WAIT, TimeUnit.SECONDS);
				if (pathAndParams == null)
					throw new RuntimeException(
							Task2.APPLICATION_IS_DEADLOCKED);
				Path path = pathAndParams.getFirst();
				FiveTuple<Task2, Path, Path, Boolean, String> fiveParams = pathAndParams
						.getSecond();
				if (path == null) {
					if (fiveParams != null)
						pathListsQueue.put(fiveParams.getFirst()
								.getPathChildren(fiveParams.getSecond(),
										fiveParams.getThird(),
										fiveParams.getFourth(),
										fiveParams.getFifth()));
					else
						break;
				} else {
					parentsQueue.put(Task2.getParentPath(path));
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(Task2.INTERRUPTED_IN_BLOCKING_QUEUE,
					e);
		} catch (IOException e) {
			throw new RuntimeException(Task1.IO_ERROR, e);
		}
	}

	private void stopServer() {
		server.stop();
	}

	/*
	 * The Server class
	 */
	private class Server implements Runnable {
		/*
		 * The port number
		 */
		private int port;
		/*
		 * The serverSocket opened
		 */
		private ServerSocket serverSocket;
		/*
		 * The flag showing if the server is stopped
		 */
		private boolean isStopped = false;

		/*
		 * The constructor. Obtains the port number and starts the run method
		 * 
		 * 
		 * @ param port the port number
		 */
		Server(int port, ExecutorService exec) {
			this.port = port;
			exec.execute(this);
		}

		/*
		 * The Runnable's run method. Calls the {@link
		 * com.alex.Task3$Server#openServerSocket() openServerSocket()}, then
		 * accepts the socket and puts it inside the FirstPhase wrapper via its
		 * constructor, see the {@link
		 * Task3$FirstPhase#FirstPhase(ClientSocket,ExecutorService)
		 * FirstPhase(ClientSocket,ExecutorService)}. Method stops it work when
		 * the server is stopped(calls the {@link #Task3$Server#stop() stop()
		 * method}.
		 */
		public void run() {
			openServerSocket();
			int clientSocketCounter = 0;
			while (!isStopped) {
				Socket clientSocket = null;
				try {
					clientSocket = serverSocket.accept();
					System.out.format(SOCKET_CONNECTED,clientSocketCounter);
				} catch (IOException e) {
					if (isStopped) {
						System.out.println(SERVER_STOPPED);
						return;
					}
					throw new RuntimeException(CLIENT_CONNECTION_ERROR, e);
				}
				final int id = clientSocketCounter++;
				final Socket socket = clientSocket;
				mainExec.execute(() -> prepareTask2(id, socket));
			}
		}

		/*
		 * Opens the server socket.
		 */
		private void openServerSocket() {

			try {
				this.serverSocket = new ServerSocket(this.port);
				System.out.println(SERVER_STARTED);
				System.out.println(CLIENT_ENCOURAGE);
				System.out.println(CLIENT_USAGE);
			} catch (IOException e) {
				throw new RuntimeException(OPEN_PORT_ERROR + port, e);
			}
		}

		/*
		 * Stops the server, setting isStopped to true, closing the
		 * serverSocket, putting the finishing signal to the searcher queue and
		 * shutting down all the executors
		 */
		private void stop() {
			this.isStopped = true;
			try {
				this.serverSocket.close();
				Task3.this.searcherQueue.put(finishingFiveParams);
				for (ExecutorService e : Task3.this.execs)
					e.shutdown();
			} catch (IOException e) {
				throw new RuntimeException(CLOSING_SERVER_ERROR, e);
			} catch (InterruptedException e) {
				throw new RuntimeException(Task2.INTERRUPTED_IN_BLOCKING_QUEUE, e);
			}
		}
	}

	/*
	 * Task3 Constructor. Initializes all the executors and queues
	 * 
	 * @ param builder the input parameter containing the info about the
	 * executors and queues(see the Builder Design Pattern)
	 */
	Task3(Builder builder) {
		this.mainExec = builder.mainExec;
		execs.add(this.mainExec);
		this.searcherExec = builder.searcherExec;
		execs.add(this.searcherExec);
		this.root = builder.root;
		this.rootQueue = builder.rootQueue;
		this.rootPathQueue = builder.rootPathQueue;
		this.searcherQueue = builder.searcherQueue;
		this.pathListsQueue = builder.pathListsQueue;
		this.parentsQueue = builder.parentsQueue;
		searcherExec.execute(() -> search());
		server = new Server(builder.portNumber, mainExec);
	}

	private void prepareTask2 (int id, Socket clientSocket) {
		try (InputStream in = clientSocket.getInputStream();
			 OutputStream out = new BufferedOutputStream(
					 clientSocket.getOutputStream());
			 BufferedReader br = new BufferedReader(
					 new InputStreamReader(in, ENCODING));
			 PrintWriter pw = new PrintWriter(new OutputStreamWriter(
					 out, ENCODING))) {

			Pair<Pair<String,String>, Integer> checkedArgs = parseLine(br, pw);
			Task2 task2 = new Task2(checkedArgs, mainExec, id, pw);
			while (true) {
				try {
					Thread.sleep(100);
					if (task2.isFinished())
						break;
				} catch (InterruptedException e) {
					closeSocket(clientSocket);
					throw new RuntimeException(INTERRUPTED_DURING_SLEEP, e);
				}
			}
		} catch (IOException e) {
			closeSocket(clientSocket);
			throw new RuntimeException(Task1.IO_ERROR, e);
		}
	}

	/*
		 * Writes the client usage using the printwriter and the error flag.
		 * This method is called from the {@link
		 * com.alex.Task3$FirstPhase#run(run) run()}
		 *
		 * @param pw the printwriter responsible for printing
		 *
		 * @param error if we have to print the error message
		 */
	private void clientUsage(PrintWriter pw, boolean error) {
		if (error)
			pw.println(CLIENT_ERROR);
		pw.println(CLIENT_USAGE);
	}

	/*
     * Parses the input line and returns the pair consisting of the mask and
     * depth. This method is called from the {@link
     * com.alex.Task3$FirstPhase#run() run()}
     *
     * @param br the bufferedreader responsible for the line to input
     *
     * @param pw the printwriter for outputting the client Usage
     *
     * @return the Pair consisting of the mask and depth
     *
     * @throws java.io.IOException
     */
	private Pair<Pair<String, String>, Integer> parseLine(BufferedReader br,
														  PrintWriter pw) throws IOException {
		int depth = -1;
		String mask = null;
		while (true) {
			String line = br.readLine().trim();

			int ixSpace = line.indexOf(' ');
			if (ixSpace == -1) {
				clientUsage(pw, true);
				continue;
			}
			depth = -1;
			mask = line.substring(ixSpace + 1).trim();
			try {
				depth = Integer.parseInt(line.substring(0, ixSpace));
			} catch (NumberFormatException e) {
				clientUsage(pw, true);
				continue;
			}
			if (depth < 0) {
				clientUsage(pw, true);
				continue;
			}
			break;
		}
		Pair<String, String> pair = new Pair<>(root, mask);
		return new Pair<>(pair, depth);
	}

	/*
	 * Method closing the given socket
	 * 
	 * @param socket socket to close
	 */
	private void closeSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(Task1.IO_ERROR, e);
		}

	}

	/*
	 * Methods checking the server args and returning the root string and port
	 * number
	 * 
	 * @param args from the {@link #main(String[]) main method}
	 * 
	 * @return the Pair consisting of the root string and the port number
	 */

	private static Pair<String, Integer> serverArgsCheck(String[] args) {
		if ((args.length != 2) && (args.length != 3))// for the compatibility
														// with build xml
			serverUsage();

		int port = 0;
		try {
			port = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			serverUsage();
		}
		if (port <= 0)
			serverUsage();

		return new Pair<String, Integer>(args[0], port);
	}

	/*
	 * Prints the server usage if there was an error
	 */
	private static void serverUsage() {
		System.err.println(SERVER_USAGE);
		System.exit(-1);
	}

	/*
	 * The main method. 1. Checks the args and initializes the root 2.
	 * Initializes the new Task3 instance with all the executors and blocking
	 * queues: -mainExec is multithreaded and runs all except the file system
	 * operations(Searcher inner class, searcherExec executor). -rootqueue and
	 * rootpathqueue take the root string and return the rootpath string
	 * respectively(searcher provides the transformation) -searcherqueue takes
	 * the parameters for parents search as well as for the children search
	 * -pathListsqueue returns the children lists -parents Queue returns the
	 * parents. and then we initialize the server process with the port number(it
	 * is started internally) and executorservice. Server then starts the
	 * FirstPhases inside the server's context space. The FirstPhase instances
	 * are started from their constructors and finally create the SecondPhase
	 * instances. Every SecondPhase instance starts the execution of its nested
	 * Task2 instance and the Task2's Printer instance. Task2 instance calls the
	 * Searcher via the searcher queue and root queue and gets responses via the
	 * parents queue, pashLists queue and rootPath queue. Then it initializes and
	 * start the Searcher instance. Searcher is responsible for both the
	 * parents' and children's transformations from searcherqueues to the last
	 * parents and pathLists queues. All the executors are started inside the
	 * corresponding methods of the correspong classes. No executors are started
	 * directly here in main method. 3. Then we sleep a while and 4.stop the
	 * server.
	 */
	public static void main(String[] args) {
		Pair<String, Integer> checkedArgs = serverArgsCheck(args);// 1
		String root = checkedArgs.getFirst();
		int portNumber = checkedArgs.getSecond();
		Task3 task3 = new Builder()								  // 2
				.mainExec(Executors.newCachedThreadPool())
				.searcherExec(Executors.newSingleThreadExecutor())
				.root(root)
				.rootQ(new SynchronousQueue<>())
				.rootPathQ(new SynchronousQueue<>())
				.searcherQ(new LinkedBlockingQueue<>())
				.pathListsQ(new LinkedBlockingQueue<>())
				.parentsQ(new LinkedBlockingQueue<>())
				.portNumber(portNumber)
				.build();
		try { // 3
			Thread.sleep(2 * 1000 * Task2.MAXIMAL_WAIT);
		} catch (InterruptedException e) {
			throw new RuntimeException(INTERRUPTED_DURING_SLEEP, e);
		}
		System.out.println(SERVER_STOPPING);
		task3.stopServer(); // 4
	}
}
