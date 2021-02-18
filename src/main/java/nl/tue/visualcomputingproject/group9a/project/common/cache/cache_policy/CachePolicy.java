package nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy;

import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileManager;

public interface CachePolicy<T> {
	long SIZE_KB = 1024L;
	long SIZE_MB = 1048576L;
	long SIZE_GB = 1073741824L;
	
	boolean update(CacheFileManager<T> fileManager, T file);

	boolean remove(T file);
	
	boolean isRegistered(T file);

	long getMaxSize();
	
	void setMaxSize(long size);
	
	long getCurSize();
	
}
