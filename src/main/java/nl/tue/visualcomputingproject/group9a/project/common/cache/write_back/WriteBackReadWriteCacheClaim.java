package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;

import java.io.File;

public interface WriteBackReadWriteCacheClaim<T extends CacheableObject>
		extends ReadWriteCacheClaim {
	
	File getFile();
	
	FileStreamFactory getStreamFactory();
	
	ObjectSerializer<T> getSerializer();
	
	boolean isInMemory();
	
	boolean isOnDisk();
	
	T get();
	
	void toDisk();
	
	void set(T obj);
	
}
