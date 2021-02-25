package nl.tue.visualcomputingproject.group9a.project.common.cache.policy;

import nl.tue.visualcomputingproject.group9a.project.common.cache.SimpleCacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;

/**
 * Interface for cache policies.
 * A cache policy tracks files registered via {@link #track(FileId, CacheManager, ReadWriteCacheClaim)}.
 * <br>
 * Tracked files can be deleted at any time.
 * When this occurs, the file is removed using the {@link ReadWriteCacheClaim} provided by
 * the original {@link #track} call.
 * The {@link CacheManager} provided in the same call will be notified using the
 * {@link CacheManager#releaseCacheClaim(ReadWriteCacheClaim)} function.
 * <br>
 * The size of files can be updated with {@link #update(FileId)}, and can be untracked
 * with {@link #untrack(FileId)}.
 */
public interface CachePolicy {
	/** The number of bytes in 1 KiB. */
	long SIZE_KiB = 1024L;
	/** The number of bytes in 1 MiB. */
	long SIZE_MiB = 1048576L;
	/** The number of bytes in 1 GiB. */
	long SIZE_GiB = 1073741824L;

	/**
	 * Tracks the file with the given ID which is reserved by the given cache manager.
	 * It uses the write access of the given claim only to delete the file when
	 * no longer needed. <br>
	 * <br>
	 * This action is atomic.
	 * 
	 * @param id          The ID of the file to add.
	 * @param manager     The manager requesting to track the file.
	 * @param claim       The read-write claim used to delete the file if needed.
	 * @param <Read>      The type of the read claims of the manager.
	 * @param <ReadWrite> The type of the read-write claims of the manager.
	 */
	<Read extends ReadCacheClaim, ReadWrite extends ReadWriteCacheClaim> void track(
			FileId id,
			SimpleCacheManager<Read, ReadWrite> manager,
			ReadWrite claim);

	/**
	 * Updates the size and freshness of the file with the given ID. <br>
	 * <br>
	 * This action is atomic.
	 * 
	 * @param id The ID of the file to update.
	 */
	void update(FileId id);

	/**
	 * Stops tracking the file with the given ID. <br>
	 * <br>
	 * This action is atomic.
	 *
	 * @param id The ID of the file to stop tracking.
	 * 
	 * @return {@code true} if the file became untracked. {@code false} otherwise.
	 */
	boolean untrack(FileId id);

	/**
	 * Sets the maximum size of the policy.
	 * 
	 * @param size The new maximum size of all files tracked by this policy.
	 */
	void setMaxSize(long size);

	/**
	 * @return The maximum size of all files tracked by this policy.
	 */
	long getMaxSize();

	/**
	 * @return The current size of all files tracked by this policy.
	 */
	long getCurSize();
	
}
