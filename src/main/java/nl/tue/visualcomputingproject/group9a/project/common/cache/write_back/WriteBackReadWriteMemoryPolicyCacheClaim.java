package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;

class WriteBackReadWriteMemoryPolicyCacheClaim<T extends CacheableObject>
		implements WriteBackReadWriteCacheClaim<T> {
	@Getter
	private final FileId id;
	@Getter
	private boolean valid = true;
	@Getter
	private T obj;
	
	public WriteBackReadWriteMemoryPolicyCacheClaim(FileId id, T obj) {
		this.id = id;
		obj = obj;
	}
	
	@Override
	public void invalidate() {
		valid = false;
	}

	@Override
	public long size() {
		return (obj == null ? 0L : obj.memorySize());
	}

	@Override
	public boolean exists() {
		return obj != null;
	}

	@Override
	public void delete() {
		obj = null;
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
