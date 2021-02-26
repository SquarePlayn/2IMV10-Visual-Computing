package nl.tue.visualcomputingproject.group9a.project.common.cache;

/**
 * Interface for cacheable objects.
 */
public interface CacheableObject {

	/**
	 * @return The size of the object in bytes.
	 */
	long memorySize();
	
}
