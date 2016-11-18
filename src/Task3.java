
/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;

public class Task3 {
	private static final Pair<Path, Task1Wrapper> finishingFiveParams = new Pair<>(null, null);
	private final BlockingQueue<String> rootQueue;
	private final BlockingQueue<Path> rootPathQueue;
	private final BlockingQueue<Pair<Path, Task1Wrapper>> searcherQueue;
	private final BlockingQueue<Deque<Path>> pathListsQueue;
	private final BlockingQueue<Path> parentsQueue;
	private final ExecutorService mainExec;
	private final ExecutorService searcherExec;
	private final List<ExecutorService> execs = new ArrayList<>();
	private final Server server;
	private final String root;

	private Task3(Builder builder) {
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
		searcherExec.execute(this::search);
		server = new Server(builder.portNumber, mainExec);

		try {
			Thread.sleep(2 * 1000 * Helper.MAXIMAL_WAIT);
		} catch (InterruptedException e) {
			throw new RuntimeException(Helper.INTERRUPTED_DURING_SLEEP, e);
		}
		System.out.println(Helper.SERVER_STOPPING);
		server.stop();
	}

	private static Pair<String, Integer> serverArgsCheck(String[] args) {
		if ((args.length != 2) && (args.length != 3))// for the compatibility with build xml
			serverUsage();
		int port = 0;
		try {
			port = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			serverUsage();
		}
		if (port <= 0) {
			serverUsage();
		}
		return new Pair<>(args[0], port);
	}

	private static void serverUsage() {
		System.err.println(Helper.SERVER_USAGE);
		System.exit(-1);
	}

	public static void main(String[] args) {
		Pair<String, Integer> checkedArgs = serverArgsCheck(args);
		String root = checkedArgs.getFirst();
		int portNumber = checkedArgs.getSecond();
		new Builder(Executors.newCachedThreadPool(), Executors.newSingleThreadExecutor(), root, portNumber)
				.rootQ(new SynchronousQueue<>())
				.rootPathQ(new SynchronousQueue<>())
				.searcherQ(new LinkedBlockingQueue<>())
				.pathListsQ(new LinkedBlockingQueue<>())
				.parentsQ(new LinkedBlockingQueue<>())
				.build();
	}

	private void search() {
		try {
			String root = rootQueue.poll(Helper.MAXIMAL_WAIT, TimeUnit.SECONDS);
			System.out.println("Root: " + root);
			if (root == null) {
				throw new RuntimeException(Helper.APPLICATION_IS_DEADLOCKED);
			}
			rootPathQueue.put(Helper.getRootPath(root));
			while (!Thread.interrupted()) {
				Pair<Path, Task1Wrapper> pathAndParams = searcherQueue.poll(Helper.MAXIMAL_WAIT, TimeUnit.SECONDS);
				if (pathAndParams == null) {
					throw new RuntimeException(Helper.APPLICATION_IS_DEADLOCKED);
				}
				Path path = pathAndParams.getFirst();
				Task1Wrapper task1Wrapper = pathAndParams.getSecond();
				if (path == null) {
					if (task1Wrapper != null) {
						System.out.println("pathlistq");
						pathListsQueue.put(task1Wrapper.task1.getPathChildren(task1Wrapper.childPath,
								task1Wrapper.parentPath,
								task1Wrapper.neededDepth,
								task1Wrapper.mask));
					} else {
						break;
					}
				} else {
					parentsQueue.put(Helper.getParentPath(path));
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(Helper.INTERRUPTED_IN_BLOCKING_QUEUE, e);
		} catch (IOException e) {
			throw new RuntimeException(Helper.IO_ERROR, e);
		}
	}

	private void closeSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(Helper.IO_ERROR, e);
		}
	}

	private static class Task1Wrapper {
		private Task1 task1;
		private Path parentPath;
		private Path childPath;
		private boolean neededDepth;
		private String mask;
	}

	private static class Builder {
		private ExecutorService mainExec;
		private ExecutorService searcherExec;
		private String root;

