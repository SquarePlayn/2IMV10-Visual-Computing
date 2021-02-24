package nl.tue.visualcomputingproject.group9a.project.common.cache.memory;

import lombok.Getter;
import lombok.NonNull;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileIdFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;

/**
 * An {@link CacheManager} implementation which stores the data in memory.
 * 
 * @param <T> The type of data data being managed.
 */
public class MemoryCacheManager<T extends MemoryCacheObject>
		extends CacheManager<MemoryReadCacheClaim<T>, MemoryReadWriteCacheClaim<T>> {

	/**
	 * Creates a new memory cache manager using the given cache policy.
	 *
	 * @param policy The cache policy to use.
	 */
	public MemoryCacheManager(CachePolicy policy) {
		super(policy);
	}

	@Override
	public void indexCache(FileIdFactory<? extends FileId> idFactory) {
	}

	@Override
	public @NonNull MemoryReadCacheClaim<T> createReadClaim(FileId id, ClaimElem elem) {
		return new MemoryReadCacheClaim<>(id, ((MemoryClaimElem) elem).store);
	}

	@Override
	public @NonNull MemoryReadWriteCacheClaim<T> createReadWriteClaim(FileId id, ClaimElem elem) {
		return new MemoryReadWriteCacheClaim<>(id, ((MemoryClaimElem) elem).store);
	}
	
	@Getter
	protected class MemoryClaimElem
			extends ClaimElem {
		private final MemoryStore<T> store = new MemoryStore<>();
	}
	
	@Override
	protected ClaimElem createClaimElem() {
		return new MemoryClaimElem();
	}
	
}
