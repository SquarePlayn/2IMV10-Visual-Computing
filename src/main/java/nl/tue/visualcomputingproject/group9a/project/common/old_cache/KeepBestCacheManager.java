package nl.tue.visualcomputingproject.group9a.project.common.cache;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeepBestCacheManager<V>
		extends CacheManager<ChunkId, V> {
	
	final private Map<ChunkPosition, QualityLevel> memoryQualityMapper;
	final private Map<ChunkPosition, QualityLevel> diskQualityMapper;

	/**
	 * Creates a new cache manager.
	 *
	 * @param cacheDir      The root directory to store the cache files in.
	 * @param keyFactory    The factory class used generate the names of the cache files.
	 * @param valueFactory  The factory class used to serialized and deserialize the data.
	 */
	public KeepBestCacheManager(
			File cacheDir,
			CacheNameFactory<ChunkId> keyFactory,
			CacheFactory<V> valueFactory) {
		this(cacheDir, keyFactory, valueFactory, new BufferedFileStreamFactory());
	}

	/**
	 * Creates a new cache manager.
	 *
	 * @param cacheDir      The root directory to store the cache files in.
	 * @param keyFactory    The factory class used generate the names of the cache files.
	 * @param valueFactory  The factory class used to serialized and deserialize the data.
	 * @param streamFactory The factory class used to initialize a stream to and from a file.
	 *                      Default is a buffered file stream.
	 */
	public KeepBestCacheManager(
			File cacheDir,
			CacheNameFactory<ChunkId> keyFactory,
			CacheFactory<V> valueFactory,
			FileStreamFactory streamFactory) {
		super(cacheDir, keyFactory, valueFactory, streamFactory);
		memoryQualityMapper = new ConcurrentHashMap<>();
		diskQualityMapper = new ConcurrentHashMap<>();
	}

	/**
	 * @param key The position to check for.
	 *
	 * @return {@code true} if the data of any quality is cached in memory.
	 *     {@code false} otherwise.
	 */
	public boolean isMemoryCached(ChunkPosition key) {
		return memoryQualityMapper.containsKey(key);
	}

	/**
	 * @param key The position to check for.
	 *
	 * @return {@code true} if the data of any quality is cached on disk.
	 *     {@code false} otherwise.
	 */
	public boolean isDiskCached(ChunkPosition key) {
		return diskQualityMapper.containsKey(key);
	}

	/**
	 * @param key The position to check for.
	 *
	 * @return {@code true} if the data of any quality is cached in memory or on disk.
	 *     {@code false} otherwise.
	 */
	public boolean isCached(ChunkPosition key) {
		return isMemoryCached(key) || isDiskCached(key);
	}


	@Override
	protected void addIndexDiskCache(ChunkId key) {
		super.addIndexDiskCache(key);
		diskQualityMapper.compute(key.getPosition(), (k, q) -> {
			if (q == null) {
				super.addIndexDiskCache(key);
				return key.getQuality();
				
			} else if (key.getQuality().getOrder() > q.getOrder()) {
				super.removeDiskCache(new ChunkId(key.getPosition(), q));
				super.addIndexDiskCache(key);
				return key.getQuality();
				
			} else {
				return q;
			}
		});
	}

	/**
	 * @param key The position to check for.
	 * 
	 * @return The quality level of the cached data at the position in memory.
	 *     {@code null} if it is not cached.
	 */
	public QualityLevel getMemoryQualityLevel(ChunkPosition key) {
		return memoryQualityMapper.get(key);
	}

	/**
	 * @param key The position to check for.
	 *
	 * @return The quality level of the cached data at the position on disk.
	 *     {@code null} if it is not cached.
	 */
	public QualityLevel getDiskQualityLevel(ChunkPosition key) {
		return diskQualityMapper.get(key);
	}

	/**
	 * @param key The position to check for.
	 *
	 * @return The best quality level of the cached data at the position in memory or on disk.
	 *     {@code null} if it is not cached.
	 */
	public QualityLevel getBestQualityLevel(ChunkPosition key) {
		QualityLevel memory = getMemoryQualityLevel(key);
		QualityLevel disk = getDiskQualityLevel(key);
		if (disk == null) {
			return memory;
		} else if (memory == null) {
			return disk;
		} else {
			return memory.getOrder() >= disk.getOrder() ? memory : disk;
		}
	}

	/**
	 * Retrieves the quality of cached data from memory.
	 *
	 * @param key The position to get the data for.
	 *
	 * @return The cached data at the given position in memory with the quality level.
	 */
	public Pair<V, QualityLevel> getFromMemory(ChunkPosition key) {
		QualityLevel memory = getMemoryQualityLevel(key);
		if (memory == null) return null;
		V val = getFromMemory(new ChunkId(key, memory));
		if (val == null) return null;
		return new Pair<>(val, memory);
	}

	/**
	 * Retrieves the quality of cached data from disk.
	 *
	 * @param key The position to get the data for.
	 *
	 * @return The cached data at the given position on disk with the quality level.
	 */
	public Pair<V, QualityLevel> getFromDisk(ChunkPosition key) {
		QualityLevel disk = getDiskQualityLevel(key);
		if (disk == null) return null;
		V val = getFromDisk(new ChunkId(key, disk));
		if (val == null) return null;
		return new Pair<>(val, disk);
	}

	/**
	 * Retrieves the best quality of cached data from memory or disk.
	 * If both memory and disk cache have the same quality, memory is
	 * preferred.
	 * 
	 * @param key The position to get the data for.
	 *
	 * @return The best cached data at the given position with the quality level.
	 */
	public Pair<V, QualityLevel> getBest(ChunkPosition key) {
		QualityLevel memory = getMemoryQualityLevel(key);
		QualityLevel disk = getDiskQualityLevel(key);
		V val = null;
		QualityLevel best = null;
		if (memory != null && (disk == null || memory.getOrder() >= disk.getOrder())) {
			best = memory;
			val = getFromMemory(new ChunkId(key, memory));
		} else if (disk != null) {
			best = disk;
			val = getFromDisk(new ChunkId(key, disk));
		}
		if (val == null) return null;
		return new Pair<>(val, best);
	}

	@Override
	public boolean putMemoryCache(ChunkId key, V value) {
		QualityLevel curQuality = memoryQualityMapper.get(key.getPosition());
		if (curQuality.getOrder() >= key.getQuality().getOrder()) {
			return false;
		}
		if (super.putMemoryCache(key, value)) {
			memoryQualityMapper.put(key.getPosition(), key.getQuality());
			removeMemoryCache(new ChunkId(key.getPosition(), curQuality));
			return true;
		}
		return false;
	}

	@Override
	public boolean putDiskCache(ChunkId key, V value) {
		QualityLevel curQuality = diskQualityMapper.get(key.getPosition());
		if (curQuality.getOrder() >= key.getQuality().getOrder()) {
			return false;
		}
		if (super.putDiskCache(key, value)) {
			diskQualityMapper.put(key.getPosition(), key.getQuality());
			super.removeDiskCache(new ChunkId(key.getPosition(), curQuality));
			return true;
		}
		return false;
	}

	@Override
	public void removeMemoryCache(ChunkId key) {
		if (memoryQualityMapper.get(key.getPosition()) == key.getQuality()) {
			memoryQualityMapper.remove(key.getPosition());
		}
		super.removeMemoryCache(key);
	}

	@Override
	public void removeDiskCache(ChunkId key) {
		if (diskQualityMapper.get(key.getPosition()) == key.getQuality()) {
			diskQualityMapper.remove(key.getPosition());
		}
		super.removeDiskCache(key);
	}

	public void removeMemoryCache(ChunkPosition key) {
		QualityLevel memory = memoryQualityMapper.remove(key);
		if (memory != null) {
			super.removeMemoryCache(new ChunkId(key, memory));
		}
	}

	public void removeDiskCache(ChunkPosition key) {
		QualityLevel disk = memoryQualityMapper.remove(key);
		if (disk != null) {
			super.removeDiskCache(new ChunkId(key, disk));
		}
	}
	
	public void remove(ChunkPosition key) {
		removeMemoryCache(key);
		removeDiskCache(key);
	}
	
}
