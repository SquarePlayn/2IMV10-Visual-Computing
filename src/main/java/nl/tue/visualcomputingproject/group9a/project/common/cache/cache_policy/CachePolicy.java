package nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy;

import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileManager;

import java.io.File;

public interface CachePolicy {
	long SIZE_KB = 1024L;
	long SIZE_MB = 1048576L;
	long SIZE_GB = 1073741824L;
	
	boolean update(CacheFileManager fileManager, File file);

	boolean remove(File file);
	
	boolean isRegistered(File file);
	
}
