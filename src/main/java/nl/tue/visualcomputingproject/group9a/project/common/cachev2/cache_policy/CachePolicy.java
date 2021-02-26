package nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy;

import nl.tue.visualcomputingproject.group9a.project.common.cachev2.CacheManager;

@Deprecated
public interface CachePolicy {
	long SIZE_KB = 1024L;
	long SIZE_MB = 1048576L;
	long SIZE_GB = 1073741824L;
	
	<T> boolean update(CacheManager<T> fileManager, T file);

	<T> boolean remove(T id);
	
	<T> boolean isRegistered(T id);

	long getMaxSize();
	
	void setMaxSize(long size);
	
	long getCurSize();
	
}
