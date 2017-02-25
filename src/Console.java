import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

class ArgWrapper<T> {
	T val;
	int type;
	public ArgWrapper(T val, int type) {
		this.val = val;
		this.type = type;
	}

	public T getVal() {
		return val;
	}

	public boolean isLong() {
		return type == Console.ARG_LONG;
	}

	public boolean isInt() {
		return type == Console.ARG_INT;
	}

	public boolean isChar() {
		return type == Console.ARG_CHAR;
	}

	public boolean isString() {
		return type == Console.ARG_STR;
	}
}

public class Console {
	public static int ARG_LONG = (1 << 0);
	public static int ARG_INT = (1 << 1);
	public static int ARG_CHAR = (1 << 2);
	public static int ARG_STR = (1 << 3);

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	// Bookkeeping
	private Map<String, List<Integer>> cmdArgTypes;
	private Map<String, List<String>> cmdArgAnnots;

	// Console Variables
	Scanner scan;
	boolean inReplayMode;
	String logFileName;
	FileWriter fw;

	private static Console consObj;
	private Console() {
		cmdArgTypes = new HashMap<String, List<Integer>>();
		cmdArgAnnots = new HashMap<String, List<String>>();
		scan = new Scanner(System.in);
		inReplayMode = false;
		setLogFileName();
		try {
			fw = new FileWriter(logFileName);
		} catch (IOException e1) {
			System.out.println("Could not create log file... Exiting");
			System.exit(1);
		}
	}

	public static Console getInstance() {
		if (consObj == null)
			consObj = new Console();
		return consObj;
	}

	private void setLogFileName() {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		logFileName = "log-" + timeStamp;
		logFileName = "logs_conc-gdb/" + logFileName;
	}

	@SuppressWarnings("unused")
	private String getTypeStr(Integer type) {
		String typeStr = null;
		if ((type & ARG_LONG) != 0) {
			if (typeStr != null) typeStr += "|long";
			else typeStr = "long";
		}

		if ((type & ARG_INT) != 0) {
			if (typeStr != null) typeStr += "|int";
			else typeStr = "int";
		}

		if ((type & ARG_STR) != 0) {
			if (typeStr != null) typeStr += "|string";
			else typeStr = "string";
		}

		if ((type & ARG_CHAR) != 0) {
			if (typeStr != null) typeStr += "|char";
			else typeStr = "char";
		}

		return typeStr;
	}

	private String argHelpStr(String cmd) {
		String helpStr = ""; 
		List<Integer> argTypes = cmdArgTypes.get(cmd);
		List<String> argAnnots = cmdArgAnnots.get(cmd);
		for (int i = 0; i < argTypes.size(); i++) {
			Integer type = argTypes.get(i);
			String typeStr = getTypeStr(type);
			helpStr += "[" + argAnnots.get(i) + "<" + typeStr + ">] ";
		}

		return helpStr;
	}

	private void errCmdTypes(String cmd) {
		System.out.println(ANSI_CYAN + "error : " + cmd + " requires arguments " +
				argHelpStr(cmd) + ANSI_RESET);
	}

	private List<Object> validateArgs(String cmd, Scanner sc) {
		List<Object> args = new ArrayList<Object>();
		for (Integer argType : cmdArgTypes.get(cmd)) {
			// According to priority
			if (((argType & ARG_LONG) != 0) && sc.hasNextLong()) {
				args.add(new ArgWrapper<Long>(sc.nextLong(), ARG_LONG));
			} else if (((argType & ARG_INT) != 0) && sc.hasNextInt()) {
				args.add(new ArgWrapper<Integer>(sc.nextInt(), ARG_INT));
			} else if (((argType & ARG_CHAR) != 0) && sc.hasNext()) {
				args.add(new ArgWrapper<Character>(sc.next(".").charAt(0), ARG_CHAR));
			} else if (((argType & ARG_STR) != 0) && sc.hasNext()) {
				args.add(new ArgWrapper<String>(sc.next(), ARG_STR));
			} else {
				errCmdTypes(cmd);
				return null;
			}
		}

		return args;
	}

	public void regCmdTypes(String cmdName, Integer... args) {
		if (!cmdArgTypes.containsKey(cmdName)) {
			List<Integer> argTypes = new ArrayList<Integer>();
			for (Integer arg : args)
				argTypes.add(arg);

			cmdArgTypes.put(cmdName, argTypes);
		}
	}

	public void regCmdAnnots(String cmdName, String... argAnnots) {
		if (!cmdArgAnnots.containsKey(cmdName)) {
			List<String> annots = new ArrayList<String>();
			for (String arg : argAnnots)
				annots.add(arg);

			cmdArgAnnots.put(cmdName, annots);
		}
	}

	private void errExit(String msg) {
		System.out.println(msg);
		cleanUp();
		System.exit(1);
	}

	private void cleanUp() {
		scan.close();
		try { fw.close(); } catch (IOException e1) {};
	}

	private void genErr(String msg) {
		System.out.println(ANSI_CYAN + msg + ANSI_RESET);
	}

