/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

class Server implements Runnable {
    private static Queue<ExecutorService> serverExecs = new LinkedList<>();
    private int port;
    private String root;
    private ServerSocket serverSocket;
    private boolean isStopped = false;
    private ExecutorService serverExec;
    private ExecutorService browserExec;
    private ExecutorService printerExec;
    private ExecutorService dispatcherExec;

    Server(Pair<String, Integer> checkedArgs, ExecutorService serverExec) {
        this.root = checkedArgs.getFirst();
        this.port = checkedArgs.getSecond();
        this.serverExec = serverExec;
        serverExecs.add(serverExec);
    }

    private static void shutdownEverything() {
        serverExecs.forEach(ExecutorService::shutdown);
    }

    Server setBrowserExec(ExecutorService browserExec) {
        this.browserExec = browserExec;
        return this;
    }

    Server setPrinterExec(ExecutorService printerExec) {
        this.printerExec = printerExec;
        return this;
    }

    Server setDispatcherExec(ExecutorService dispatcherExec) {
        this.dispatcherExec = dispatcherExec;
        return this;
    }

    void execute() {
        serverExec.execute(this);
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
            System.out.println();
            System.out.format(Helper.SOCKET_CONNECTED, clientSocketCounter);
            final int id = clientSocketCounter++;
            dispatcherExec.execute(() -> prepareAndRunSearch(id, clientSocket));
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

    void stop() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(Helper.CLOSING_SERVER_ERROR, e);
        } finally {
            shutdownEverything();
            Client.shutdownEverything();
        }
    }

    private void prepareAndRunSearch(int id, Socket clientSocket) {
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());
             BufferedReader br = new BufferedReader(new InputStreamReader(in, Helper.ENCODING));
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, Helper.ENCODING))) {

            Client client = new Client(Helper.telnetClientCheckArgs(root, br, pw));
            client.setPrinterExec(printerExec);
            client.setBrowserExec(browserExec);
            client.setDispatcherExec(dispatcherExec);
            client.search();
            client.outputResults(pw, id);
            while (!client.isFinished()) {
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

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(Helper.IO_ERROR, e);
        }
    }
}
