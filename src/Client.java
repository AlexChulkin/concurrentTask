
/*
 * Copyright Alex Chulkin (c) 2016
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

class Client {
    private static Queue<ExecutorService> execs = new LinkedList<>();
    private static Queue<String> resultsQueue = new LinkedList<>();
    private final String root;
    private final String mask;
    private final int depth;
    private ExecutorService browserExec;
    private ExecutorService printerExec;
    private ExecutorService dispatcherExec;
    private Path rootPath;
    private boolean finished = false;

    Client(Pair<Pair<String, String>, Integer> checkedArgs) {
        this.root = checkedArgs.getFirst().getFirst();
        this.mask = checkedArgs.getFirst().getSecond();
        this.depth = checkedArgs.getSecond();
    }

    static void shutdownEverything() {
        execs.forEach(ExecutorService::shutdown);
    }

    Client setBrowserExec(ExecutorService browserExec) {
        this.browserExec = browserExec;
        execs.add(browserExec);
        return this;
    }

    Client setPrinterExec(ExecutorService printerExec) {
        this.printerExec = printerExec;
        execs.add(printerExec);
        resultsQueue = new LinkedBlockingQueue<>();
        return this;
    }

    Client setDispatcherExec(ExecutorService dispatcherExec) {
        this.dispatcherExec = dispatcherExec;
        execs.add(dispatcherExec);
        return this;
    }

    void search() {
        if (Optional.ofNullable(dispatcherExec).isPresent()) {
            dispatcherExec.execute(() -> {
                searchInsideExec();
                stopPrinter();
            });
        } else if (Optional.ofNullable(browserExec).isPresent()) {
            browserExec.execute(() -> {
                searchInsideExec();
                stopPrinter();
            });
        } else {
            searchInsideExec();
        }
    }

    private void searchInsideExec() {
        try {
            Path currentPath;
            Path rootPath = getRootPath(root);
            Pair<Deque<Path>, Integer> currentSiblingPathsAndDepth;
            int currentDepth = 0;
            Stack<Pair<Deque<Path>, Integer>> stack = new Stack<>();
            stack.push(new Pair<>(getPathChildren(getParentPath(rootPath), rootPath, depth == currentDepth, mask),
                    currentDepth));
            while (!stack.isEmpty()) {
                currentSiblingPathsAndDepth = stack.pop();
                Deque<Path> currentSiblingPaths = currentSiblingPathsAndDepth.getFirst();
                currentDepth = currentSiblingPathsAndDepth.getSecond();
                while (!currentSiblingPaths.isEmpty()) {
                    currentPath = currentSiblingPaths.removeFirst();
                    if (!currentSiblingPaths.isEmpty())
                        stack.push(new Pair<>(currentSiblingPaths, currentDepth));
                    if (currentDepth < depth) {
                        currentSiblingPaths = getPathChildren(currentPath, null, depth == ++currentDepth, mask);
                    } else {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(Helper.IO_ERROR, e);
        } catch (InterruptedException e) {
            throw new RuntimeException(Helper.INTERRUPTED_IN_EXECUTOR, e);
        } catch (ExecutionException e) {
            throw new RuntimeException(Helper.EXECUTION_EXCEPTION, e);
        }
    }

    private Deque<Path> getPathChildren(Path parent, Path child, boolean neededDepth, String mask)
            throws IOException, InterruptedException, ExecutionException {
        if (Optional.ofNullable(dispatcherExec).isPresent() && Optional.ofNullable(browserExec).isPresent()) {
            return browserExec.submit(() -> getPathChildrenInsideExec(parent, child, neededDepth, mask)).get();
        }
        return getPathChildrenInsideExec(parent, child, neededDepth, mask);
    }

    private Deque<Path> getPathChildrenInsideExec(Path parent, Path child, boolean neededDepth, String mask) throws IOException {
        Deque<Path> pathsList = new LinkedList<>();
        if (Optional.ofNullable(parent).isPresent() && !parent.toFile().isDirectory())
            return pathsList;
        if (Optional.ofNullable(child).isPresent())
            pathsList.add(child);
        if (!Optional.ofNullable(parent).isPresent()) {
            for (File f : File.listRoots())
                if (f.exists()) {
                    if (neededDepth && f.getName().contains(mask)) {
                        addPathToResults(f.toPath().toRealPath().toString());
                    }
                    Path p = f.toPath();
                    if (!Optional.ofNullable(child).isPresent() || !child.equals(p)) {
                        pathsList.add(p);
                    }
                }
            return pathsList;
        }

        if (Files.isReadable(parent)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
                for (Path entry : stream) {
                    if (entry.toFile().exists()) {
                        if (neededDepth && entry.getFileName().toString().contains(mask)) {
                            addPathToResults(entry.toRealPath().toString());
                        }
                        if (!Optional.ofNullable(child).isPresent() || !child.equals(entry)) {
                            pathsList.add(entry);
                        }
                    }
                }
            } catch (DirectoryIteratorException ex) {
                throw ex.getCause();
            }
        }
        return pathsList;
    }

    private void addPathToResults(String filename) {
        if (Optional.ofNullable(dispatcherExec).isPresent() && Optional.ofNullable(browserExec).isPresent()) {
            dispatcherExec.execute(() -> addPathToResultsInsideExec(filename));
        } else {
            addPathToResultsInsideExec(filename);
        }
    }

    private void addPathToResultsInsideExec(String filename) {
        resultsQueue.add(filename);
    }

    boolean isFinished() {
        return finished;
    }

    void outputResults(PrintWriter pw, Integer id) {
        if (Optional.ofNullable(printerExec).isPresent()) {
            if (Optional.ofNullable(dispatcherExec).isPresent()) {
                printerExec.execute(() -> outputResultsInsideExec(pw, id));
            } else {
                printerExec.execute(() -> outputResultsInsideExec(null, null));
            }
        } else {
            clientPrintln(resultsQueue.isEmpty() ? Helper.NO_RESULTS : Helper.RESULTS, pw);
            for (String s : resultsQueue) {
                clientPrintln(s, pw);
            }
        }
    }

    private void outputResultsInsideExec(PrintWriter pw, Integer id) {
        boolean empty = true;
        try {
            while (!Thread.interrupted()) {
                String res = ((BlockingQueue<String>) resultsQueue).poll(Helper.MAXIMAL_WAIT, TimeUnit.SECONDS);
                if (!Optional.ofNullable(res).isPresent()) {
                    throw new RuntimeException(Helper.APPLICATION_IS_DEADLOCKED);
                }
                if (!res.equals(Helper.END_FLAG)) {
                    if (empty) {
                        clientPrintln(Helper.RESULTS, pw);
                        serverPrintln(id, Helper.RESULTS);
                        empty = false;
                    }
                    clientPrintln(res, pw);
                    serverPrintln(id, res);
                } else {
                    clientPrintln(empty ? Helper.NO_RESULTS : Helper.END_OF_OUTPUT, pw);
                    serverPrintln(id, empty ? Helper.NO_RESULTS : Helper.END_OF_OUTPUT);
                    break;
                }
            }
            finished = true;
        } catch (InterruptedException e) {
            throw new RuntimeException(Helper.INTERRUPTED_IN_BLOCKING_QUEUE, e);
        }
    }

    private void serverPrintln(Integer id, String s) {
        if (!Optional.ofNullable(id).isPresent()) {
            return;
        }
        System.out.format(Helper.CLIENT_SOCKET_OUTPUT, id, s);
    }

    private void stopPrinter() {
        resultsQueue.add(Helper.END_FLAG);
    }

    private void clientPrintln(String s, PrintWriter pw) {
        if (!Optional.ofNullable(pw).isPresent()) {
            System.out.println(s);
        } else {
            pw.println(s);
        }
    }

    private Path getParentPath(Path path) throws IOException, InterruptedException, ExecutionException {
        if (Optional.ofNullable(dispatcherExec).isPresent() && Optional.ofNullable(browserExec).isPresent()) {
            return browserExec.submit(() -> getParentPathInsideExec(path)).get();
        }
        return getParentPathInsideExec(path);
    }

    private Path getParentPathInsideExec(Path path) throws IOException {
        return path.toRealPath().getParent();
    }

    private Path getRootPath(String root) throws IOException, InterruptedException, ExecutionException {
        if (Optional.ofNullable(rootPath).isPresent()) {
            return rootPath;
        }
        if (Optional.ofNullable(dispatcherExec).isPresent() && Optional.ofNullable(browserExec).isPresent()) {
            return browserExec.submit(() -> getRootPathInsideExec(root)).get();
        }
        return getRootPathInsideExec(root);
    }

    private Path getRootPathInsideExec(String root) throws IOException {
        rootPath = Paths.get(root).toRealPath();
        return rootPath;
    }
}
