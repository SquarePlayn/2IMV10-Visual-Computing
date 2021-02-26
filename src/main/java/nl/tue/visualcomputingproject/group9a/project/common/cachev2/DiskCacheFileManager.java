package nl.tue.visualcomputingproject.group9a.project.common.cachev2;

import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy.CachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.*;

@Deprecated
@SuppressWarnings("ResultOfMethodCallIgnored")
public class DiskCacheFileManager
		extends CacheManager<File> {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** The file extension of the cache files. */
	static private final String CACHE_EXT = ".cache";
	static private final String TMP_EXT = ".tmp";

	private final File cacheDir;
	
	public DiskCacheFileManager(CachePolicy policy, File cacheDir) {
		super(policy);
		this.cacheDir = cacheDir;
	}

	@Override
	public void indexCache() {
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

				} else if (file.getName().endsWith(CACHE_EXT)) {
					policy.update(this, file);
					LOGGER.info("Added " + file + " to disk cache!");
				}
			}

			LOGGER.info("Finished indexing disk cache!");

		} catch (Exception e) {
			LOGGER.error("Aborted indexing disk cache!");
			e.printStackTrace();
		}
	}

	@Override
	public File fileOf(FileId id) {
		return new File(cacheDir.getPath() + File.separator +
				id.getPath() +
				CACHE_EXT);
	}

	public File tmpFileOf(FileId id) {
		return new File(cacheDir.getPath() + File.separator +
				"tmp" + File.separator +
				id.getPath() +
				TMP_EXT);
	}
	
	@Override
	public File claimFile(FileId id) {
		File file = super.claimFile(id);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		File tmpFileParent = tmpFileOf(id).getParentFile();
		if (!tmpFileParent.exists()) {
			tmpFileParent.mkdirs();
		}
		return file;
	}

	@Override
	public void deleteFile(FileId id) {
		File file = fileOf(id);
		policy.remove(file);
		file.delete();
	}

	@Override
	public void notifyCacheDelete(File file) {
		file.delete();
	}

	@Override
	public long sizeOf(File file) {
		try {
			return Files.size(file.toPath());
		} catch (IOException e) {
			LOGGER.error("Could not obtain size of " + file + "!");
		}
		return 0;
	}

	@Override
	public boolean exists(File file) {
		return file.exists();
	}
	
}
