import java.util.Random;

public class Assignment1_Buggy {

	public static void main(String[] args) {
		// The second argument is the mode - 
		// 0 - Completely Unsynchronized
		HashTable hashTable = new HashTable(5, 0);
		int numWorkers = 4;
		// Force a bug by picking elements that go to the same chain
		// i.e. 0, 5, 10, 15, 20
		int[] numRange = {0, 5, 10, 15, 20}; 
		WorkerThread [] threads = new WorkerThread[numWorkers];
		for (int i = 0; i < numWorkers; i++) {
			threads[i] = new WorkerThread(i, hashTable, numRange);
		}
		
		for (int i = 0; i < numWorkers; i++) {
			threads[i].start();
			try {
				Thread.sleep(10);
			} catch (Exception e1) {
			}
		}
	}
}

class WorkerThread extends Thread {
	int id;
	HashTable table;
	int[] numRange; // The list of numbers from which it needs to pick
	public String[] name = {"Just", "do", "it"};
	WorkerThread(int id, HashTable table, int[] numRange) {
		this.id = id;
		this.table = table;
		this.numRange = numRange;
	}
	
	public void run() {
		Random rand = new Random();
		int n;
		if (id % 2 == 1) {
			// Insert
			for (int i = 0; i < 5; i++) {
				n = numRange[rand.nextInt(numRange.length)];
				System.out.println("Thread " + id + " inserting " + n);
				table.Insert(n);
				try {  
					sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		} else {
			// Delete
			for (int i = 0; i < 5; i++) {
				n = numRange[rand.nextInt(numRange.length)];
				System.out.println("Thread " + id + " deleting " + n);
				table.Delete(n);
				try {
					sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}


