public class Main {
	public static void main(String[] args) {

		DummyObj obj = new DummyObj();
		Worker worker = new Worker(obj);
		worker.start();

		DummyObj.doSomething();
		obj.iopending = true;
		while(obj.iopending) {
			// Keep Waiting
		}

		System.out.println("Done!");
	}
}

class DummyObj {
	public boolean iopending;
	public static boolean dummy;
	public static void doSomething() {
		dummy = true;
	}
}

class Worker extends Thread {
	DummyObj obj;
	Worker(DummyObj obj) {
		this.obj = obj;
	}

	public void run() {
		obj.iopending = false;
	}
}
