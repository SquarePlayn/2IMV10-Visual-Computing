package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CacheFileManager {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** The file extension of the cache files. */
	static private final String CACHE_EXT = ".cache";
	static private final String TMP_EXT = ".tmp";

	private final CachePolicy policy;
	private final Map<File, FileData> fileDataMap;
	private final Map<Owner, Set<File>> ownerFileMap;
	
	private File cacheDir;
	
	@Value
	private static class FileData {
		Owner owner;
		FileId id;
	}
	
	public CacheFileManager(CachePolicy policy, File cacheDir) {
		this.policy = policy;
		this.cacheDir = cacheDir;
		fileDataMap = new ConcurrentHashMap<>();
		ownerFileMap = new ConcurrentHashMap<>();
	}
	
	public <T extends FileId> void registerOwnerResolveUncached(
			Owner owner,
			FileIdFactory<T> idFactory)
			throws IllegalOwnerException {
		try {
			LOGGER.info("Indexing disk cache for " + owner + "...");
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

			Map<File, FileData> addFileData = new HashMap<>();
			Set<File> addFiles = ConcurrentHashMap.newKeySet();
			File srcFile = dirOf(owner);
	
			int rootDirLength = srcFile.getAbsolutePath().length() + 1;
			File[] fileArr = srcFile.listFiles();
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
					String path = file.getAbsolutePath();
					String name = path.substring(
							rootDirLength,
							path.length() - CACHE_EXT.length());
					try {
						FileData data = new FileData(owner, idFactory.fromPath(name));
						addFileData.put(file, data);
						addFiles.add(file);
						LOGGER.info("Added " + name + " to disk cache!");
	
					} catch (IllegalArgumentException e) {
						LOGGER.error("Could not process " + path + "! Skipping file.");
						e.printStackTrace();
					}
				}
			}
			
			fileDataMap.putAll(addFileData);
			ownerFileMap.put(owner, addFiles);
			for (File file : addFileData.keySet()) {
				policy.update(this, file);
			}
			
			LOGGER.info("Finished indexing disk cache for " + owner + "!");
	
		} catch (Exception e) {
			LOGGER.error("Aborted indexing disk cache for " + owner + "!");
			e.printStackTrace();
		}
	}
 	
	public void registerOwner(Owner owner)
			throws IllegalOwnerException {
		ownerFileMap.put(owner, ConcurrentHashMap.newKeySet());
	}
	
	public void deregisterOwner(Owner owner)
			throws IllegalOwnerException {
		Set<File> files = ownerFileMap.remove(owner);
		for (File f : files) {
			fileDataMap.remove(f);
		}
	}
	
	public FileId getFileId(File file) {
		FileData data = fileDataMap.get(file);
		if (data == null) return null;
		return data.getId();
	}
	
	public Owner getOwner(File file) {
		FileData data = fileDataMap.get(file);
		if (data == null) return null;
		return data.getOwner();
	}
	
	public File fileOf(Owner owner, FileId id) {
		return new File(cacheDir.getPath() + File.separator +
				owner.getPath() + File.separator +
				id.getPath());
	}

	public File dirOf(Owner owner) {
		return new File(cacheDir.getPath() + File.separator +
				owner.getPath());
	}

	public File tmpFileOf(Owner owner, FileId id) {
		return new File(cacheDir.getPath() + File.separator +
				"tmp" + File.separator +
				owner.getPath() + File.separator +
				id.getPath());
	}

	public File tmpDirOf(Owner owner) {
		return new File(cacheDir.getPath() + File.separator +
				"tmp" + File.separator +
				owner.getPath());
	}
	
	public File claimFile(Owner owner, FileId id) {
		File file = fileOf(owner, id);
		policy.remove(file);
		Set<File> ownerFiles = ownerFileMap.get(owner);
		if (ownerFiles == null) {
			throw new IllegalOwnerException("The owner " + owner + " does not exist!");
		}
		ownerFiles.add(file);
		fileDataMap.put(file, new FileData(owner, id));
		return file;
	}
	
	public void releaseFile(Owner owner, FileId id) {
		policy.update(this, fileOf(owner, id));
	}
	
	public boolean deleteFile(Owner owner, FileId id) {
		return deleteFile(owner, fileOf(owner, id));
	}
	
	public boolean deleteFile(Owner owner, File file) {
		Set<File> ownerFiles = ownerFileMap.get(owner);
		if (ownerFiles.isEmpty()) {
			throw new IllegalOwnerException("The owner " + owner + " does not exist!");
		}
		ownerFiles.remove(file);
		fileDataMap.remove(file);
		policy.remove(file);
		return file.delete();
	}
	
	public boolean isClaimed(Owner owner, FileId id) {
		return policy.isRegistered(fileOf(owner, id));
	}
	
	public Collection<File> getUnClaimedFiles(Owner owner) {
		return null; // TODO
	}
	
	public Collection<File> getClaimedFiles(Owner owner) {
		Set<File> files = ownerFileMap.get(owner);
		if (files == null) {
			throw new IllegalOwnerException("The owner is not registered");
		}
		return files;
	}
	
	public void cacheDeleteFile(File file) {
		FileData data = fileDataMap.remove(file);
		Set<File> files = ownerFileMap.get(data.getOwner());
		if (files == null) {
			return;
		}
		files.remove(file);
	}
	
	
}
