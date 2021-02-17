package nl.tue.visualcomputingproject.group9a.project.common.cache;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CacheFileManager {
	/** The file extension of the cache files. */
	static private final String CACHE_EXT = ".cache";
	static private final String TMP_EXT = ".tmp";
	
	private File cacheDir;

	private final Map<File, Pair<Owner, FileId>> fileMap;
	private final Map<Owner, Set<File>> ownerFileMap;
	
	
	public CacheFileManager(File cacheDir) {
		this.cacheDir = cacheDir;
		fileMap = new ConcurrentHashMap<>();
		ownerFileMap = new ConcurrentHashMap<>();
	}
	
	public <T extends FileId> void registerOwnerResolveUnclaimed(
			Owner owner,
			FileIdFactory<T> idFactory)
			throws IllegalOwnerException {
		return; // TODO
	}
 	
	public void registerOwner(Owner owner)
			throws IllegalOwnerException {
		return; // TODO
	}
	
	public void deregisterOwner(Owner owner)
			throws IllegalOwnerException {
		return; // TODO
	}
	
	public FileId getFileId(File file) {
		return null; // TODO
	}
	
	public Owner getOwner(File file) {
		return null; // TODO
	}
	
	public File fileOf(Owner owner, FileId id) {
		return new File(cacheDir.getPath() + File.separator +
				owner.getPath() + File.separator +
				id.getPath());
	}
	
	public File claimFile(Owner owner, FileId id) {
		return null; // TODO
	}
	
	public File releaseFile(Owner owner, FileId id) {
		return null; // TODO
	}
	
	public boolean deleteFile(Owner owner, FileId id) {
		return false; // TODO
//		return deleteFile(owner, toFile(owner, id));
	}
	
	public boolean deleteFile(Owner owner, File file) {
		return false; // TODO
	}
	
	public boolean isClaimedBy(Owner owner, FileId id) {
		return false;
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
	
	public void clearAllFor(Owner owner) {
		
	}
	
	
}
