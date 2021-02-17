package nl.tue.visualcomputingproject.group9a.project.common.old_cache;

/**
 * Factory class for serializing and deserializing a target class to a string
 * which will be used as a file name.
 * 
 * @param <V> The target class.
 */
public interface CacheNameFactory<V> {

	/**
	 * Creates a string from the given object.
	 * 
	 * @param val The object to create the string for.
	 * 
	 * @return The string representing the object.
	 */
	String getString(V val);

	/**
	 * Creates an object from the given string.
	 * 
	 * @param str The string to create object from.
	 * 
	 * @return The object created from the string.
	 */
	V fromString(String str);
	
}
