package nl.tue.visualcomputingproject.group9a.project.common.cache.memory;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Memory store for storing a {@link MemoryCacheObject}.
 * 
 * @param <T> The type of the object to store.
 */
@NoArgsConstructor
@AllArgsConstructor
public class MemoryStore<T extends MemoryCacheObject> {
	/** The stored object. */
	private T obj = null;

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
	
}
