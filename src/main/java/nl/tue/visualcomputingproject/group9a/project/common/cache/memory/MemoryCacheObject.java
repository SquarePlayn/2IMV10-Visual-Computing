package nl.tue.visualcomputingproject.group9a.project.common.cache.memory;

/**
 * Interface for in-memory cacheable objects.
 */
public interface MemoryCacheObject {

	/**
	 * @return The size of the object in bytes.
	 */
	long memorySize();
	
}
