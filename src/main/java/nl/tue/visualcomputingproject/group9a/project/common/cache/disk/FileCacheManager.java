package nl.tue.visualcomputingproject.group9a.project.common.cache.disk;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.*;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link CacheManager} implementation which stores the data on disk.
 */
public class FileCacheManager
		extends SimpleCacheManager<FileReadCacheClaim, FileReadWriteCacheClaim> {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/** The caching directory of this cache manager. */
	private final File cacheDir;
	
	/**
	 * Creates a new file cache manager with the given policy and directory.
	 * 
	 * @param policy   The cache policy used for deleting files.
	 * @param cacheDir The cache directory to store the files at.
	 */
	public FileCacheManager(CachePolicy policy, File cacheDir) {
		super(policy);
		this.cacheDir = cacheDir;
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
						if (id != null) {
							track(id);
							LOGGER.info("Added " + name + " to disk cache!");
						}
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

	@Override
	public FileReadCacheClaim createReadClaim(FileId id, ClaimElem elem) {
		return new FileReadCacheClaim(id, cacheDir);
	}

	@Override
	public FileReadWriteCacheClaim createReadWriteClaim(FileId id, ClaimElem elem) {
		return new FileReadWriteCacheClaim(id, cacheDir);
	}
	
}
