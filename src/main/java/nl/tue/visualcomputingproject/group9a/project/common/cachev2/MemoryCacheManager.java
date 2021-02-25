package nl.tue.visualcomputingproject.group9a.project.common.cachev2;

import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class MemoryCacheManager<T extends CacheableObject>
		extends CacheManager<CacheMemoryFile<T>> {
	
	private final Map<FileId, CacheMemoryFile<T>> idToFile;
	
	public MemoryCacheManager(CachePolicy policy) {
		super(policy);
		idToFile = new ConcurrentHashMap<>();
	}

	@Override
	public void indexCache() {
	}

	@Override
	public CacheMemoryFile<T> fileOf(FileId id) {
		if (id == null) return null;
		CacheMemoryFile<T> file = idToFile.get(id);
		if (file == null) {
			file = new CacheMemoryFile<>(id, this);
			idToFile.put(id, file);
		}
		return file;
	}

	@Override
	public void deleteFile(FileId id) {
		if (id == null) return;
		CacheMemoryFile<T> file = idToFile.remove(id);
		if (file != null) {
			policy.remove(id);
		}
	}

	@Override
	public void notifyCacheDelete(CacheMemoryFile<T> file) {
		FileId id = file.getId();
		if (id != null) {
			idToFile.remove(id);
		}
	}

	@Override
	public boolean exists(FileId id) {
		return idToFile.containsKey(id);
	}

	@Override
	public boolean exists(CacheMemoryFile<T> file) {
		return idToFile.containsValue(file);
	}

	@Override
	public long sizeOf(FileId id) {
		if (id == null) return 0L;
		return sizeOf(fileOf(id));
	}

	@Override
	public long sizeOf(CacheMemoryFile<T> file) {
		if (file == null) return 0L;
		return file.memorySize();
	}

	public static void main(String[] args) {
		MemoryCacheManager<MeshChunkData> cache = new MemoryCacheManager<>(null);
	}
	
}
