import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method; 

public class ConcGDB {

	static String inpProgName;
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	
	static Console cons;

	private static void gdbConsole() {
		cons.run();
	}

	private static void runProgram() {
		ProgramMain inpProgram = null;
		try {
			inpProgram = new ProgramMain(inpProgName);
		} catch (ClassNotFoundException | SecurityException
				| NoSuchMethodException | IllegalArgumentException
				| IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		inpProgram.start();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Usage: java ConcGDB <classname>");
			System.exit(0);
		}

		try {
			Controls.loadDeclaredClasses();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		String welcomeString = "################################################\n" +
				"------------------------------------------------\n" +
				"             Welcome to ConcGDB!                \n" +
				"------------------------------------------------\n" +
				"   Type 'help' for info on the available cmds   \n" +
				"################################################\n";

		System.out.print(ANSI_YELLOW + welcomeString + ANSI_RESET);
		
		cons = Console.getInstance();
		
		// Set it all up
		cons.regCmdTypes("replay", Console.ARG_STR);
		cons.regCmdAnnots("replay", "filename");
		cons.regCmdTypes("break", Console.ARG_LONG|Console.ARG_CHAR, Console.ARG_INT, Console.ARG_STR);
		cons.regCmdAnnots("break", "threadID", "linenum", "filename");
		cons.regCmdTypes("continue", Console.ARG_LONG|Console.ARG_CHAR);
		cons.regCmdAnnots("continue", "threadID");
		cons.regCmdTypes("next", Console.ARG_LONG);
		cons.regCmdAnnots("next", "threadID");
		cons.regCmdTypes("getfields", Console.ARG_LONG);
		cons.regCmdAnnots("getfields", "threadID");
		
		

		inpProgName = args[0];
		while (true) {
			runProgram();
			gdbConsole();
		}
	}

}

class ProgramMain extends Thread {
	Class<?> loadClass;
	Method meth;
	ProgramMain(String clsName) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		loadClass = Class.forName(clsName);
		meth = loadClass.getMethod("main", String[].class);
	}

	public void run(){
		String[] params = null;
		try {
			meth.invoke(null, (Object) params);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
	}	
}