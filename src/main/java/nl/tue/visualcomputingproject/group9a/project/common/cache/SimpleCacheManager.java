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
public abstract class SimpleCacheManager<Read extends ReadCacheClaim, ReadWrite extends ReadWriteCacheClaim>
		implements CacheManager<Read, ReadWrite> {
	/** The cache policy of this cache manager. */
	@Getter
	protected final CachePolicy policy;
	/** Storage of all tracked and externally claimed claims. */
	protected final Map<FileId, ClaimElem> claimMap;
	/** The lock of this cache manager. */
	protected final Lock lock;

	/**
	 * Creates a new cache manager using the given cache policy.
	 * 
	 * @param policy The cache policy to use.
	 */
	public SimpleCacheManager(CachePolicy policy) {
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
	
	@Override
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

	@Override
	public ReadWrite requestReadWriteClaim(FileId id) {
		lock.lock();
		try {
			ClaimElem elem = claimMap.get(id);
			if (elem != null &&
					((!elem.isTracked() && elem.hasClaim()) ||
							elem.isTracked() && !untrackWithLock(id, elem))) {
				return null;
			}
			
			if (elem == null) {
				elem = createClaimElem();
				elem.tracked = false;
				claimMap.put(id, elem);
			}
			
			return elem.writeClaim = createReadWriteClaim(id, elem);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public void releaseCacheClaim(ReadWrite claim) {
		invalidateClaim(claim);
		
		ReadWrite trackClaim = null;
		final FileId id = claim.getId();
		lock.lock();
		try {
			ClaimElem elem = claimMap.get(id);
			if (elem == null || elem.writeClaim != claim) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			
			if (claim.exists()) {
				elem.tracked = true;
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

	@Override
	public void releaseCacheClaim(Read claim) {
		invalidateClaim(claim);

		final FileId id = claim.getId();
		ReadWrite trackClaim = null;
		lock.lock();
		try {
			ClaimElem elem = claimMap.get(id);
			if (elem == null || !elem.readClaims.remove(claim)) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			
			if (claim.exists()) {
				if (!elem.hasClaim()) {
					elem.tracked = true;
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

	@Override
	public Read degradeClaim(ReadWrite readWrite) {
		invalidateClaim(readWrite);
		
		lock.lock();
		try {
			final FileId id = readWrite.getId();
			ClaimElem elem = claimMap.get(id);
			if (elem == null || elem.writeClaim != readWrite) {
				throw new IllegalArgumentException("The given claim is not valid for this cache manager!");
			}
			elem.writeClaim = null;
			
			if (!readWrite.exists()) {
				claimMap.remove(id);
				return null;
			}
			Read read = createReadClaim(id, elem);
			elem.readClaims.add(read);
			return read;
			
		} finally {
			lock.unlock();
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
			if ((removed = policy.untrack(id)) && !elem.writeClaim.invalidate()) {
				throw new IllegalStateException("Tracking claim was already invalid!");
			}
		} finally {
			lock.lock();
		}
		
		if (removed) {
			elem.writeClaim = null;
			elem.tracked = false;
		}

		if (!removed && elem.isTracked()) {
			throw new IllegalStateException("Tracked element is not tracked!");
		}
		return removed;
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
			}
			elem.tracked = true;
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
