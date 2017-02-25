public class Main {
	public static void main(String[] args) {

		Object obj1 = new Object();
		Object obj2 = new Object();

		Worker worker = new Worker(obj1, obj2);
		worker.start();

		synchronized(obj1) {
			synchronized(obj2) {
				System.out.println("Hello World from the main thread");
			}
		}
	}
}

class Worker extends Thread {
	Object obj1, obj2;
	Worker(Object obj1, Object obj2) {
		this.obj1 = obj1;
		this.obj2 = obj2;
	}

	public void run() {
		synchronized(obj2) {
			synchronized(obj1) {
				System.out.println("Hello World from Worker Thread");
			}
		}
	}
}
