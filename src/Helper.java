/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.io.File;

class Helper {
    static final String NO_RESULTS = "NO_RESULTS";
    static final String RESULTS = "RESULTS";
    static final String IO_ERROR = "I/O Error occured";
    static final String APPLICATION_IS_DEADLOCKED = "New sockets are not connecting for a long time "
            + "or the application is deadlocked";
    static final Long MAXIMAL_WAIT = 600000L;
    static final String INTERRUPTED_IN_BLOCKING_QUEUE = "Interrupted in blocking queue";
    static final String INTERRUPTED_IN_EXECUTOR = "Interrupted in executor";
    static final String EXECUTION_EXCEPTION = "Exeption occurred during execution";
    static final String END_FLAG = "";
    static final String EXCEPTION = "Exception ";
    static final String THROWN_IN_THREAD = " thrown in thread ";
    static final String CLIENT_SOCKET_OUTPUT = "Client socket #%d prints: %s\n";
    static final String ENCODING = "ISO-8859-1";
    static final String CLIENT_ERROR = "Input error!";
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
    private static final String USAGE_BEG = "Input error, the pattern is the following: java Task";
    private static final String USAGE_END = " <rootPath>"
            + "  <mask>\n <depth>, rootPath should be a valid path,"
            + " depth should be not-negative integer";
    private static final String SERVER_USAGE = "java Task3 <rootPath> <portNumber>,\nrootPath should be a valid path";

    static Pair<Pair<String, String>, Integer> argsCheck(String[] args, int taskNum) {
        if ((args.length != 3) || (!new File(args[0]).exists())) {
            usage(taskNum);
        }
        int depth = -1;
        try {
            depth = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            usage(taskNum);
        }
        if (depth < 0)
            usage(taskNum);
        Pair<String, String> pair = new Pair<>(args[0], args[1]);
        return new Pair<>(pair, depth);
    }

    private static void usage(int taskNum) {
        System.err.println(Helper.USAGE_BEG + taskNum + " " + Helper.USAGE_END);
        System.exit(-1);
    }

    static Pair<String, Integer> serverArgsCheck(String[] args) {
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


}
