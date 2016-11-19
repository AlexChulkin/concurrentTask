
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

    Client setBrowserExec(ExecutorService browserExec) {
        this.browserExec = browserExec;
        return this;
    }

    Client setPrinterExec(ExecutorService printerExec) {
        this.printerExec = printerExec;
        resultsQueue = new LinkedBlockingQueue<>();
        return this;
    }

    Client setDispatcherExec(ExecutorService dispatcherExec) {
        this.dispatcherExec = dispatcherExec;
        return this;
    }

    void search() {
        if (Optional.ofNullable(dispatcherExec).isPresent()) {
            dispatcherExec.execute(this::search_);
        } else if (Optional.ofNullable(browserExec).isPresent()) {
            browserExec.execute(() -> {
                search_();
                stopPrinter();
            });
        } else {
            search_();
        }
    }

    private void search_() {
        try {
            search__();
        } catch (IOException e) {
            throw new RuntimeException(Helper.IO_ERROR, e);
        } catch (InterruptedException e) {
            throw new RuntimeException(Helper.INTERRUPTED_IN_EXECUTOR, e);
        } catch (ExecutionException e) {
            throw new RuntimeException(Helper.EXECUTION_EXCEPTION, e);
        }
    }

    private void search__() throws IOException, InterruptedException, ExecutionException {
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
    }

    private Deque<Path> getPathChildren(Path parent, Path child, boolean neededDepth, String mask)
            throws IOException, InterruptedException, ExecutionException {
        if (Optional.ofNullable(browserExec).isPresent()) {
            return browserExec.submit(() -> getPathChildren_(parent, child, neededDepth, mask)).get();
        }
        return getPathChildren_(parent, child, neededDepth, mask);
    }

    private Deque<Path> getPathChildren_(Path parent, Path child, boolean neededDepth, String mask) throws IOException {
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

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
            for (Path entry : stream)
                if (entry.toFile().exists()) {
                    if (neededDepth && entry.getFileName().toString().contains(mask)) {
                        addPathToResults(entry.toRealPath().toString());
                    }
                    if (!Optional.ofNullable(child).isPresent() || !child.equals(entry)) {
                        pathsList.add(entry);
                    }
                }
        } catch (DirectoryIteratorException ex) {
            throw ex.getCause();
        }
        return pathsList;
    }

    private void addPathToResults(String filename) {
        if (Optional.ofNullable(dispatcherExec).isPresent()) {
            dispatcherExec.execute(() -> addPathToResults_(filename));
        } else if (Optional.ofNullable(browserExec).isPresent()) {
            browserExec.execute(() -> addPathToResults_(filename));
        } else {
            addPathToResults_(filename);
        }
    }

    private void addPathToResults_(String filename) {
        resultsQueue.add(filename);
    }

    boolean isFinished() {
        return finished;
    }

    void outputResults(PrintWriter pw, Integer id) {
        if (Optional.ofNullable(printerExec).isPresent()) {
            if (Optional.ofNullable(dispatcherExec).isPresent()) {
                printerExec.execute(() -> outputResults_(pw, id));
            } else {
                printerExec.execute(() -> outputResults_(null, null));
            }
        } else {
            outputResults_(pw);
        }
    }

    private void outputResults_(PrintWriter pw) {
        println(resultsQueue.isEmpty() ? Helper.NO_RESULTS : Helper.RESULTS, pw);
        for (String s : resultsQueue) {
            println(s, pw);
        }
    }

    private void outputResults_(PrintWriter pw, Integer id) {
        boolean empty = true;
        try {
            while (!Thread.interrupted()) {
                String res = ((BlockingQueue<String>) resultsQueue).poll(Helper.MAXIMAL_WAIT, TimeUnit.SECONDS);
                if (!Optional.ofNullable(res).isPresent()) {
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

                    if (empty) {
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

    private void serverOutput(Integer id, String s) {
        if (!Optional.ofNullable(id).isPresent()) {
            return;
        }
        System.out.format(Helper.CLIENT_SOCKET_OUTPUT, id, s);
    }

    private void stopPrinter() {
        resultsQueue.add(Helper.END_FLAG);
    }

    private void println(String s, PrintWriter pw) {
        if (!Optional.ofNullable(pw).isPresent()) {
            System.out.println(s);
        } else {
            pw.println(s);
        }
    }

    private Path getParentPath(Path path) throws IOException, InterruptedException, ExecutionException {
        if (Optional.ofNullable(browserExec).isPresent()) {
            return browserExec.submit(() -> getParentPath_(path)).get();
        }
        return getParentPath_(path);
    }

    private Path getParentPath_(Path path) throws IOException {
        return path.toRealPath().getParent();
    }

    private Path getRootPath(String root) throws IOException, InterruptedException, ExecutionException {
        if (Optional.ofNullable(rootPath).isPresent()) {
            return rootPath;
        }
        if (Optional.ofNullable(browserExec).isPresent()) {
            return browserExec.submit(() -> getRootPath_(root)).get();
        }
        return getRootPath_(root);
    }

    private Path getRootPath_(String root) throws IOException {
        rootPath = Paths.get(root).toRealPath();
        return rootPath;
    }
}
