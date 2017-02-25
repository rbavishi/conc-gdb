import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Controls {
	/* Misc */
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	
	/* Important Stuff */
	private static Object gdbThreadSync = new Object();
	private static Object clsSync = new Object();

	/* Auxiliary Data */
	private static Map<Long, Integer> threadCurLine = new ConcurrentHashMap<Long, Integer>();
	private static Map<Long, String> threadCurFile = new ConcurrentHashMap<Long, String>();
	private static Map<Long, Thread> threadObjs = new ConcurrentHashMap<Long, Thread>();
	private static Map<Long, Set<BreakPoint>> breakPoints = new ConcurrentHashMap<Long, Set<BreakPoint>>();
	private static Map<Long, Boolean> pausePoints = new ConcurrentHashMap<Long, Boolean>();
	private static Map<Long, Boolean> threadPaused = new ConcurrentHashMap<Long, Boolean>();
	private static Set<BreakPoint> globalBreakPoints = new HashSet<BreakPoint>();
	private static Set<String> declaredClasses = new HashSet<String>();

	private static Map<Long, Object> syncVars = new ConcurrentHashMap<Long, Object>();

	private static long allowedThread = -2;
	private static long threadCnt = 0; // Assigning custom IDs
	public static Map<Long, Long> id2Cnt = new ConcurrentHashMap<Long, Long>();
	public static Map<Long, Long> cnt2id = new ConcurrentHashMap<Long, Long>();
	
	public static synchronized void clear() {
		threadCurLine.clear();
		threadCurFile.clear();
		threadObjs.clear();
		breakPoints.clear();
		pausePoints.clear();
		threadPaused.clear();
		globalBreakPoints.clear();
		syncVars.clear();
		id2Cnt.clear();
		cnt2id.clear();
	}

	private static synchronized long getTCnt() {
		long val = threadCnt++;
		return val;
	}
	
	public static synchronized void resetTCnt() {
		threadCnt = 0;
	}
	
	public static synchronized void setAllowedThread(long id) {
		allowedThread = id;
	}

	public static synchronized long getAllowedThread() {
		return allowedThread;
	}

	public static synchronized void regClassName(String name) {
		declaredClasses.add(name);
	}
	
	

	public static void regAllClasses() throws IOException {
		FileWriter fw = new FileWriter(".conc-gdb-declared-classes.txt");
		try {
			for (String clsName:declaredClasses) {
				fw.write(clsName + "\n");
			}
		} finally {
			if (fw != null)
				fw.close();
		}
	}

	public static synchronized void loadDeclaredClasses() throws IOException {
		Reader inpFile = null;
		String line;
		BufferedReader br;

		try {
			inpFile = new FileReader(".conc-gdb-declared-classes.txt");
			br = new BufferedReader(inpFile);
			while ((line = br.readLine()) != null) {
				declaredClasses.add(line);
			}
		} finally {
			if (inpFile != null)
				inpFile.close();
		}

	}

	public static void regThreadExistence(int lineNum, String fileName) {
		/* This will be done at the starting of every method.
		 * This is because the internal thread IDs can be reused if the threads die.
		 * We want the info to be updated. The GDB thread can pick up data from this as well
		 */
		long myID = Thread.currentThread().getId();
		threadCurLine.put(myID, lineNum);
		threadCurFile.put(myID, fileName);
		threadObjs.put(myID, Thread.currentThread());
		threadPaused.put(myID, false);

		/* The following may break if an ID is reused, but that is unlikely */
		if (!(breakPoints.containsKey(myID)))
			breakPoints.put(myID, new HashSet<BreakPoint>());
		if (!pausePoints.containsKey(myID))
			pausePoints.put(myID, false);
		if (!syncVars.containsKey(myID))
			syncVars.put(myID, new Object());
		if (!id2Cnt.containsKey(myID)) {
			long id = getTCnt();
			id2Cnt.put(myID, id);
			cnt2id.put(id, myID);
		}
	}

	public static void selfPause(int lineNum, String fileName) {
		long myID = Thread.currentThread().getId();
		// Don't need synchronization here as the GDB thread cannot run in parallel
		// with any of the program threads
		// Go to sleep
		Object mySyncVar = syncVars.get(myID);

		threadCurLine.put(myID, lineNum);
		threadCurFile.put(myID, fileName);
		setAllowedThread(-2); // GDB Thread

		synchronized (gdbThreadSync) {
			gdbThreadSync.notifyAll();
		}

		// Go to sleep yourself
		synchronized (mySyncVar) {
			long allowedT = getAllowedThread();
			while (allowedT != -1 && allowedT != myID) {
				try {
					threadPaused.put(myID, true);
					mySyncVar.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				allowedT = getAllowedThread();

			}
		}
	}
	
	public static void addBreakPoint(Long id, int lineNum, String fileName) {
		if (id == -1) { // For everyone
			globalBreakPoints.add(new BreakPoint(lineNum, fileName));
			System.out.println(ANSI_CYAN + "Breakpoint added at line " + lineNum + " in file " + fileName + ANSI_RESET);
		} else {
			if (breakPoints.containsKey(id)) {
				System.out.println(ANSI_CYAN + "Breakpoint added at line " + lineNum + " in file " + 
						fileName + " for thread " + id2Cnt.get(id) + ANSI_RESET);
				breakPoints.get(id).add(new BreakPoint(lineNum, fileName));
			}
		}
	}
	
	public static void nextCmd(Long id) {
		if (pausePoints.containsKey(id)) {
			pausePoints.put(id, true);
			continueCmd(id);
		}
	}
	
	public static void checkForBreaks(int lineNum, String fileName) {

		// Should be called by threads belonging to input program
		long myID = Thread.currentThread().getId();
		BreakPoint bp = new BreakPoint(lineNum, fileName);
		Object mySyncVar = syncVars.get(myID);

		// Don't need synchronization here as the GDB thread cannot run in parallel
		// with any of the program threads
		if ((!pausePoints.containsKey(myID) || !pausePoints.get(myID)) && 
			!(breakPoints.get(myID).contains(bp)) && !(globalBreakPoints.contains(bp)))
			return;
		
		if (pausePoints.containsKey(myID) && pausePoints.get(myID)) {
			pausePoints.put(myID,  false);
		}

		synchronized(clsSync) {
			synchronized(mySyncVar) {
				System.out.println(ANSI_CYAN + "Thread " + id2Cnt.get(myID) + " at line " + lineNum + " in file " + fileName + ANSI_RESET);
			}
		}
		
		selfPause(lineNum, fileName);
	}

	private static void displayFieldsRec(Object obj, String prefix, boolean nestCustObj, boolean firstLev) {
		Field[] fields = obj.getClass().getDeclaredFields();
		if (!nestCustObj) {
			System.out.print(obj);
			return;
		}

		for (Field f:fields) {
			try {
				if (f.getType().isPrimitive()) {
					System.out.print(ANSI_GREEN + prefix + f.getName() + " : " + f.get(obj) + ANSI_RESET);
				} else if (f.getType().isArray()) {
					if (f.getType().getComponentType().isPrimitive()) {
						Class<?> compType = f.getType().getComponentType();
						String dispString = "<unprintable>";
						if (compType.equals(Long.TYPE))
							dispString = Arrays.toString((long[]) f.get(obj));
						else if (compType.equals(Integer.TYPE))
							dispString = Arrays.toString((int[]) f.get(obj));
						else if (compType.equals(Short.TYPE))
							dispString = Arrays.toString((short[]) f.get(obj));
						else if (compType.equals(Character.TYPE))
							dispString = Arrays.toString((char[]) f.get(obj));
						else if (compType.equals(Boolean.TYPE)) 
							dispString = Arrays.toString((boolean[]) f.get(obj));
						else if (compType.equals(Byte.TYPE))
							dispString = Arrays.toString((byte[]) f.get(obj));
						else if (compType.equals(Float.TYPE))
							dispString = Arrays.toString((float[]) f.get(obj));
						else if (compType.equals(Double.TYPE))
							dispString = Arrays.toString((double[]) f.get(obj));


						dispString = prefix + f.getName() + " : " + dispString;
						System.out.print(ANSI_BLUE + dispString + ANSI_RESET);

					} else {
						System.out.print(ANSI_BLUE + prefix + f.getName() + " : " + ANSI_RESET);
						Object[] arr = (Object[]) f.get(obj);
						if (arr == null)
							System.out.print("null");
						else {
							System.out.print ("{ ");
							for (Object o : arr) {
								displayFieldsRec(o, "", false, false);
								System.out.print(", ");
							}
							System.out.print (" }");
						}

					}
				} else {
					Object fieldObj = f.get(obj);
					if (fieldObj == null)
						System.out.print(ANSI_YELLOW + "null" + ANSI_RESET);
					else if (declaredClasses.contains(fieldObj.getClass().getName()) && nestCustObj) {
						displayFieldsRec(fieldObj, prefix + f.getName() + ".", true, true);
					} else {
						System.out.print(ANSI_YELLOW + prefix + f.getName() + " : " + fieldObj + ANSI_RESET);
					}
				}

			} catch (IllegalArgumentException | IllegalAccessException e) {
				System.out.print("<inaccessible>");
				return;
			}

			if (firstLev)
				System.out.println();
		}
	}

	public static void displayFields(long tID) {
		Object obj = threadObjs.get(tID);
		displayFieldsRec(obj, "", true, true);
	}
	
	public static boolean allThreadsFinished() {
		long myID = Thread.currentThread().getId();
		for(Map.Entry<Long, Thread> threadEntry : threadObjs.entrySet()) {
			Thread t = threadEntry.getValue();
			long tid = threadEntry.getKey();
			if (syncVars.containsKey(tid)) {
				Object syncVar = syncVars.get(tid);

				synchronized(syncVar) {
					if (t.isAlive() && tid != myID) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

	private static boolean allThreadsFinishedOrPaused() {
		long myID = Thread.currentThread().getId();
		for(Map.Entry<Long, Thread> threadEntry : threadObjs.entrySet()) {
			Thread t = threadEntry.getValue();
			long tid = threadEntry.getKey();
			if (syncVars.containsKey(tid)) {
				Object syncVar = syncVars.get(tid);

				synchronized(syncVar) {
					if ((t.isAlive() && !(threadPaused.get(tid))) && tid != myID) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public static boolean checkIfThreadExists(Long id) {
		return cnt2id.containsKey(id);
	}

	public static void displayThreadInfo() {
		long myID = Thread.currentThread().getId();
		boolean atleastOneThreadAlive = false;
		for (Long id : threadObjs.keySet()) {
			if (id != myID && threadObjs.get(id).isAlive()) {
				atleastOneThreadAlive = true;
				long dispID = id2Cnt.get(id);
				System.out.println(ANSI_CYAN + "Thread " + dispID + " at line " + threadCurLine.get(id) + " in file " + threadCurFile.get(id) + ANSI_RESET);
			}
		}

		if (!atleastOneThreadAlive)
			System.out.println(ANSI_CYAN + "All threads have exited" + ANSI_RESET);
	}

	public static void wakeUpThreads(Long id) {
		if (id == -1) {
			for (Map.Entry<Long, Object> entry : syncVars.entrySet()) {
				Object syncObj = entry.getValue();
				long t_id = entry.getKey();
				synchronized(syncObj) {
					syncObj.notifyAll();
					threadPaused.put(t_id, false);
				}
			}
		} else {
			Object syncObj = syncVars.get(id);
			synchronized(syncObj) {
				syncObj.notifyAll();
				threadPaused.put(id, false);
			}
		}

	}

	public static void continueCmd(Long id) {
		// Should be called by ConcGDB
		setAllowedThread(id);
		wakeUpThreads(id);

		synchronized(gdbThreadSync) {
			//while ((allowedThread != -2) || !allThreadsFinishedOrPaused()) {
			while ( !allThreadsFinishedOrPaused()) {
				try {
					gdbThreadSync.wait(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			setAllowedThread(-2);
		}

	}
}

class BreakPoint {
	int lineNum;
	String fileName;
	BreakPoint(int _lineNum, String _filename) {
		lineNum = _lineNum;
		fileName = _filename;
	}

	@Override
	public int hashCode() {
		return lineNum;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BreakPoint))
			return false;

		if (obj == this)
			return true;

		BreakPoint rhs = (BreakPoint) obj;
		return (rhs.lineNum == this.lineNum && rhs.fileName.equals(this.fileName));
	}
}