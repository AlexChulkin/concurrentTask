
/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class Helper {
    static final String NO_RESULTS = "NO_RESULTS";
    static final String END_OF_OUTPUT = "END OF OUTPUT";
    static final String RESULTS = "RESULTS";
    static final String IO_ERROR = "I/O Error occured";
    static final String APPLICATION_IS_DEADLOCKED = "New sockets are not connecting for a long time "
            + "or the application is deadlocked";
    static final long MAXIMAL_WAIT = 600000L;
    static final String INTERRUPTED_IN_BLOCKING_QUEUE = "Interrupted in blocking queue";
    static final String INTERRUPTED_IN_EXECUTOR = "Interrupted in executor";
    static final String EXECUTION_EXCEPTION = "Exeption occurred during execution";
    static final String END_FLAG = "";
    static final String EXCEPTION = "Exception ";
    static final String THROWN_IN_THREAD = " thrown in thread ";
    static final String CLIENT_SOCKET_OUTPUT = "Client socket #%d prints: %s\n";
    static final String ENCODING = "ISO-8859-1";
    static final String CLOSING_SERVER_ERROR = "Error closing server";
    static final String OPEN_PORT_ERROR = "Cannot open port #";
    static final String CLIENT_CONNECTION_ERROR = "Error accepting client connection";
    static final String INTERRUPTED_DURING_SLEEP = "Interrupted during sleep";
    static final String CLIENT_ENCOURAGE = "Please start the telnet clients!";
    static final String CLIENT_USAGE
            = "The pattern should be the following: <depth> <mask>. " +
            "Remember that <depth> should be a non-negative integer";
    static final String SERVER_STARTED = "Server started!";
    static final String SERVER_STOPPING = "Server stopping...";
    static final String SERVER_STOPPED = "Server stopped!";
    static final String SOCKET_CONNECTED = "The client socket #%d has connected to the server!\n";
    private static final String CLIENT_ERROR = "Input error!";
    private static final String CLIENT_USAGE_BEG = "Input error, the pattern is the following: java Task";
    private static final String CLIENT_USAGE_END = " <rootPath>"
            + "  <mask>\n <depth>, rootPath should be a valid path,"
            + " depth should be not-negative integer";
    private static final String SERVER_USAGE = "<rootPath> <portNumber>,\n<rootPath> should be a valid path and " +
            "<portNumber> should be positive integer ";

    static Pair<Pair<String, String>, Integer> clientArgsCheck(String[] args, int taskNum) {
        if ((args.length != 3) || (!new File(args[0]).exists())) {
            clientUsage(taskNum);
        }
        int depth = -1;
        try {
            depth = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            clientUsage(taskNum);
        }
        if (depth < 0) {
            clientUsage(taskNum);
        }
        Pair<String, String> pair = new Pair<>(args[0], args[1]);
        return new Pair<>(pair, depth);
    }

    private static void clientUsage(int taskNum) {
        System.err.println(Helper.CLIENT_USAGE_BEG + taskNum + " " + Helper.CLIENT_USAGE_END);
        System.exit(-1);
    }

    static Pair<String, Integer> serverArgsCheck(String[] args) {
        if ((args.length != 2) && (args.length != 3)) {
            serverUsage();
        }
        int port = 0;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            serverUsage();
        }
        String root = args[0];
        Path rootPath = null;
        try {
            rootPath = Paths.get(root).toRealPath();
        } catch (IOException e) {
            serverUsage();
        }
        if (port <= 0 || !Files.exists(rootPath)) {
            serverUsage();
        }
        return new Pair<>(root, port);
    }

    private static void serverUsage() {
        System.err.println(Helper.SERVER_USAGE);
        System.exit(-1);
    }

    static Pair<Pair<String, String>, Integer> telnetClientCheckArgs(String root, BufferedReader br, PrintWriter pw)
            throws IOException {
        int depth;
        String mask;
        while (true) {
            String line = br.readLine().trim();
            int ixSpace = line.indexOf(' ');
            if (ixSpace == -1) {
                telnetClientUsage(pw, true);
                continue;
            }
            mask = line.substring(ixSpace + 1).trim();
            try {
                depth = Integer.parseInt(line.substring(0, ixSpace));
            } catch (NumberFormatException e) {
                telnetClientUsage(pw, true);
                continue;
            }
            Path rootPath = null;
            try {
                rootPath = Paths.get(root).toRealPath();
            } catch (IOException e) {
                serverUsage();
            }
            if (depth < 0 || !Files.exists(rootPath)) {
                telnetClientUsage(pw, true);
                continue;
            }
            break;
        }
        return new Pair<>(new Pair<>(root, mask), depth);
    }

    private static void telnetClientUsage(PrintWriter pw, boolean error) {
        if (error) {
            pw.println(Helper.CLIENT_ERROR);
        }
        pw.println(Helper.CLIENT_USAGE);
    }

    static void tryToSleep(long millisecs) {
        try {
            Thread.sleep(millisecs);
        } catch (InterruptedException e) {
            throw new RuntimeException(Helper.INTERRUPTED_DURING_SLEEP, e);
        }
    }
}
