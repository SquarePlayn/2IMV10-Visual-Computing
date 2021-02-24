package nl.tue.visualcomputingproject.group9a.project.common.cache;


/**
 * A claim of a file determined by a {@link FileId} which supports read operations.
 */
public interface ReadWriteCacheClaim
		extends ReadCacheClaim {

	/**
	 * Deletes the file of this claim.
	 * The claim must be valid when the file is deleted.
	 */
	void delete();
	
}
