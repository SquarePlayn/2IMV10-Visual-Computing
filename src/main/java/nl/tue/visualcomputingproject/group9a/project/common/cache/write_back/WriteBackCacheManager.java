package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.*;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class WriteBackCacheManager<T extends CacheableObject>
		implements CacheManager<WriteBackReadCacheClaim<T>, WriteBackReadWriteCacheClaim<T>> {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** The map containing all claims. */
	protected final Map<FileId, WriteBackClaimElem> claimMap;
	
	/** The policy used for the memory cache. */
	@Getter
	private final CachePolicy memoryPolicy;
	/** The policy used for the file cache. */
	@Getter
	private final CachePolicy filePolicy;
	/** The directory used for caching. */
	@Getter
	private final File cacheDir;
	/** The factory used to create a stream from a file.  */
	@Getter
	private final FileStreamFactory streamFactory;
	/** The serializer used to serialize and deserialize an object. */
	@Getter
	private final ObjectSerializer<T> serializer;


	/**
	 * Class keeping track of all claims of a single file.
	 */
	@Getter
	protected class WriteBackClaimElem {
		/** The set of all read claims. */
		private final Set<WriteBackReadCacheClaim<T>> readClaims = new HashSet<>();
		/** The object stored in memory. */
		private final MemoryStore<T> store = new MemoryStore<>();
		/** The write claim used for the user. */
		private WriteBackReadWriteCacheClaim<T> userWriteClaim = null;
		/** The write claim used by the memory policy. */
		private WriteBackReadWriteMemoryPolicyCacheClaim<T> memoryWriteClaim = null;
		/** The write claim used by the file policy. */
		private WriteBackReadWriteFilePolicyCacheClaim<T> fileWriteClaim = null;
		/** Denotes whether the element is stable. */
		private boolean valid = true;
		
		public void lock() {
			store.lock();
		}
		
		public void unlock() {
			store.unlock();
		}

		/**
		 * @return {@code true} if the object is stored in memory. {@code false} otherwise.
		 */
		public boolean isMemoryTracked() {
			return memoryWriteClaim != null;
		}

		/**
		 * @return {@code true} if the object is stored on disk. {@code false} otherwise.
		 */
		public boolean isFileTracked() {
			return fileWriteClaim != null;
		}

		/**
		 * @return {@code true} if the object is tracked by any policy.
		 */
		public boolean isTracked() {
			return isMemoryTracked() || isFileTracked();
		}

		/**
		 * @return {@code true} if the claim element has a write claim which is held by the user.
		 *     {@code false} otherwise.
		 */
		public boolean hasUserWriteClaim() {
			return userWriteClaim != null;
		}

		/**
		 * @return {@code true} if the claim element has any user claim.
		 */
		public boolean hasUserClaim() {
			return userWriteClaim != null && !readClaims.isEmpty();
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
			String subDir,
			FileStreamFactory streamFactory,
			ObjectSerializer<T> serializer) {
		claimMap = new ConcurrentHashMap<>();
		this.memoryPolicy = memoryPolicy;
		this.filePolicy = filePolicy;
		this.cacheDir = new File(cacheDir, subDir);
		this.streamFactory = streamFactory;
		this.serializer = serializer;
	}
	
	public WriteBackClaimElem getOrPutDefaultAndLock(
			FileId id,
			Function<? super FileId, ? extends WriteBackClaimElem> defFunction) {
		WriteBackClaimElem old = null;
		WriteBackClaimElem elem;
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
	public WriteBackReadCacheClaim<T> requestReadClaim(FileId id) {
		WriteBackClaimElem elem = getOrPutDefaultAndLock(id, null);
		if (elem == null) {
			return null;
		}
		try {
			
			if (elem.hasUserWriteClaim()) {
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
			elem.unlock();
			if (elem.isTracked()) {
				untrack(id, elem);
			}
		}
	}

	@Override
	public WriteBackReadWriteCacheClaim<T> requestReadWriteClaim(FileId id) {
		WriteBackClaimElem elem = getOrPutDefaultAndLock(id, (v) -> new WriteBackClaimElem());
		try {
			if (elem.hasUserClaim()) {
				return null;
			}
			
			return elem.userWriteClaim = new WriteBackReadWriteUserCacheClaim<>(
					id,
					elem.store,
					cacheDir,
					streamFactory,
					serializer);

		} finally {
			elem.unlock();
			if (elem.isTracked()) {
				untrack(id, elem);
			}
		}
	}

	@Override
	public void releaseCacheClaim(WriteBackReadWriteCacheClaim<T> readWrite) {
		invalidateClaim(readWrite);

		final FileId id = readWrite.getId();
		WriteBackClaimElem elem = getOrPutDefaultAndLock(id, null);
		if (elem == null) {
			throw new IllegalStateException("Tried to release unclaimed claim!");
		}
		try {
			if (readWrite instanceof WriteBackReadWriteUserCacheClaim) {
				if (elem.userWriteClaim != readWrite) {
					throw new IllegalArgumentException("Tried to release unclaimed claim!");
				}
				elem.userWriteClaim = null;
				track(id, elem, readWrite.isInMemory(), readWrite.isOnDisk());
				if (!readWrite.isInMemory() && !readWrite.isOnDisk()) {
					elem.valid = false;
					claimMap.remove(id);
				}
				
			} else if (readWrite instanceof WriteBackReadWriteMemoryPolicyCacheClaim) {
				if (elem.memoryWriteClaim != readWrite) {
					throw new IllegalArgumentException("Tried to release unclaimed claim!");
				}
				elem.memoryWriteClaim = null;
				
				if (elem.isFileTracked()) {
					filePolicy.update(id);
					
				} else {
					final File file = new File(cacheDir, id.getPath() + Settings.CACHE_EXT);
					final File tmpFile = new File(file.getPath() + Settings.TMP_CACHE_EXT);
					try {
						try (OutputStream os = streamFactory.write(tmpFile)) {
							serializer.serialize(os, elem.store.get());
						}
						Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				elem.store.set(null);
				
			} else if (readWrite instanceof WriteBackReadWriteFilePolicyCacheClaim) {
				if (elem.fileWriteClaim != readWrite) {
					throw new IllegalArgumentException("Tried to release unclaimed claim!");
				}
				elem.fileWriteClaim = null;
				if (!elem.isMemoryTracked()) {
					elem.valid = false;
					claimMap.remove(id);
				}
			}
			
		} finally {
			elem.unlock();
		}
	}

	@Override
	public void releaseCacheClaim(WriteBackReadCacheClaim<T> claim) {
		invalidateClaim(claim);

		final FileId id = claim.getId();
		WriteBackClaimElem elem = getOrPutDefaultAndLock(id, null);
		if (elem == null) {
			throw new IllegalArgumentException("Tried to release unclaimed claim!");
		}
		try {
			if (!elem.readClaims.remove(claim)) {
				throw new IllegalArgumentException("Tried to release unclaimed claim!");
			}
			
			if (elem.readClaims.isEmpty()) {
				if (claim.exists()) {
					track(id, elem, claim.isInMemory(), claim.isOnDisk());
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
	public WriteBackReadCacheClaim<T> degradeClaim(WriteBackReadWriteCacheClaim<T> claim) {
		invalidateClaim(claim);

		final FileId id = claim.getId();
		WriteBackClaimElem elem = getOrPutDefaultAndLock(id, null);
		if (elem == null) {
			throw new IllegalArgumentException("Tried to release unclaimed claim!");
		}
		try {
			if (elem.userWriteClaim != claim) {
				throw new IllegalArgumentException("The given claim is not valid for this cache manager!");
			}
			elem.userWriteClaim = null;
			if (!claim.exists()) {
				elem.valid = false;
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
			elem.unlock();
		}
	}

	@Override
	public void indexCache(FileIdFactory<? extends FileId> idFactory) {
		try {
			LOGGER.info("Indexing disk cache...");
			if (!cacheDir.exists() && !cacheDir.mkdirs()) {
				LOGGER.error("Could not create cache directory: " + cacheDir);
				LOGGER.error("Continuing without disk cache.");
				return;
			}

			if (!Files.isDirectory(cacheDir.toPath())) {
				LOGGER.error("Disk cache directory is not a directory");
				LOGGER.error("Continuing without disk cache.");
				return;
			}

			String cachePath = cacheDir.getAbsolutePath();
			int rootDirLength = cachePath.length() +
					(cachePath.endsWith(File.separator)
							? 0
							: File.separator.length());
			File[] fileArr = cacheDir.listFiles();
			if (fileArr == null) return;
			List<File> files = new ArrayList<>(Arrays.asList(fileArr));
			for (int i = 0; i < files.size(); i++) {
				File file = files.get(i);
				if (file.isDirectory()) {
					fileArr = file.listFiles();
					if (fileArr != null) {
						files.addAll(Arrays.asList(fileArr));
					}

				} else if (file.getName().endsWith(Settings.CACHE_EXT)) {
					String path = file.getAbsolutePath();
					String name = path.substring(
							rootDirLength,
							path.length() - Settings.CACHE_EXT.length());
					try {
						FileId id = idFactory.fromPath(name);
						if (id == null) continue;
						WriteBackClaimElem elem = getOrPutDefaultAndLock(id, (v) -> new WriteBackClaimElem());
						try {
							track(id, elem, false, true);
							
						} finally {
							elem.unlock();
						}
						LOGGER.info("Added " + name + " to disk cache!");
						
					} catch (Exception e) {
						LOGGER.info("Exception occurred while indexing " + name);
						e.printStackTrace();
					}
					
				} else if (file.getName().endsWith(Settings.TMP_CACHE_EXT)) {
					// Delete old temporary cache files if found.
					//noinspection ResultOfMethodCallIgnored
					file.delete();
				}
			}

			LOGGER.info("Finished indexing disk cache!");

		} catch (Exception e) {
			LOGGER.error("Aborted indexing disk cache!");
			e.printStackTrace();
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
	
	private void track(FileId id, WriteBackClaimElem elem, boolean memory, boolean disk) {
		if (memory) {
			elem.memoryWriteClaim = new WriteBackReadWriteMemoryPolicyCacheClaim<>(id, elem.store);
		}
		if (disk) {
			elem.fileWriteClaim = new WriteBackReadWriteFilePolicyCacheClaim<>(id, cacheDir);
		}

		if (elem.isMemoryTracked()) {
			memoryPolicy.track(id, this, elem.memoryWriteClaim);
		}
		if (elem.isFileTracked()) {
			filePolicy.track(id, this, elem.fileWriteClaim);
		}
	}
	
	/**
	 * Untracks the file with the given id and claim element.
	 *
	 * @param id   The id of the file to untrack.
	 * @param elem The claim element of the file.
	 */
	private void untrack(FileId id, WriteBackClaimElem elem) {
		if (!elem.isTracked()) return;
		
		if (elem.isMemoryTracked() && memoryPolicy.untrack(id)) {
			elem.memoryWriteClaim.invalidate();
			elem.memoryWriteClaim = null;
		}
		
		if (elem.isFileTracked() && filePolicy.untrack(id)) {
			elem.fileWriteClaim.invalidate();
			elem.fileWriteClaim = null;
		}
	}
	
}
