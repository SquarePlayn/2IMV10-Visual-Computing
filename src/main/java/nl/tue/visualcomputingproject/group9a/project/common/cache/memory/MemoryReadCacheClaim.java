package nl.tue.visualcomputingproject.group9a.project.common.cache.memory;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.MemoryStore;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadCacheClaim;

public class MemoryReadCacheClaim<T extends CacheableObject>
		implements ReadCacheClaim {
	@Getter
	protected final FileId id;
	protected final MemoryStore<T> store;
	volatile protected boolean valid = true;
	
	MemoryReadCacheClaim(FileId id, MemoryStore<T> store) {
		this.id = id;
		this.store = store;
	}
	protected void checkValid(String msg) {
		if (!valid) {
			throw new IllegalStateException("Cannot " + msg + " for an invalidated claim!");
		}
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public boolean invalidate() {
		boolean oldValid = valid;
		valid = false;
		return oldValid;
	}

	@Override
	public long size() {
		return store.memorySize();
	}

	@Override
	public boolean exists() {
		return !store.isEmpty();
	}
	
	public T get() {
		checkValid("get object");
		return store.get();
	}
	
}
