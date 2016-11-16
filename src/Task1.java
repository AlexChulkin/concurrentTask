/**
 * First task.
 * Finds the filetree elements without recursion
 * @author Alex Chulkin
 * 
 */
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Task1 {
	/*
	 * the group of string constants
	 */
	static final String NO_RESULTS = "NO_RESULTS";
	static final String RESULTS = "RESULTS";
	static final String IO_ERROR = "I/O Error occured";
	private static final String USAGE_BEG = "Input error, the pattern is the following: java Task";
	private static final String USAGE_END = " <rootPath>"
			+ "  <mask>\n <depth>, rootPath should be a valid path,"
			+ "depth should be not-negative integer";
	/*
	 * the results list
	 */
	List<String> resultsList = new ArrayList<String>();
	/*
	 * The parameters of search, see the {@link #Task1(String,String,int)
	 * Task1(String,String,int)}
	 */
	final private String root;
	final private String mask;
	final private int depth;

	/*
	 * Constructor
	 * 
	 * @param root root, from the depth of this element we begin the search
	 * 
	 * @param mask mask for search
	 * 
	 * @param depth the depth level for the search
	 */
	Task1(String root, String mask, int depth) {
		this.root = root;
		this.mask = mask;
		this.depth = depth;
	}

	/*
	 * The main method for searching, it uses the parameters input in the
	 * constructor
	 * 
	 * @throws java.io.IOException
	 */
	void search() throws IOException {
		Path currentPath, rootPath = getRootPath(root);
		Pair<Deque<Path>, Integer> currentSiblingPathsAndDepth;
		int currentDepth = 0;

		Stack<Pair<Deque<Path>, Integer>> stack = new Stack<Pair<Deque<Path>, Integer>>();

		stack.push(new Pair<>(
				getPathChildren(getParentPath(rootPath), rootPath,
						depth == currentDepth, mask), currentDepth));

		while (!stack.isEmpty()) {
			currentSiblingPathsAndDepth = stack.pop();
			Deque<Path> currentSiblingPaths = currentSiblingPathsAndDepth
					.getFirst();
			currentDepth = currentSiblingPathsAndDepth.getSecond();
			while (!currentSiblingPaths.isEmpty()) {
				currentPath = currentSiblingPaths.removeFirst();
				if (!currentSiblingPaths.isEmpty())
					stack.push(new Pair<>(
							currentSiblingPaths, currentDepth));

				if (currentDepth < depth) {
					currentSiblingPaths = getPathChildren(currentPath, null,
							depth == ++currentDepth, mask);

				} else
					break;
			}
		}
	}

	/*
	 * The method used by the {@link #search() search()}, finds the filetree
	 * children at the given depth
	 * 
	 * @param parent the parent Path(null if does not exist)
	 * 
	 * @param son on of the descendants of the parent, is not null if we want to
	 * find his siblings as the descendants of his parent
	 * 
	 * @param neededDepth shows if the current depth is the one we need; if so
	 * we run {@link #addPathToResults(String) addPathToResults(String)}
	 * 
	 * @param mask the mask for search, we could omit this parameter here, but in this
	 * case we should have overload this method in later tasks, that could seem
	 * not elegant
	 * 
	 * @return the double-array queue consisting of the children
	 * 
	 * @throws java.io.IOException
	 */
	Deque<Path> getPathChildren(Path parent, Path son, boolean neededDepth,
			String mask) throws IOException {


		Deque<Path> pathsList = new LinkedList<Path>();
		if (parent != null && !parent.toFile().isDirectory())
			return pathsList;
		if (son != null)
			pathsList.add(son);
		if (parent == null) {
			for (File f : File.listRoots())
				if (f.exists()) {
					if (neededDepth && f.getName().contains(mask))
						addPathToResults(f.toPath().toRealPath().toString());
					Path p = f.toPath();
					if (son == null || !son.equals(p))
						pathsList.add(p);
				}
			return pathsList;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
			for (Path entry : stream)
				if (entry.toFile().exists()) {
					if (neededDepth
							&& entry.getFileName().toString().contains(mask))
						addPathToResults(entry.toRealPath().toString());
					if (son == null || !son.equals(entry)) {
						pathsList.add(entry);
					}
				}
		} catch (DirectoryIteratorException ex) {
			throw ex.getCause();
		}
		return pathsList;
	}

	/*
	 * The method used by the {@link #search() search()} Finds the path for the
	 * given string
	 * 
	 * @param root the string path representation, it is not the absolute path,
	 * but the relative one( not <directory path>\<filename> but the <filename>
	 * itself)
	 * 
	 * @return the relative path based on the root
	 * 
	 * @throws java.io.IOException
	 */
	static Path getRootPath(String root) throws IOException {

		return Paths.get(root).toRealPath();

	}

	/*
	 * The method used by the {@link #search() search()} Finds the parent path
	 * for the given path
	 * 
	 * @param path the path for which we have to find the parent path
	 * 
	 * @return the parent real path for the given one or null if the given is one of
	 * the root elements in the file system
	 * 
	 * @throws java.io.IOException
	 */
	static Path getParentPath(Path path) throws IOException {

		return path.toRealPath().getParent();
	}

	/*
	 * Adds the filename to results if this path meets our criteria
	 * 
	 * @param filename the string representation of the path we want to add to
	 * results
	 */
	void addPathToResults(String filename) {
		resultsList.add(filename);
	}

	/*
	 * Outputs the results via the given printwriter or via the standard
	 * System.out if the printwriter is null
	 * 
	 * @param pw the printwriter to print the results
	 */
	void outputResults(PrintWriter pw) {
		println(resultsList.isEmpty() ? NO_RESULTS : RESULTS, pw);
		for (String s : resultsList)
			println(s, pw);
	}

	/*
	 * The method used by the {@link #outputResults(PrintWriter)
	 * outputResults(PrintWriter)}. Prints the given string using the given
	 * printwriter or the System.out if the given printwriter is null
	 * 
	 * @param s the string to print
	 * 
	 * @param pw the printwriter to print with
	 */
	void println(String s, PrintWriter pw) {
		if (pw == null)
			System.out.println(s);
		else
			pw.println(s);
	}

	/*
	 * Checks the args from the {@link #main(String[]) main(String[])} method
	 * 
	 * @param args the args from the {@link #main(String[]) main(String[])}
	 * method
	 * 
	 * @taskNum the number of Task java file
	 */
	static Pair<Pair<String, String>, Integer> argsCheck(String[] args, int taskNum) {
		if ((args.length != 3) || (!new File(args[0]).exists()))
			usage(taskNum);

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

	/*
	 * The method used by the {@link #argsCheck(String[],int)
	 * argsCheck(String[],int)}. Prints the information about the input error to
	 * the System.err and exits
	 * 
	 * @param taskNum the number of Task java file
	 */
	static void usage(int taskNum) {
		System.err.println(USAGE_BEG + taskNum + " " + USAGE_END);
		System.exit(-1);
	}

	/*
	 * The main method, checks the arguments using the {@link
	 * #argsCheck(String[],int) argsCheck(String[],int)}, creates the class's
	 * instance, runs {@link #search() search()} and then the {@link
	 * #outputResults(PrintWriter) outputResults(PrintWriter)}
	 * 
	 * @param args the args
	 */
	public static void main(String[] args) {
		Pair<Pair<String, String>, Integer> checkedArgs = argsCheck(args, 1);
		Task1 task1 = new Task1(checkedArgs.getFirst().getFirst(), checkedArgs
				.getFirst().getSecond(), checkedArgs.getSecond());
		try {
			task1.search();
		} catch (IOException e) {
			throw new RuntimeException(IO_ERROR, e);
		}
		task1.outputResults(null);
	}

}
