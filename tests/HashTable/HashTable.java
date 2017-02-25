import java.util.concurrent.locks.*;

public class HashTable {
	int size;
	int mode;
	HashChain[] table;
	HashTable(int size, int mode) {
		// Size will determine the hash function as well (it'll be % size)
		this.size = size;
		this.mode = mode;
		table = new HashChain[size];
		for (int i = 0; i < size; i++) {
			table[i] = new HashChain();
		}
	}
	
	boolean Insert(int key) {
		return InsertUnsynchronized(key);
	}
	
	boolean Delete(int key) {
		return DeleteUnsynchronized(key);
	}
	
	boolean InsertUnsynchronized(int key) {
		// This will be the completely unsynchronized version
		// which will help us discover bugs
		// Return value is whether an element was inserted or not
		return table[key % size].InsertUnsynchronized(key);
	}
	
	boolean DeleteUnsynchronized(int key) {
		// Fully Unsynchronized
		// Return value is whether an element was deleted or not
		return table[key % size].DeleteUnsynchronized(key);
	}
}

class HashChainElem {
	int value; // The hash table will contain integers
	HashChainElem next;
	private final ReentrantLock lock = new ReentrantLock();
	HashChainElem(int value) {
		next = null;
		this.value = value;
	}
	
	void ElemLock() {
		lock.lock();
	}
	
	void ElemUnlock() {
		lock.unlock();
	}
}

class HashChain {
	HashChainElem head;
	HashChainElem tail;
	HashChain() {
		head = null;
		tail = null;
	}
	
	boolean InsertUnsynchronized(int key) {
		HashChainElem cur = head;
		while (cur != null) {
			if (cur.value == key)
				return false;

			cur = cur.next;
		}
		
		HashChainElem newElem = new HashChainElem(key);
		// head is compared against null just to force a visible exception
		// Using tail causes a different kind of bug (loss of entries) but
		// that is not so easy to demonstrate
		if (head != null) { 
			tail.next = newElem;
			tail = newElem;
		} else {
			head = newElem;
			tail = head;
		}
		
		return true;
	}
	
	// If element doesn't exist, it won't do anything at all
	boolean DeleteUnsynchronized(int key) {
		HashChainElem cur = head;
		HashChainElem prev = null;
		while (cur != null) {
			if (cur.value == key) {
				// Delete this
				if (prev != null)
					prev.next = cur.next; // All hail the GC!
				else
					head = cur.next;
				
				return true;
			}
			prev = cur;
			cur = cur.next;
		}
		
		return false;
	}
}

