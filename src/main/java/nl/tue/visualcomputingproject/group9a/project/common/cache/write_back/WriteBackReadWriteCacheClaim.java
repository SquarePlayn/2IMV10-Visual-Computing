package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadWriteCacheClaim;

public interface WriteBackReadWriteCacheClaim<T extends CacheableObject>
		extends ReadWriteCacheClaim {
	
	void toDisk();
	
	void set(T obj);
	
}
