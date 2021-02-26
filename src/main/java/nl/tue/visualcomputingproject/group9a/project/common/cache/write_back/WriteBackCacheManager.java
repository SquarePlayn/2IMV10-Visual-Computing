package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.*;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WriteBackCacheManager<T extends CacheableObject>
		implements CacheManager<WriteBackReadCacheClaim<T>, WriteBackReadWriteCacheClaim<T>> {
	private final Lock lock = new ReentrantLock();

	protected final Map<FileId, WriteBackClaimElem> claimMap;
	
	@Getter
	private final CachePolicy memoryPolicy;
	@Getter
	private final CachePolicy filePolicy;
	@Getter
	private final File cacheDir;
	@Getter
	private final FileStreamFactory streamFactory;
	@Getter
	private final ObjectSerializer<T> serializer;


	/**
	 * Class keeping track of all claims of a single file.
	 */
	@Getter
	protected class WriteBackClaimElem {
		/** The set of all read claims. */
		private final Set<WriteBackReadCacheClaim<T>> readClaims = new HashSet<>();
		private final MemoryStore<T> store = new MemoryStore<>();
		/** The current write claim. */
		private WriteBackReadWriteCacheClaim<T> userWriteClaim = null;
		private WriteBackReadWriteMemoryPolicyCacheClaim<T> memoryWriteClaim = null;
		private WriteBackReadWriteFilePolicyCacheClaim<T> fileWriteClaim = null;
		private int stable = 0;
		
		public boolean isMemoryTracked() {
			return memoryWriteClaim != null;
		}
		
		public boolean isFileTracked() {
			return fileWriteClaim != null;
		}
		
		public boolean isTracked() {
			return isMemoryTracked() || isFileTracked();
		}

		/**
		 * @return {@code true} if the claim element has a write claim.
		 *     {@code false} otherwise.
		 */
		public boolean hasUserWriteClaim() {
			return userWriteClaim != null;
		}
		
		public boolean hasUserClaim() {
			return userWriteClaim != null && !readClaims.isEmpty();
		}
		
		public boolean isStable() {
			return stable == 0;
		}
		
	}
	
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
		claimMap = new HashMap<>();
		this.memoryPolicy = memoryPolicy;
		this.filePolicy = filePolicy;
		this.cacheDir = cacheDir;
		this.streamFactory = streamFactory;
		this.serializer = serializer;
	}

	@Override
	public WriteBackReadCacheClaim<T> requestReadClaim(FileId id) {
		lock.lock();
		try {
			WriteBackClaimElem elem = claimMap.get(id);
			if (elem == null ||
					!elem.isStable() ||
					(elem.isTracked() && !untrackWithLock(id, elem)) ||
					elem.hasUserWriteClaim()) {
				return null;
			}
			
			WriteBackReadCacheClaim<T> read = new WriteBackReadCacheClaim<>(
					id,
					elem.store,
					cacheDir,
					streamFactory,
					serializer);
			elem.readClaims.add(read);
			return read;
			
		} finally {
			lock.unlock();
		}
	}

	@Override
	public WriteBackReadWriteCacheClaim<T> requestReadWriteClaim(FileId id) {
		lock.lock();
		try {
			WriteBackClaimElem elem = claimMap.get(id);
			if (elem != null &&
					(!elem.isStable() ||
					(elem.isTracked() && !untrackWithLock(id, elem)) ||
							elem.hasUserClaim())) {
				return null;
			}
			
			if (elem == null) {
				elem = new WriteBackClaimElem();
				claimMap.put(id, elem);
			}
			
			return elem.userWriteClaim = new WriteBackReadWriteUserCacheClaim<>(
					id,
					elem.store,
					cacheDir,
					streamFactory,
					serializer);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public void releaseCacheClaim(WriteBackReadWriteCacheClaim<T> readWrite) {
		invalidateClaim(readWrite);

		final FileId id = readWrite.getId();
		WriteBackReadWriteMemoryPolicyCacheClaim<T> memoryClaim = null;
		WriteBackReadWriteFilePolicyCacheClaim<T> fileClaim = null;
		boolean updateFile = false;
		T toDisk = null;

		WriteBackClaimElem elem;
		lock.lock();
		try {
			boolean trackMemory = false;
			boolean trackFile = false;
			elem = claimMap.get(id);
			if (elem == null) {
				throw new IllegalStateException("Tried to release unclaimed claim!");
			}
			elem.stable++;
			
			if (readWrite instanceof WriteBackReadWriteUserCacheClaim) {
				WriteBackReadWriteUserCacheClaim<T> claim =
						(WriteBackReadWriteUserCacheClaim<T>) readWrite;
				if (elem.userWriteClaim != claim) {
					throw new IllegalArgumentException("Tried to release unclaimed claim!");
				}
				elem.userWriteClaim = null;
				trackMemory = claim.isInMemory();
				trackFile = claim.isOnDisk();
				if (!trackMemory && !trackFile) {
					claimMap.remove(id);
				}
				
			} else if (readWrite instanceof WriteBackReadWriteMemoryPolicyCacheClaim) {
				WriteBackReadWriteMemoryPolicyCacheClaim<T> claim =
						(WriteBackReadWriteMemoryPolicyCacheClaim<T>) readWrite;
				if (elem.memoryWriteClaim != claim) {
					throw new IllegalArgumentException("Tried to release unclaimed claim!");
				}
				elem.memoryWriteClaim = null;
				updateFile = elem.isStable() && elem.isFileTracked();
				trackFile = !elem.isFileTracked();
				toDisk = elem.store.get();
				elem.store.set(null);
				
			} else if (readWrite instanceof WriteBackReadWriteFilePolicyCacheClaim) {
				WriteBackReadWriteFilePolicyCacheClaim<T> claim =
						(WriteBackReadWriteFilePolicyCacheClaim<T>) readWrite;
				if (elem.fileWriteClaim != claim) {
					throw new IllegalArgumentException("Tried to release unclaimed claim!");
				}
				elem.fileWriteClaim = null;
				if (!elem.isMemoryTracked()) {
					claimMap.remove(id);
				}

			} else {
				throw new IllegalArgumentException("Attempted to release claim of unknown type: " +
						readWrite.getClass());
			}
			
			if (trackMemory) {
				elem.memoryWriteClaim = memoryClaim =
						new WriteBackReadWriteMemoryPolicyCacheClaim<>(id, elem.store);
			}
			if (trackFile) {
				elem.fileWriteClaim = fileClaim =
						new WriteBackReadWriteFilePolicyCacheClaim<>(id, cacheDir);
			}

		} finally {
			lock.unlock();
		}
		
		try {
			if (memoryClaim != null) {
				// Add store to tracker.
				memoryPolicy.track(id, this, memoryClaim);
			}

			if ((updateFile && !filePolicy.update(id) && toDisk != null) ||
					(!updateFile && toDisk != null)) {
				if (fileClaim == null) {
					// Only happens when the file was deleted before it could be updated.
					lock.lock();
					try {
						if (elem.isFileTracked()) {
							// This should never occur.
							// This case only happens when the following events are executed
							// in exactly this order:
							// - [Thread A] The memory tracker deletes a file, and it arrives at
							//   the current outer if-statement.
							// - [Thread B] The file tracker deletes the file and completes this function.
							// - [Thread B or C] The memory tracker somehow deletes the file again
							//   (breach of contract). It completes this function.
							// - [Thread A] We resume execution.
							throw new IllegalStateException("Wonk");
						}
						elem.fileWriteClaim = fileClaim =
								new WriteBackReadWriteFilePolicyCacheClaim<>(id, cacheDir);
						
					} finally {
						lock.unlock();
					}
				}

				// Write to temporary file.
				final File file = new File(cacheDir, id.getPath() + Settings.CACHE_EXT);
				final File tmpFile = new File(file.getPath() + Settings.TMP_CACHE_EXT);
				try (OutputStream os = streamFactory.write(tmpFile)) {
					serializer.serialize(os, toDisk);

				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				// Move from temporary file to target.
				try {
					Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

				} catch (IOException e) {
					e.printStackTrace();
					//noinspection ResultOfMethodCallIgnored
					tmpFile.delete();
					return;
				}
			}

			if (fileClaim != null) {
				// Add file to tracker.
				filePolicy.track(id, this, fileClaim);
			}
			
		} finally {
			lock.lock();
			try {
				elem.stable--;
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public void releaseCacheClaim(WriteBackReadCacheClaim<T> claim) {
		invalidateClaim(claim);

		final FileId id = claim.getId();
		WriteBackReadWriteMemoryPolicyCacheClaim<T> trackMemory = null;
		WriteBackReadWriteFilePolicyCacheClaim<T> trackFile = null;
		WriteBackClaimElem elem;
		lock.lock();
		try {
			elem = claimMap.get(id);
			if (elem == null || !elem.readClaims.remove(claim)) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			elem.stable++;
			
			if (claim.isInMemory()) {
				 elem.memoryWriteClaim = trackMemory =
						 new WriteBackReadWriteMemoryPolicyCacheClaim<>(id, elem.store);
			}
			if (claim.isOnDisk()) {
				elem.fileWriteClaim = trackFile =
						new WriteBackReadWriteFilePolicyCacheClaim<>(id, cacheDir);
			}
			
		} finally {
			lock.unlock();
		}
		
		try {
			if (elem.memoryWriteClaim != null) {
				memoryPolicy.track(id, this, trackMemory);
			}

			if (trackFile != null) {
				filePolicy.track(id, this, trackFile);
			}
			
		} finally {
			lock.lock();
			try {
				elem.stable--;
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public WriteBackReadCacheClaim<T> degradeClaim(WriteBackReadWriteCacheClaim<T> claim) {
		invalidateClaim(claim);
		
		lock.lock();
		try {
			final FileId id = claim.getId();
			WriteBackClaimElem elem = claimMap.get(id);
			if (elem == null || elem.userWriteClaim != claim) {
				throw new IllegalArgumentException("The given claim is not valid for this cache manager!");
			}
			elem.userWriteClaim = null;
			if (!claim.exists()) {
				claimMap.remove(id);
				return null;
			}
			WriteBackReadCacheClaim<T> read = new WriteBackReadWriteUserCacheClaim<>(
					id,
					elem.store,
					cacheDir,
					streamFactory,
					serializer);
			elem.readClaims.add(read);
			return read;

		} finally {
			lock.unlock();
		}
	}

	@Override
	public void indexCache(FileIdFactory<? extends FileId> idFactory) {
		// TODO
	}
	
	private void invalidateClaim(ReadCacheClaim claim) {
		if (claim == null) {
			throw new NullPointerException("Cannot release a null claim.");
		}
		if (!claim.isValid() || claim.invalidate()) {
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
	private boolean untrackWithLock(FileId id, WriteBackClaimElem elem) {
		if (!elem.isStable()) return false;
		if (!elem.isTracked()) return true;
		
		boolean isMemoryTracked = elem.isMemoryTracked();
		boolean isFileTracked = elem.isFileTracked();
		boolean untrackedMemory = false;
		boolean untrackedFile = false;
		elem.stable++;
		lock.unlock();
		try {
			if (elem.isMemoryTracked() && (untrackedMemory = memoryPolicy.untrack(id))) {
				elem.memoryWriteClaim.invalidate();
			}
			if (elem.isFileTracked() && (untrackedFile = filePolicy.untrack(id))) {
				elem.fileWriteClaim.invalidate();
			}
		} finally {
			lock.lock();

			if (untrackedMemory) {
				elem.memoryWriteClaim = null;
			}
			if (untrackedFile) {
				elem.fileWriteClaim = null;
			}
			elem.stable--;
		}
		
		if (isMemoryTracked != untrackedMemory && !elem.isMemoryTracked()) {
			throw new IllegalStateException("Tracked element is not tracked!");
		}
		if (isFileTracked != untrackedFile && !elem.isFileTracked()) {
			throw new IllegalStateException("Tracked element is not tracked!");
		}
		return untrackedMemory || untrackedFile;
	}
	
}
