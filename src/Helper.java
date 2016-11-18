/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class Helper {
    static final String NO_RESULTS = "NO_RESULTS";
    static final String RESULTS = "RESULTS";
    static final String IO_ERROR = "I/O Error occured";
    static final String USAGE_BEG = "Input error, the pattern is the following: java Task";
    static final String USAGE_END = " <rootPath>"
            + "  <mask>\n <depth>, rootPath should be a valid path,"
            + " depth should be not-negative integer";
    static final String APPLICATION_IS_DEADLOCKED = "New sockets are not connecting for a long time "
            + "or the application is deadlocked";
    static final Long MAXIMAL_WAIT = 150L;
    static final String INTERRUPTED_IN_BLOCKING_QUEUE = "Interrupted in blocking queue";
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
    static final String SERVER_USAGE = "java Task3 <rootPath> <portNumber>,\nrootPath should be a valid path";
    static final String SERVER_STARTED = "Server started!";
    static final String SERVER_STOPPING = "Server stopping...";
    static final String SERVER_STOPPED = "Server stopped!";
    static final String SOCKET_CONNECTED = "The client socket #%d has connected to the server!\n";

    static Path getParentPath(Path path) throws IOException {
        return path.toRealPath().getParent();
    }

    static Path getRootPath(String root) throws IOException {
        return Paths.get(root).toRealPath();
    }
}
