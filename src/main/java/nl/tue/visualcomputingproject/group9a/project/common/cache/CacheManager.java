package nl.tue.visualcomputingproject.group9a.project.common.cache;

public interface CacheManager<Read extends ReadCacheClaim, ReadWrite extends ReadWriteCacheClaim> {

	/**
	 * Requests a read claim on the given file to prevent the cache manager
	 * from deleting it.
	 * It is possible that there are multiple read claims for the same file.
	 * The function returns {@code null} if the file has a read-write claim
	 * or does not exist. <br>
	 * <br>
	 * This operation can be considered atomic.
	 * That is, the file will not be modified in any way if, and only if,
	 * a non-null claim is returned.
	 *
	 * @param id The id of the file to claim.
	 *
	 * @return A read claim for the given file, or {@code null} if none could
	 *     be obtained.
	 */
	Read requestReadClaim(FileId id);

	/**
	 * Requests a read-write claim on the given file to prevent the cache
	 * manager from deleting the file.
	 * This function returns {@code null} if the file has any claim. <br>
	 * <br>
	 * This operation can be considered atomic.
	 * That is, the file will not be modified in any way except via this
	 * claim if, and only if, a non-null claim is returned. <br>
	 * <br>
	 * A non-null read-write claim can be used to execute both read and
	 * write operations.
	 *
	 * @param id The id of the file to claim.
	 *
	 * @return A read-write claim for the given file, or {@code null} if none
	 *     could be obtained.
	 */
	ReadWrite requestReadWriteClaim(FileId id);
	
	/**
	 * Releases a read-write claim.
	 * It invalidates the read claim, where it waits on any read or write
	 * actions to terminate.
	 *
	 * @param claim The claim to release.
	 */
	void releaseCacheClaim(ReadWrite claim);

	/**
	 * Releases a read claim.
	 * It invalidates the read claim, where it waits on any read actions
	 * to terminate.
	 *
	 * @param claim The claim to release.
	 */
	void releaseCacheClaim(Read claim);
	
	/**
	 * Atomically degrades a read-write claim to a read claim.
	 * That is, the file will remain untouched by any process
	 * except those already started from the read-write claim.
	 * It invalidates the read-write claim, where it waits on
	 * any write actions to terminate.
	 *
	 * @param readWrite The claim to degrade to a read claim.
	 *
	 * @return A read claim replacing the read-write claim, or
	 *     {@code null} if the file does not exist.
	 */
	Read degradeClaim(ReadWrite readWrite);

	/**
	 * Indexes the existing cache using the {@code idFactory} to generate
	 * the entries.
	 * If the factory returns {@code null}, then that file will not be indexed.
	 *
	 * @param idFactory The factory used to generate the id's for the files.
	 */
	void indexCache(FileIdFactory<? extends FileId> idFactory);
	
}
