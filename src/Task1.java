/*
 * Copyright Alex Chulkin (c) 2016
 */


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Task1 {

	private final String root;
	private final String mask;
	private final int depth;

	private List<String> resultsList = new ArrayList<>();

	Task1(String root, String mask, int depth) {
		this.root = root;
		this.mask = mask;
		this.depth = depth;
	}

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

	public static void main(String[] args) {
		Pair<Pair<String, String>, Integer> checkedArgs = argsCheck(args, 1);
		Task1 task1 = new Task1(checkedArgs.getFirst().getFirst(),
				checkedArgs.getFirst().getSecond(),
				checkedArgs.getSecond());
		try {
			task1.search();
		} catch (IOException e) {
			throw new RuntimeException(Helper.IO_ERROR, e);
		}
		task1.outputResults(null);
	}

	public void search() throws IOException {
		Path currentPath;
		Path rootPath = Helper.getRootPath(root);
		Pair<Deque<Path>, Integer> currentSiblingPathsAndDepth;
		int currentDepth = 0;
		Stack<Pair<Deque<Path>, Integer>> stack = new Stack<>();
		stack.push(new Pair<>(getPathChildren(Helper.getParentPath(rootPath), rootPath, depth == currentDepth, mask),
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

	Deque<Path> getPathChildren(Path parent, Path child, boolean neededDepth, String mask) throws IOException {
		Deque<Path> pathsList = new LinkedList<>();
		if (parent != null && !parent.toFile().isDirectory())
			return pathsList;
		if (child != null)
			pathsList.add(child);
		if (parent == null) {
			for (File f : File.listRoots())
				if (f.exists()) {
					if (neededDepth && f.getName().contains(mask)) {
						addPathToResults(f.toPath().toRealPath().toString());
					}
					Path p = f.toPath();
					if (child == null || !child.equals(p)) {
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
					if (child == null || !child.equals(entry)) {
						pathsList.add(entry);
					}
				}
		} catch (DirectoryIteratorException ex) {
			throw ex.getCause();
		}
		return pathsList;
	}

	void addPathToResults(String filename) {
		resultsList.add(filename);
	}

	private void outputResults(PrintWriter pw) {
		println(resultsList.isEmpty() ? Helper.NO_RESULTS : Helper.RESULTS, pw);
		for (String s : resultsList) {
			println(s, pw);
		}
	}

	void println(String s, PrintWriter pw) {
		if (pw == null) {
			System.out.println(s);
		} else {
			pw.println(s);
		}
	}
}
