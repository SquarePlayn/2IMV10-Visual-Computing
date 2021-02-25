package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import nl.tue.visualcomputingproject.group9a.project.common.cache.*;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;

import java.io.File;

public class WriteBackCacheManager<T extends CacheableObject>
		implements CacheManager<WriteBackReadCacheClaim<T>, WriteBackReadWriteCacheClaim<T>> {
	private final CachePolicy memoryPolicy;
	private final CachePolicy filePolicy;
	private final File cacheDir;
	private final FileStreamFactory streamFactory;
	private final ObjectSerializer<T> serializer;
	
	
	/**
	 * Creates a new write back cache manager using the given cache policy.
	 *
	 * @param memoryPolicy The cache policy to use.
	 */
	public WriteBackCacheManager(
			CachePolicy memoryPolicy,
			CachePolicy filePolicy,
			File cacheDir,
			FileStreamFactory streamFactory,
			ObjectSerializer<T> serializer) {
		this.memoryPolicy = memoryPolicy;
		this.filePolicy = filePolicy;
		this.cacheDir = cacheDir;
		this.streamFactory = streamFactory;
		this.serializer = serializer;
	}

	@Override
	public WriteBackReadCacheClaim<T> requestReadClaim(FileId id) {
		return null;
	}

	@Override
	public WriteBackReadWriteCacheClaim<T> requestReadWriteClaim(FileId id) {
		return null;
	}

	@Override
	public void releaseCacheClaim(WriteBackReadWriteCacheClaim<T> claim) {

	}

	@Override
	public void releaseCacheClaim(WriteBackReadCacheClaim<T> claim) {

	}

	@Override
	public WriteBackReadCacheClaim<T> degradeClaim(WriteBackReadWriteCacheClaim<T> tWriteBackReadWriteCacheClaim) {
		return null;
	}

	@Override
	public void indexCache(FileIdFactory<? extends FileId> idFactory) {
		
	}

//	@Override
//	public @NonNull WriteBackReadCacheClaim<T> createReadClaim(FileId id, ClaimElem elem) {
//		return new WriteBackReadCacheClaim<>(
//				id,
//				((WriteBackClaimElem) elem).store,
//				cacheDir,
//				streamFactory,
//				serializer);
//	}

//	@Override
//	public @NonNull WriteBackReadWriteCacheClaim<T> createReadWriteClaim(FileId id, ClaimElem elem) {
//		MemoryStore<T> store = ((WriteBackClaimElem) elem).store;
//		if (elem.isTracked() && !store.isEmpty()) {
//			return new WriteBackReadWriteMemoryPolicyCacheClaim<>(
//					id,
//					((WriteBackClaimElem) elem).store.get());
//			
//		} else {
//			return new WriteBackReadWriteUserCacheClaim<>(
//					id,
//					((WriteBackClaimElem) elem).store,
//					cacheDir,
//					streamFactory,
//					serializer);
//		}
//	}

	
}
