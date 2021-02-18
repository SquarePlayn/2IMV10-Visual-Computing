package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy.CachePolicy;

@Getter
public abstract class CacheFileManager<T> {
	protected final CachePolicy<T> policy;
	
	public CacheFileManager(CachePolicy<T> policy) {
		this.policy = policy;
	}
	
	public abstract void indexCache();
	
	public abstract T fileOf(FileId id);

	public abstract T tmpFileOf(FileId id);
	
	public T claimFile(FileId id) {
		T file = fileOf(id);
		policy.remove(file);
		return file;
	}
	
	public void releaseFile(FileId id) {
		policy.update(this, fileOf(id));
	}
	
	public abstract void deleteFile(FileId id);

	public abstract void cacheDeleteFile(T file);
	
	public boolean isClaimed(T file) {
		return policy.isRegistered(file);
	}
	
	public boolean isClaimed(FileId id) {
		return isClaimed(fileOf(id));
	}
	
	public abstract long sizeOf(T file);
	
	public long sizeOf(FileId id) {
		return sizeOf(fileOf(id));
	}
	
	public abstract boolean exists(T file);
	
	public boolean exists(FileId id) {
		return exists(fileOf(id));
	}
	
	
}
