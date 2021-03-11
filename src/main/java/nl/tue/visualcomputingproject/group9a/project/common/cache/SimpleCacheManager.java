package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * A manager for various types of cache files using claims.
 * Any files must be claimed before usage.
 * Unclaimed files can be deleted or modified at any time, depending on the used cache policy.
 * The method {@link #indexCache(FileIdFactory)} must be called to track existing cache files.
 * 
 * @param <Read>      The type of the read-only cache claims.
 * @param <ReadWrite> The type of the read-write cache claims.
 */
public abstract class SimpleCacheManager<Read extends ReadCacheClaim, ReadWrite extends ReadWriteCacheClaim>
		implements CacheManager<Read, ReadWrite> {
	/** The cache policy of this cache manager. */
	@Getter
	protected final CachePolicy policy;
	/** Storage of all tracked and externally claimed claims. */
	protected final Map<FileId, ClaimElem> claimMap;
//	/** The lock of this cache manager. */
//	protected final Lock lock;

	/**
	 * Creates a new cache manager using the given cache policy.
	 * 
	 * @param policy The cache policy to use.
	 */
	public SimpleCacheManager(CachePolicy policy) {
		this.policy = policy;
		claimMap = new ConcurrentHashMap<>();
//		claimMap = new HashMap<>();
//		lock = new ReentrantLock();
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
		/** The lock used for this element. */
		private Lock lock = new ReentrantLock();
		/** Whether this element is valid. */
		@Setter
		private boolean valid = true;
		
		public void lock() {
			lock.lock();
		}
		
		public void unlock() {
			lock.unlock();
		}

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
	
	public ClaimElem getOrPutDefaultAndLockClaimElem(
			FileId id,
			Function<? super FileId, ? extends ClaimElem> defFunction) {
		ClaimElem old = null;
		ClaimElem elem;
		while (true) {
			// Fetch claim element from the claim map.
			if (defFunction == null) {
				elem = claimMap.get(id);
			} else {
				elem = claimMap.computeIfAbsent(id, defFunction);
			}

			// If no claim element exists, return {@code null}.
			if (elem == null) {
				return null;
			}
			
			// Check whether it is still valid and return it if so.
			elem.lock();
			if (elem.isValid()) {
				return elem;
			} else {
				// If it is invalid, fetch again.
				if (old == elem) {
					throw new IllegalStateException("Invalid claim in map!");
				}
				old = elem;
			}
		}
	}
	
	
	@Override
	public Read requestReadClaim(FileId id) {
		ClaimElem elem = getOrPutDefaultAndLockClaimElem(id, null);
		if (elem == null) {
			return null;
		}
		try {
			if ((!elem.isTracked() && elem.hasWriteClaim()) ||
					(elem.isTracked() && !untrack(id, elem))) {
				return null;
			}
			
			Read claim = createReadClaim(id, elem);
			elem.readClaims.add(claim);
			return claim;
			
		} finally {
			elem.unlock();
		}
	}

	@Override
	public ReadWrite requestReadWriteClaim(FileId id) {
		ClaimElem elem = getOrPutDefaultAndLockClaimElem(id, (v) -> createClaimElem());
		try {
			if ((!elem.isTracked() && elem.hasClaim()) ||
					(elem.isTracked() && !untrack(id, elem))) {
				return null;
			}
			return elem.writeClaim = createReadWriteClaim(id, elem);
			
		} finally {
			elem.unlock();
		}
	}

	@Override
	public void releaseCacheClaim(ReadWrite claim) {
		invalidateClaim(claim);
		
		final FileId id = claim.getId();
		ClaimElem elem = getOrPutDefaultAndLockClaimElem(id, null);
		if (elem == null) {
			throw new IllegalArgumentException("Tried to release unclaimed claim!");
		}
		try {
			if (elem.writeClaim != claim) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			
			if (elem.isTracked() || !claim.exists()) {
				elem.valid = false;
				elem.writeClaim = null;
				elem.tracked = false;
				claimMap.remove(id);
			} else {
				elem.tracked = true;
				policy.track(id, this, createReadWriteClaim(id, elem));
			}
			
		} finally {
			elem.unlock();
		}
	}

	@Override
	public void releaseCacheClaim(Read claim) {
		invalidateClaim(claim);

		final FileId id = claim.getId();
		ClaimElem elem = getOrPutDefaultAndLockClaimElem(id, null);
		if (elem == null) {
			throw new IllegalArgumentException("Tried to release unclaimed claim!");
		}
		try {
			if (!elem.readClaims.remove(claim)) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			
			if (!elem.hasClaim()) {
				if (claim.exists()) {
					elem.tracked = true;
					elem.writeClaim = createReadWriteClaim(id, elem);
					policy.track(id, this, elem.writeClaim);
					
				} else {
					elem.valid = false;
					claimMap.remove(id);
				}
			}
			
		} finally {
			elem.unlock();
		}
	}

	@Override
	public Read degradeClaim(ReadWrite readWrite) {
		invalidateClaim(readWrite);
		
		final FileId id = readWrite.getId();
		ClaimElem elem = getOrPutDefaultAndLockClaimElem(id, null);
		if (elem == null) {
			throw new IllegalArgumentException("Tried to release unclaimed claim!");
		}
		try {
			if (elem.writeClaim != readWrite) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			elem.writeClaim = null;
			
			if (!readWrite.exists()) {
				elem.valid = false;
				claimMap.remove(id);
				return null;
			}
			Read read = createReadClaim(id, elem);
			elem.readClaims.add(read);
			return read;
			
		} finally {
			elem.unlock();
		}
	}
	
	private void invalidateClaim(ReadCacheClaim claim) {
		if (claim == null) {
			throw new NullPointerException("Cannot release a null claim.");
		}
		if (!claim.isValid() || !claim.invalidate()) {
			throw new IllegalArgumentException("Cannot release a null or invalidated claim: " + claim);
		}
	}

	/**
	 * Untracks the file with the given id and claim element.
	 *
	 * @param id   The id of the file to untrack.
	 * @param elem The claim element of the file.
	 *
	 * @return {@code true} if the the file was untracked successfully.
	 *     {@code false} if the policy removed the file.
	 */
	private boolean untrack(FileId id, ClaimElem elem) {
		if (!elem.isTracked()) return true;
		
		if (policy.untrack(id)) {
			try {
				if (!elem.writeClaim.invalidate()) {
					throw new IllegalStateException("Tracking claim was already invalid!");
				}
				
			} finally {
				elem.writeClaim = null;
				elem.tracked = false;
			}
			
		} else {
			throw new IllegalStateException("Tracked element is not tracked!"); // TODO: return false
		}
		return true;
	}

	/**
	 * Adds the file with the given ID to the cache policy.
	 * Only tracks files if they exist.
	 *
	 * @param id The ID of the file to track.
	 */
	protected void track(FileId id) {
		ClaimElem elem = getOrPutDefaultAndLockClaimElem(id, (v) -> createClaimElem());
		try {
			if (elem.isTracked()) {
				return;
			} else if (elem.hasClaim()) {
				throw new IllegalArgumentException("Tried to track claim which is already claimed!");
			}
			elem.tracked = true;
			
			elem.writeClaim = createReadWriteClaim(id, elem);
			policy.track(id, this, elem.writeClaim);

		} finally {
			elem.unlock();
		}
	}

	/**
	 * @return A new {@link ClaimElem} storing future claims.
	 */
	protected ClaimElem createClaimElem() {
		return new ClaimElem();
	}

	/**
	 * Creates a new read claim for the given file ID.
	 * The claim will only be added to the given {@link ClaimElem},
	 * but there is no guarantee whether or when it will be added.
	 *
	 * @param id The file ID to create a new read write claim for.
	 *
	 * @return A non-null read claim for the given file ID.
	 */
	protected abstract @NonNull Read createReadClaim(FileId id, ClaimElem elem);

	/**
	 * Creates a new read-write claim for the given file ID.
	 * The claim will only be added to the given {@link ClaimElem},
	 * but there is no guarantee whether or when it will be added.
	 * 
	 * @param id The file ID to create a new read write claim for.
	 * 
	 * @return A non-null read-write claim for the given file ID.
	 */
	protected abstract @NonNull ReadWrite createReadWriteClaim(FileId id, ClaimElem elem);
	
}
