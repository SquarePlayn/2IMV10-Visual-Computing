package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A manager for various types of cache files using claims.
 * Any files must be claimed before usage.
 * Unclaimed files can be deleted or modified at any time, depending on the used cache policy.
 * The method {@link #indexCache(FileIdFactory)} must be called to track existing cache files.
 * 
 * @param <Read>      The type of the read-only cache claims.
 * @param <ReadWrite> The type of the read-write cache claims.
 */
public abstract class CacheManager<Read extends ReadCacheClaim, ReadWrite extends ReadWriteCacheClaim> {
	/** The cache policy of this cache manager. */
	@Getter
	private final CachePolicy policy;
	/** Storage of all tracked and externally claimed claims. */
	private final Map<FileId, ClaimElem> claimMap;
	/** The lock of this cache manager. */
	private final Lock lock;

	/**
	 * Creates a new cache manager using the given cache policy.
	 * 
	 * @param policy The cache policy to use.
	 */
	public CacheManager(CachePolicy policy) {
		this.policy = policy;
		claimMap = new HashMap<>();
		lock = new ReentrantLock();
	}

	/**
	 * Class keeping track of all claims of a single file.
	 */
	@Getter
	protected class ClaimElem {
		/** The set of all read claims. */
		private final Set<Read> readClaims = new HashSet<>();
		/** The current write claim. */
		@Setter
		private ReadWrite writeClaim = null;
		/** Whether this claim is tracked in {@link #policy}. */
		@Setter
		private boolean tracked = false;

		/**
		 * @return {@code true} if the claim element has a write claim.
		 *     {@code false} otherwise.
		 */
		public boolean hasWriteClaim() {
			return writeClaim != null;
		}

		/**
		 * @return {@code true} if the claim element has any claim.
		 *     {@code false} otherwise.
		 */
		public boolean hasClaim() {
			return writeClaim != null || !readClaims.isEmpty();
		}
	}

	/**
	 * Untracks the file with the given id and claim element.
	 * Assumes that the lock {@link #lock} is held exactly once.
	 * 
	 * @param id   The id of the file to untrack.
	 * @param elem The claim element of the file.
	 * 
	 * @return {@code true} if the the file was untracked successfully.
	 *     {@code false} if the policy removed the file.
	 */
	private boolean untrackWithLock(FileId id, ClaimElem elem) {
		if (!elem.isTracked()) return true;
		boolean removed;
		lock.unlock();
		try {
			removed = policy.untrack(id);
			if (removed) {
				elem.writeClaim.invalidate();
			}
		} finally {
			lock.lock();
		}

		if (removed) {
			elem.tracked = false;
			elem.writeClaim = null;
			return true;

		} else {
			if (elem.isTracked()) {
				throw new IllegalStateException("Tracked element is not tracked!");
			}
			return false;
		}
	}
	
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
	 *
	 * @see #createReadWriteClaim(FileId, ClaimElem)
	 */
	public Read requestReadClaim(FileId id) {
		lock.lock();
		try {
			ClaimElem elem = claimMap.get(id);
			if (elem == null ||
					(!elem.isTracked() && elem.hasWriteClaim()) ||
					(elem.isTracked() && !untrackWithLock(id, elem)))  {
				return null;
			}
			Read claim = createReadClaim(id, elem);
			elem.readClaims.add(claim);
			return claim;
			
		} finally {
			lock.unlock();
		}
	}

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
	 *
	 * @see #createReadClaim(FileId, ClaimElem)
	 */
	public ReadWrite requestReadWriteClaim(FileId id) {
		lock.lock();
		try {
			ClaimElem elem = claimMap.get(id);
			if (elem != null && elem.hasClaim() && !elem.isTracked()) {
				return null;
			}
			
			if (elem == null) {
				elem = createClaimElem();
				elem.tracked = false;
				elem.writeClaim = createReadWriteClaim(id, elem);
				claimMap.put(id, elem);

			} else {
				if (!untrackWithLock(id, elem)) {
					return null;
				}
				elem.writeClaim = createReadWriteClaim(id, elem);
			}
			return elem.writeClaim;

		} finally {
			lock.unlock();
		}
	}

	/**
	 * Releases a read-write claim.
	 * It invalidates the read claim, where it waits on any read or write
	 * actions to terminate.
	 *
	 * @param claim The claim to release.
	 */
	public void releaseCacheClaim(ReadWrite claim) {
		if (!claim.isValid()) {
			throw new IllegalArgumentException("Cannot release an invalidated claim!");
		}
		claim.invalidate();
		
		ReadWrite trackClaim = null;
		final FileId id = claim.getId();
		lock.lock();
		try {
			ClaimElem elem = claimMap.get(id);
			if (elem == null || elem.writeClaim != claim) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			
			if (claim.exists()) {
				trackClaim = createReadWriteClaim(id, elem);
			} else {
				elem.writeClaim = null;
				claimMap.remove(id);
			}
			
		} finally {
			lock.unlock();
		}
		
		if (trackClaim != null) {
			policy.track(id, this, trackClaim);
		}
	}

	/**
	 * Releases a read claim.
	 * It invalidates the read claim, where it waits on any read actions
	 * to terminate.
	 * 
	 * @param claim The claim to release.
	 */
	public void releaseCacheClaim(Read claim) {
		if (claim instanceof ReadWriteCacheClaim) {
			//noinspection unchecked
			releaseCacheClaim((ReadWrite) claim);
		}

		if (!claim.isValid()) {
			throw new IllegalArgumentException("Cannot release an invalidated claim!");
		}
		claim.invalidate();
		
		ReadWrite trackClaim = null;
		final FileId id = claim.getId();
		lock.lock();
		try {
			ClaimElem elem = claimMap.get(id);
			if (elem == null || !elem.readClaims.remove(claim)) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			
			if (claim.exists()) {
				if (!elem.hasClaim()) {
					trackClaim = createReadWriteClaim(id, elem);
				}
			} else {
				claimMap.remove(id);
			}
			
		} finally {
			lock.unlock();
		}
		
		if (trackClaim != null) {
			policy.track(id, this, trackClaim);
		}
	}

	/**
	 * Adds the file with the given ID to the cache policy.
	 * Only tracks files if they exist.
	 * 
	 * @param id The ID of the file to track.
	 */
	protected void track(FileId id) {
		ClaimElem elem;
		lock.lock();
		try {
			elem = claimMap.get(id);
			if (elem != null && elem.hasClaim()) {
				if (elem.isTracked()) return;
				throw new IllegalArgumentException("Tried to track claim which is already claimed!");
			}
			
			boolean newClaim = (elem == null);
			if (newClaim) {
				elem = createClaimElem();
				elem.tracked = true;
			}
			ReadWrite claim = createReadWriteClaim(id, elem);
			if (!claim.exists()) {
				throw new IllegalArgumentException("Tried to track claim which doesn't exist!");
			}
			elem.writeClaim = claim;
			if (newClaim) {
				claimMap.put(id, elem);
			}
			
		} finally {
			lock.unlock();
		}
		
		policy.track(id, this, elem.writeClaim);
	}

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
	public Read degradeClaim(ReadWrite readWrite) {
		if (!readWrite.isValid()) {
			throw new IllegalArgumentException("Cannot release an invalidated claim!");
		}
		readWrite.invalidate();
		
		lock.lock();
		try {
			FileId id = readWrite.getId();
			ClaimElem elem = claimMap.get(id);
			if (elem == null || elem.writeClaim != readWrite) {
				throw new IllegalArgumentException("The given claim is not valid for this cache manager!");
			}
			elem.writeClaim = null;
			
			if (!readWrite.exists()) {
				return null;
			}
			Read read = createReadClaim(id, elem);
			elem.readClaims.add(read);
			return read;
			
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @return A new {@link ClaimElem} storing future claims.
	 */
	protected ClaimElem createClaimElem() {
		return new ClaimElem();
	}

	/**
	 * Indexes the existing cache using the {@code idFactory} to generate
	 * the entries.
	 * If the factory returns {@code null}, then that file will not be indexed.
	 * 
	 * @param idFactory The factory used to generate the id's for the files.
	 */
	public abstract void indexCache(FileIdFactory<? extends FileId> idFactory);

	/**
	 * Creates a new read claim for the given file ID.
	 * The claim will only be added to the given {@link ClaimElem},
	 * but there is no guarantee whether or when it will be added.
	 *
	 * @param id The file ID to create a new read write claim for.
	 *
	 * @return A non-null read claim for the given file ID.
	 */
	public abstract @NonNull Read createReadClaim(FileId id, ClaimElem elem);

	/**
	 * Creates a new read-write claim for the given file ID.
	 * The claim will only be added to the given {@link ClaimElem},
	 * but there is no guarantee whether or when it will be added.
	 * 
	 * @param id The file ID to create a new read write claim for.
	 * 
	 * @return A non-null read-write claim for the given file ID.
	 */
	public abstract @NonNull ReadWrite createReadWriteClaim(FileId id, ClaimElem elem);
	
}
