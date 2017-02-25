public class Main {
	public static void main(String[] args) {

		DummyObj obj = new DummyObj();
		Worker worker = new Worker(obj);
		worker.start();

		if (obj.myObj != null) {
			System.out.println("My Object : " + obj.toString());
		}
	}
}

class DummyObj {
	public Object myObj;
	DummyObj() {
		myObj = new Object();
	}

	@Override
	public String toString() {
		return myObj.toString();
	}
}

class Worker extends Thread {
	DummyObj obj;
	Worker(DummyObj obj) {
		this.obj = obj;
	}

	public void run() {
		obj.myObj = null;
	}
}