		private BlockingQueue<String> rootQueue;
		private BlockingQueue<Path> rootPathQueue;
		private BlockingQueue<Pair<Path, Task1Wrapper>> searcherQueue;
		private BlockingQueue<Deque<Path>> pathListsQueue;
		private BlockingQueue<Path> parentsQueue;

		private int portNumber;

		Builder(ExecutorService mainExec, ExecutorService searcherExec, String root, int portNumber) {
			this.mainExec = mainExec;
			this.searcherExec = searcherExec;
			this.root = root;
			this.portNumber = portNumber;
		}

		Builder rootQ(BlockingQueue<String> rootQ) {
			rootQueue = rootQ;
			return this;
		}

		Builder rootPathQ(BlockingQueue<Path> rootPathQ) {
			rootPathQueue = rootPathQ;
			return this;
		}

		Builder searcherQ(BlockingQueue<Pair<Path, Task1Wrapper>> searcherQ) {
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

		Task3 build() {
			return new Task3(this);
		}
	}

	private class Server implements Runnable {
		private int port;
		private ServerSocket serverSocket;
		private boolean isStopped = false;
		private ExecutorService exec;

		Server(int port, ExecutorService exec) {
			this.port = port;
			this.exec = exec;
			this.exec.execute(this);
		}

		public void run() {
			openServerSocket();
			int clientSocketCounter = 0;
			while (!isStopped) {
				Socket clientSocket;
				try {
					clientSocket = serverSocket.accept();
				} catch (IOException e) {
					if (isStopped) {
						System.out.println(Helper.SERVER_STOPPED);
						return;
					}
					throw new RuntimeException(Helper.CLIENT_CONNECTION_ERROR, e);
				}
				System.out.format(Helper.SOCKET_CONNECTED, clientSocketCounter);
				prepareAndRunSearch(clientSocketCounter++, clientSocket);
			}
		}

		private void openServerSocket() {
			try {
				this.serverSocket = new ServerSocket(this.port);
				System.out.println(Helper.SERVER_STARTED);
				System.out.println(Helper.CLIENT_ENCOURAGE);
				System.out.println(Helper.CLIENT_USAGE);
			} catch (IOException e) {
				throw new RuntimeException(Helper.OPEN_PORT_ERROR + port, e);
			}
		}

		private void stop() {
			this.isStopped = true;
			try {
				this.serverSocket.close();
				searcherQueue.put(finishingFiveParams);
				execs.stream().forEach(ExecutorService::shutdown);
			} catch (IOException e) {
				throw new RuntimeException(Helper.CLOSING_SERVER_ERROR, e);
			} catch (InterruptedException e) {
				throw new RuntimeException(Helper.INTERRUPTED_IN_BLOCKING_QUEUE, e);
			}
		}

		private void prepareAndRunSearch(int id, Socket clientSocket) {
			try (InputStream in = clientSocket.getInputStream();
				 OutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());
				 BufferedReader br = new BufferedReader(new InputStreamReader(in, Helper.ENCODING));
				 PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, Helper.ENCODING))) {
				Pair<Pair<String, String>, Integer> checkedArgs = parseLine(br, pw);
				Task2 task2 = new Task2(checkedArgs, mainExec, id, pw);
				while (!task2.isFinished()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						closeSocket(clientSocket);
						throw new RuntimeException(Helper.INTERRUPTED_DURING_SLEEP, e);
					}
				}
			} catch (IOException e) {
				closeSocket(clientSocket);
				throw new RuntimeException(Helper.IO_ERROR, e);
			}
		}

		private Pair<Pair<String, String>, Integer> parseLine(BufferedReader br, PrintWriter pw) throws IOException {
			int depth;
			String mask;
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
			return new Pair<>(new Pair<>(root, mask), depth);
		}

		private void clientUsage(PrintWriter pw, boolean error) {
			if (error) {
				pw.println(Helper.CLIENT_ERROR);
			}
			pw.println(Helper.CLIENT_USAGE);
		}
	}
}
