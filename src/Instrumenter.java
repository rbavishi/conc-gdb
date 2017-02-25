import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import soot.*;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.util.Chain;
import soot.tagkit.*;

@SuppressWarnings("unused")
public class Instrumenter {
	public static void main(String[] args) {
		/* check the arguments */
		if (args.length == 0) {
			System.err.println("Usage: java Instrumenter [options] classname");
			System.exit(0);
		}

		/* add a phase to transformer pack by call Pack.add */
		Pack jtp = PackManager.v().getPack("jtp");
		jtp.add(new Transform("jtp.instrumenter", new GDBInstrumenter()));

		/* Give control to Soot to process all options,
		 * InvokeStaticInstrumenter.internalTransform will get called.
		 */
		soot.Main.main(args);
		try {
			Controls.regAllClasses();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class GDBInstrumenter extends BodyTransformer {
	/* some internal fields */
	static SootClass controlsClass;
	static SootMethod regThreadExistence, selfPause, checkForBreaks;

	static {
		controlsClass    = Scene.v().loadClassAndSupport("Controls");
		selfPause = controlsClass.getMethod("void selfPause(int,java.lang.String)");
		checkForBreaks = controlsClass.getMethod("void checkForBreaks(int,java.lang.String)");
		regThreadExistence = controlsClass.getMethod("void regThreadExistence(int,java.lang.String)");
	}

	private static String getFileName(SootClass cls) {
		for (Tag tg:cls.getTags()) {
			if (tg instanceof SourceFileTag) {
				return ((SourceFileTag) tg).getSourceFile();
			}
		}

		return "unknown";
	}

	private static int getLineNum(Unit unit) {
		for(Tag tg:unit.getTags()) {
			if (tg instanceof LineNumberTag)
				return ((LineNumberTag) tg).getLineNumber();
		}

		return -1;
	}

	@SuppressWarnings("rawtypes")
	protected void internalTransform(Body body, String phase, Map options) {
		SootMethod method = body.getMethod();
		Controls.regClassName(method.getDeclaringClass().getName());
		
		String filename = getFileName(method.getDeclaringClass());

		String signature = method.getSubSignature();
		boolean isMain = signature.equals("void main(java.lang.String[])");
		boolean firstStatement = true;
		int prevLineNum = -1;
		int lineNum;

		// debugging
		System.out.println("instrumenting method : " + method.getSignature());

		// get body's unit as a chain
		Chain<Unit> units = body.getUnits();

		// get a snapshot iterator of the unit since we are going to
		// mutate the chain when iterating over it.
		Iterator stmtIt = units.snapshotIterator();
	
		while (stmtIt.hasNext()) {

			Stmt stmt = (Stmt)stmtIt.next();
			// If this is a main function, and this is just the beginning,
			// we need to break here
			if (stmt instanceof IdentityStmt) {
				// Can't insert before these guys apparently
				continue;
			}

			lineNum = getLineNum(stmt);

			// Register the thread
			if (firstStatement) {
				InvokeExpr regExpr = Jimple.v().newStaticInvokeExpr(regThreadExistence.makeRef(), IntConstant.v(lineNum), StringConstant.v(filename));
				Stmt regExprStmt = Jimple.v().newInvokeStmt(regExpr);
				units.insertBefore(regExprStmt, stmt);
			}

			// Self-break : Prevent the thread from running
			if (isMain && firstStatement) {
				InvokeExpr selfPauseExpr = Jimple.v().newStaticInvokeExpr(selfPause.makeRef(), IntConstant.v(lineNum), StringConstant.v(filename));
				Stmt selfPauseStmt = Jimple.v().newInvokeStmt(selfPauseExpr);
				units.insertBefore(selfPauseStmt, stmt);

			}

			firstStatement = false;

			if (lineNum != prevLineNum) {
				// Insert Breakpoint instrumentation here
				prevLineNum = lineNum;
				InvokeExpr breakPointExpr = Jimple.v().newStaticInvokeExpr(checkForBreaks.makeRef(), IntConstant.v(lineNum), StringConstant.v(filename));
				Stmt breakPointStmt = Jimple.v().newInvokeStmt(breakPointExpr);
				units.insertBefore(breakPointStmt, stmt);
			}
		}

		/*
		// Add De-Registration code when the method finishes
		stmtIt = units.snapshotIterator();
		while (stmtIt.hasNext()) {
			Stmt stmt = (Stmt)stmtIt.next();

			// check if the instruction is a return with/without value
			if ((stmt instanceof ReturnStmt)
					||(stmt instanceof ReturnVoidStmt)) {
				
				InvokeExpr deRegExpr = Jimple.v().newStaticInvokeExpr(deRegThread.makeRef());
				Stmt breakPointStmt = Jimple.v().newInvokeStmt(deRegExpr);
				units.insertBefore(breakPointStmt, stmt);

			}
		}
		*/
	}
}