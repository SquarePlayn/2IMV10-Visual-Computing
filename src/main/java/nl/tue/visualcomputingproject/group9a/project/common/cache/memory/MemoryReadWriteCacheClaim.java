package nl.tue.visualcomputingproject.group9a.project.common.cache.memory;

import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.MemoryStore;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadWriteCacheClaim;

public class MemoryReadWriteCacheClaim<T extends CacheableObject>
		extends MemoryReadCacheClaim<T>
		implements ReadWriteCacheClaim {

	MemoryReadWriteCacheClaim(FileId id, MemoryStore<T> store) {
		super(id, store);
	}

	@Override
	public void delete() {
		checkValid("delete object");
		store.set(null);
	}
	
	public void set(T obj) {
		checkValid("set object");
		store.set(obj);
	}
	
}
