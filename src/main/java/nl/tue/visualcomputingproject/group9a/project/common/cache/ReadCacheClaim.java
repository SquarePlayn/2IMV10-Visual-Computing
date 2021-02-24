package nl.tue.visualcomputingproject.group9a.project.common.cache;

/**
 * A claim of a file determined by a {@link FileId} which supports read operations.
 */
public interface ReadCacheClaim {

	/**
	 * @return The ID of the file claimed.
	 */
	FileId getId();

	/**
	 * Returns whether this claim is valid.
	 * A claim becomes invalid only when it is released by a {@link CacheManager}.
	 * 
	 * @return {@code true} if this claim is valid. {@code false} otherwise.
	 */
	boolean isValid();

	/**
	 * Invalidates this claim.
	 * Attempting to read data from the claimed file will result in a {@link IllegalStateException}. <br>
	 * <br>
	 * <b>WARNING!</b><br>
	 * This function should ONLY be called by the {@link CacheManager}.
	 */
	void invalidate();

	/**
	 * @return The size of the claimed file, unaffected by the validity of this claim.
	 */
	long size();

	/**
	 * @return Whether the claimed file exists, unaffected by the validity of this claim.
	 */
	boolean exists();

	/**
	 * @return {@code true} if write operations are permitted on this claim by casting
	 *     it to the appropriate type.
	 */
	default boolean canWrite() {
		return (this instanceof ReadWriteCacheClaim);
	}
	
}
