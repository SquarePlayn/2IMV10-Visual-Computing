package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Memory store for storing a {@link CacheableObject}.
 * 
 * @param <T> The type of the object to store.
 */
@NoArgsConstructor
@AllArgsConstructor
public class MemoryStore<T extends CacheableObject> {
	/** The stored object. */
	private T obj = null;
	@Getter
	private Lock lock = new ReentrantLock(); 

	/**
	 * @return The stored object.
	 */
	public T get() {
		return obj;
	}

	/**
	 * Sets the object.
	 * 
	 * @param obj The new object.
	 */
	public void set(T obj) {
		this.obj = obj;
	}

	/**
	 * @return The amount of memory used by the stored object in bytes,
	 *     or {@code 0L} if the object is {@code null}.
	 */
	public long memorySize() {
		return (obj == null ? 0L : obj.memorySize());
	}

	/**
	 * @return Whether the stored object is {@code null}.
	 */
	public boolean isEmpty() {
		return (obj == null);
	}
	
	public void lock() {
		lock.lock();
	}
	
	public void unlock() {
		lock.unlock();
	}
	
	public boolean tryLock() {
		return lock.tryLock();
	}
	
}
