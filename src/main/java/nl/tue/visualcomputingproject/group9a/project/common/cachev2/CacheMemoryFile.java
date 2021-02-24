package nl.tue.visualcomputingproject.group9a.project.common.cachev2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.memory.MemoryCacheObject;

@RequiredArgsConstructor
public class CacheMemoryFile<V extends MemoryCacheObject> {
	@Getter
	private final FileId id;
	private final MemoryCacheManager<V> manager;
	private V data = null;
	
	long memorySize() {
		if (data == null) return 0L;
		return data.memorySize();
	}
	
	public V getData() {
		manager.getPolicy().update(manager, this);
		return data;
	}
	
	public void setData(V data) {
		this.data = data;
		if (data == null) {
			manager.getPolicy().remove(this);
		} else {
			manager.getPolicy().update(manager, this);
		}
	}
	
}