	private void dispHelpStr() {
		String str = "" +
		"Note : In all cases <threadID> can be a number or the character '*' (to mean all threads)\n" +
		ANSI_GREEN + "help" + ANSI_RESET + " : Display this message\n" +
		ANSI_GREEN + "break" + ANSI_RESET +" <threadID> <linenum> <filename> : Add a breakpoint for thread with ID <threadID> at line <linenum> in file <filename>.java\n" +
		ANSI_GREEN + "continue " + ANSI_RESET + "<threadID> : Continue the thread with ID <threadID>. If threadID is '*', all threads continue\n" +
		ANSI_GREEN + "next" + ANSI_RESET + " <threadID> : Make thread with ID <threadID> take a step (execute a single line)\n" +
		ANSI_GREEN + "getfields" + ANSI_RESET +" <threadID> : Display the values of class variables for the thread with ID <threadID> (Experimental)\n" +
		ANSI_GREEN + "info" + ANSI_RESET + " : Display the current status of each thread (only those which are alive)\n" +
		ANSI_GREEN + "run" + ANSI_RESET + " : Restart the session if all the threads have exited\n" +
		ANSI_GREEN + "replay" + ANSI_RESET + " <filename> : Replay the commands stored in the file given by <filename>. It should be without spaces, and the path must be provided.\n";
		
		System.out.println(str + ANSI_RESET);
	}
	
	private void logCmd(String cmd) {
		try {
			fw.write(cmd + "\n");
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
			try { fw.close(); } catch (IOException e1) {};
			System.exit(1);
		}
	}

	@SuppressWarnings("unchecked")
	private long checkTIDwithAllThreads(List<Object> args, String cmd) {
		long tid;
		char chr;

		if (((ArgWrapper<?>) args.get(0)).isLong()) {
			tid = ((ArgWrapper<Long>) args.get(0)).getVal();
			if (!Controls.checkIfThreadExists(tid)) {
				genErr("error : No thread with ID " + tid + " exists");
				return -2;
			} 
			
			tid = Controls.cnt2id.get(tid);
		} else {
			chr = ((ArgWrapper<Character>) args.get(0)).getVal();
			if (chr != '*') {
				genErr("error : Invalid thread ID. Should be an integer or a '*' (all threads)");
				return -2;
			}

			tid = -1;
		}

		return tid;

	}

	@SuppressWarnings("unchecked")
	private long checkTID(List<Object> args, String cmd) {
		long tid = -2;

		if (((ArgWrapper<?>) args.get(0)).isLong()) {
			tid = ((ArgWrapper<Long>) args.get(0)).getVal();
			if (!Controls.checkIfThreadExists(tid)) {
				genErr("error : No thread with ID " + tid + " exists");
				return -2;
			} 
			
			tid = Controls.cnt2id.get(tid);
		}

		return tid;

	}

	@SuppressWarnings({ "unchecked", "resource" })
	public void run() {
		String inp = null, cmd = null;
		while (true) {
			System.out.print(ANSI_PURPLE + ">>> " + ANSI_RESET);
			System.out.flush();

			if (scan.hasNextLine())
				inp = scan.nextLine();
			else if (inReplayMode) {
				inReplayMode = false;
				scan = new Scanner(System.in);
				inp = scan.nextLine();
			} else
				errExit("error : Input empty. Exiting...");

			if (inReplayMode) System.out.println("[REPLAY] " + inp);

			Scanner inpScanner = new Scanner(inp);
			if (inpScanner.hasNext())
				cmd = inpScanner.next();
			else
				cmd = "";

			List<Object> args;
			long tid;
			int lineNum;
			String fileName;

			switch(cmd) {
			case "":
				break;

			case "help":
				dispHelpStr();
				break;

			case "run":
				if (!Controls.allThreadsFinished()) {
					genErr("All threads haven't exited yet");
					break;
				} 
				
				Controls.resetTCnt();
				Controls.clear();
				try { fw.close(); } catch (IOException e1) {};
				setLogFileName();
				try {
					fw = new FileWriter(logFileName);
				} catch (IOException e1) {
					System.out.println("Could not create log file... Exiting");
					System.exit(1);
				}
				
				genErr("Restarting...");
				return;

			case "replay":
				args = validateArgs(cmd, inpScanner);
				if (args == null) break;
				fileName = ((ArgWrapper<String>) args.get(0)).getVal();
				inReplayMode = true;
				try {
					InputStream newInp = new FileInputStream(fileName);
					scan = new Scanner(newInp);
				} catch (FileNotFoundException e) {
					genErr("error : file " + fileName + " does not exist");
				}
				break;

			case "break":
				args = validateArgs(cmd, inpScanner);
				if (args == null) break;
				tid = checkTIDwithAllThreads(args, cmd);
				if (tid == -2) break;
				lineNum = ((ArgWrapper<Integer>) args.get(1)).getVal();
				fileName = ((ArgWrapper<String>) args.get(2)).getVal();
				logCmd(inp);
				Controls.addBreakPoint(tid, lineNum, fileName);
				break;

			case "continue":
				args = validateArgs(cmd, inpScanner);
				if (args == null) break;
				tid = checkTIDwithAllThreads(args, cmd);
				if (tid == -2) break;
				logCmd(inp);
				Controls.continueCmd(tid);
				break;

			case "next":
				args = validateArgs(cmd, inpScanner);
				if (args == null) break;
				tid = checkTID(args, cmd);
				if (tid == -2) break;
				logCmd(inp);
				Controls.nextCmd(tid);
				break;

			case "getfields":
				args = validateArgs(cmd, inpScanner);
				if (args == null) break;
				tid = checkTID(args, cmd);
				if (tid == -2) break;
				Controls.displayFields(tid);
				break;

			case "info":
				Controls.displayThreadInfo();
				break;

			case "quit":
				inpScanner.close();
				cleanUp();
				System.out.println("Exiting...");
				System.exit(0);
				break;

			default:
				System.out.println(ANSI_RED + "Invalid Command : " + cmd + ANSI_RESET);
				break;

			}

			inpScanner.close();

		}

	}
}
