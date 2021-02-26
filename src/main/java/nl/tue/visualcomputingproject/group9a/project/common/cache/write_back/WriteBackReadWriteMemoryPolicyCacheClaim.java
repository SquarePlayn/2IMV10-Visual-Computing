package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.MemoryStore;

class WriteBackReadWriteMemoryPolicyCacheClaim<T extends CacheableObject>
		implements WriteBackReadWriteCacheClaim<T> {
	@Getter
	private final FileId id;
	@Getter
	private final MemoryStore<T> store;
	@Getter
	private boolean valid = true;
	
	public WriteBackReadWriteMemoryPolicyCacheClaim(FileId id, MemoryStore<T> store) {
		this.id = id;
		this.store = store;
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

	@Override
	public void delete() {
	}

	@Override
	public void toDisk() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(T obj) {
		throw new UnsupportedOperationException();
	}

}
