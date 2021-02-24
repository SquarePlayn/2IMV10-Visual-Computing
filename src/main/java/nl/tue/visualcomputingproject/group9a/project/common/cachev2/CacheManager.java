package nl.tue.visualcomputingproject.group9a.project.common.cachev2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy.CachePolicy;

@Getter
@AllArgsConstructor
public abstract class CacheManager<T> {
	protected final CachePolicy policy;
	
	public abstract void indexCache();
	
	public abstract T fileOf(FileId id);
	
	public T claimFile(FileId id) {
		T file = fileOf(id);
		policy.remove(file);
		return file;
	}
	
	public void releaseFile(FileId id) {
		T file = fileOf(id);
		policy.update(this, file);
	}
	
	public abstract void deleteFile(FileId id);

	public abstract void notifyCacheDelete(T file);
	
	public boolean isClaimed(T file) {
		return !policy.isRegistered(file);
	}
	
	public boolean isClaimed(FileId id) {
		return !policy.isRegistered(fileOf(id));
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
