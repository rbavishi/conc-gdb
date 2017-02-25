import java.util.concurrent.locks.*;

public class DiningPhilo {

	public static void main(String[] args) {
		Fork[] forks = new Fork[5];
		for (int i = 0; i < 5; i++) {
			forks[i] = new Fork();
		}
		
		Philosopher[] philos = new Philosopher[5];
		for (int i = 0; i < 5; i++) {
			philos[i] = new Philosopher(i, forks);
		}
		
		for (int i = 0; i < 5; i++) {
			philos[i].start();
		}
	}

}

class Fork {
	Lock lock;
	Fork() {
		lock = new ReentrantLock();
	}
	
	void Pickup() {
		lock.lock();
	}
	
	void PutDown() {
		lock.unlock();
	}
	
}

class Philosopher extends Thread {
	Fork[] forks;
	int id;
	Philosopher(int _id, Fork[] _forks) {
		forks = _forks;
		id = _id;
	}	
	
	public void run() {
		while (true) {
			// Pick forks
			forks[(id + 1) % 5].Pickup();
			forks[(id + 4) % 5].Pickup();
				
			System.out.println("Philosopher " + id + " is Eating...");
			
			try {
				sleep(1000);
			} catch (Exception e) {
			}

			// Release the stuff
			forks[(id + 1) % 5].PutDown();
			// Put down the second fork
			forks[(id + 4) % 5].PutDown();

			System.out.println("Philosopher " + id + " is thinking...");
			
			try {
				sleep(1000);
			} catch (Exception e) {
			}
			
		}
			
	}
}


// To try out - 
/*
Blocking Queue (put/take) (Try the producer/consumer problem)

CountDownLatch
CountDown() - decrements the count
await() waits for the count to reach zero. Thread blocks till this point. When the count becomes 
zero, all the threads blocked in await become unblocked.

Threadpools
*/
