package nl.tue.visualcomputingproject.group9a.project.common.cache;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;

/**
 * A cache manager which caches data both memory and on disk.
 * The data can be added and removed for both simultaneously and separately.
 * 
 * @param <K> The (small) class used to uniquely identify each data class.
 * @param <V> The class of the data to store.
 */
public class CacheManager<K, V> {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** The file extension of the cache files. */
	static private final String EXT = "cache";
	/** Whether to overwrite old data if there are clashing keys. */
	static private final boolean DEFAULT_OVERWRITE = true; // TODO: move to {@link Settings}?
	
	/** The root directory to store the cache files in. */
	private final File cacheDir;
	/** The factory class used generate the names of the cache files. */
	private final CacheNameFactory<K> keyFactory;
	/** The factory class used to serialized and deserialize the data. */
	private final CacheFactory<V> valueFactory;
	/** The in memory cache. */
	private final Map<K, V> memoryCache;
	/** Set containing the data cached on disk. */
	private final Set<K> diskCache;

	/**
	 * Creates a new cache manager.
	 * 
	 * @param cacheDir     The root directory to store the cache files in.
	 * @param keyFactory   The factory class used generate the names of the cache files.
	 * @param valueFactory The factory class used to serialized and deserialize the data.
	 */
	public CacheManager(
			File cacheDir,
			CacheNameFactory<K> keyFactory,
			CacheFactory<V> valueFactory) {
		this.cacheDir = cacheDir;
		this.keyFactory = keyFactory;
		this.valueFactory = valueFactory;
		memoryCache = new HashMap<>();
		diskCache = new HashSet<>();
	}

	/**
	 * Creates a file which is unique for the given key.
	 * 
	 * @param key The key to generate the file for.
	 * 
	 * @return A file unique to the given key.
	 */
	private File keyToFileName(K key) {
		return new File(cacheDir.toString() + File.separator + keyFactory.getString(key) + "." + EXT);
	}

	/**
	 * Reads a file and deserializes it into an object.
	 * 
	 * @param file The file to read from.
	 * 
	 * @return The deserialized object.
	 * 
	 * @throws IOException If some IO exception occurs.
	 */
	private V fileToValue(File file)
			throws IOException {
		if (!file.exists()) return null;
		try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
			V val = valueFactory.deserialize(is);
			if (is.read() != -1) {
				throw new EOFException("Expected EOF, but there is still data remaining!");
			}
			return val;
		}
	}

	/**
	 * Deserializes the {@code value} object and writes it to
	 * a file uniquely defined by {@code key}.
	 * 
	 * @param key       The key uniquely defining {@code value}.
	 * @param value     The data object.
	 * @param overwrite Whether to overwrite any existing files.
	 * 
	 * @return {@code true} if the new version was installed successfully.
	 *     {@code false} otherwise.
	 *
	 * @throws IOException If some IO exception occurs.
	 */
	@SuppressWarnings("SameParameterValue")
	private boolean valueToFile(K key, V value, boolean overwrite)
			throws IOException {
		File file = keyToFileName(key);
		
		if (file.exists() && overwrite && !file.delete()) {
			return false;
		}
		
		if (!file.getParentFile().exists()) {
			if (!file.getParentFile().mkdirs()) return false;
		}
		
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file, false))) {
			valueFactory.serialize(os, value);
		}
		
		return true;
	}

	/**
	 * Indexes the cache on disk, making it ready for usage.
	 */
	public void indexDiskCache() {
		try {
			LOGGER.info("Indexing disk cache...");
			diskCache.clear();
			if (!cacheDir.exists() && !cacheDir.mkdirs()) {
				LOGGER.error("Could not create cache directory: " + cacheDir);
				LOGGER.error("Continuing without disk cache.");
				return;
			}
			
			if (!Files.isDirectory(cacheDir.toPath())) {
				LOGGER.error("Disk cache directory is not a directory");
				LOGGER.error("Continuing without disk cache.");
				return;
			}
			
			int rootDirLength = cacheDir.getAbsolutePath().length() + 1;
			File[] fileArr = cacheDir.listFiles();
			if (fileArr == null) return;
			List<File> files = new ArrayList<>(Arrays.asList(fileArr));
			for (int i = 0; i < files.size(); i++) {
				File file = files.get(i);
				if (file.isDirectory()) {
					fileArr = file.listFiles();
					if (fileArr != null) {
						files.addAll(Arrays.asList(fileArr));
					}

				} else if (file.getName().endsWith("." + EXT)) {
					String path = file.getAbsolutePath();
					String name = path.substring(
							rootDirLength,
							path.length() - EXT.length() - 1);
					System.out.println(path);
					System.out.println(name);
					try {
						diskCache.add(keyFactory.fromString(name));
						LOGGER.info("Added " + name + " to disk cache!");
						
					} catch (IllegalArgumentException e) {
						LOGGER.error("Could not process " + path +
								"! Skipping file.");
						e.printStackTrace();
					}
				}
			}
			LOGGER.info("Finished indexing disk cache!");
			
		} catch (Exception e) {
			LOGGER.error("Aborted indexing disk cache!");
			e.printStackTrace();
		}
	}

	/**
	 * @param key The key to check for.
	 *    
	 * @return {@code true} if the data is cached in memory. {@code false} otherwise.
	 */
	public boolean isMemoryCached(K key) {
		return memoryCache.containsKey(key);
	}

	/**
	 * @param key The key to check for.
	 *
	 * @return {@code true} if the data is cached on disk. {@code false} otherwise.
	 */
	public boolean isDiskCached(K key) {
		return diskCache.contains(key);
	}

	/**
	 * @param key The key to check for.
	 *
	 * @return {@code true} if the data is cached in memory or on disk. {@code false} otherwise.
	 */
	public boolean isCached(K key) {
		return isMemoryCached(key) || isDiskCached(key);
	}

	/**
	 * Retrieves the data with the given key from the in memory cache.
	 *
	 * @param key The key to get the data for.
	 * 
	 * @return The data of the key, or {@code null} if it is not cached in memory.
	 */
	public V getFromMemory(K key) {
		return memoryCache.get(key);
	}

	/**
	 * Retrieves the data with the given key from the disk cache.
	 *
	 * @param key The key to get the data for.
	 *
	 * @return The data of the key, or {@code null} if it is not cached on disk.
	 */
	public V getFromDisk(K key) {
		if (!diskCache.contains(key)) return null;
		try {
			V val = fileToValue(keyToFileName(key));
			if (val != null) {
				memoryCache.put(key, val);
				return val;
			}
			
		} catch (IOException e) {
			LOGGER.error("Error while reading cache file for " + keyFactory.getString(key));
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Retrieves the data with the given key from the in memory cache.
	 * If it could not be found, then it tries the disk cache.
	 *
	 * @param key The key to get the data for.
	 *
	 * @return The data of the key, or {@code null} if it is cached neither
	 *     in memory nor on disk.
	 */
	public V get(K key) {
		V val = getFromMemory(key);
		if (val != null) return val;
		return getFromDisk(key);
	}

	/**
	 * Caches the given key and data pair in memory only.
	 * 
	 * @param key   The key of the data to store.
	 * @param value The data to store.
	 * 
	 * @return {@code true} if the new data was installed in memory.
	 */
	public boolean putMemoryCache(K key, V value) {
		if (!DEFAULT_OVERWRITE && memoryCache.containsKey(key)) {
			return false;
		}

		memoryCache.put(key, value);
		return true;
	}

	/**
	 * Caches the given key and data pair on disk only.
	 * 
	 * @param key   The key of the data to store.
	 * @param value The data to store.
	 *
	 * @return {@code true} if the new data was installed on disk.
	 */
	public boolean putDiskCache(K key, V value) {
		if (!DEFAULT_OVERWRITE && diskCache.contains(key)) {
			return false;
		}
		
		try {
			if (valueToFile(key, value, DEFAULT_OVERWRITE)) {
				diskCache.add(key);
				return true;
			}
			
		} catch (IOException e) {
			LOGGER.error("Error while caching to disk for: " + keyFactory.getString(key));
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Caches the given key and data pair both in memory and on disk.
	 *
	 * @param key   The key of the data to store.
	 * @param value The data to store.
	 */
	public void put(K key, V value) {
		putMemoryCache(key, value);
		putDiskCache(key, value);
	}

	/**
	 * Removes the data with the given key from the in memory cache.
	 * 
	 * @param key The key of the data to remove.
	 */
	public void removeMemoryCache(K key) {
		try {
			memoryCache.remove(key);
			
		} catch (Exception e) {
			LOGGER.error("Exception occurred while removing " + key +
					" from memory cache.");
		}
	}

	/**
	 * Removes the data with the given key from the on disk cache.
	 *
	 * @param key The key of the data to remove.
	 */
	public void removeDiskCache(K key) {
		try {
			//noinspection ResultOfMethodCallIgnored
			keyToFileName(key).delete();
			diskCache.remove(key);
			
		} catch (Exception e) {
			LOGGER.error("Exception occurred while removing " + key +
					" from disk cache.");
		}
	}

	/**
	 * Removes the data with the given key from both the in memory and on disk cache.
	 *
	 * @param key The key of the data to remove.
	 */
	public void remove(K key) {
		removeDiskCache(key);
		removeMemoryCache(key);
	}

	/**
	 * TODO: to be removed.
	 * @param args
	 */
	public static void main(String[] args) {
		File cacheDir = new File("cache");
		System.out.println(cacheDir.getAbsoluteFile());
		
		ByteBuffer big = BufferUtils.createByteBuffer(24);
		ByteBuffer small = BufferUtils.createByteBuffer(24);
		byte[] bigArr = new byte[24];
		byte[] smallArr = new byte[24];
		for (int i = 0; i < 24; i++) {
			bigArr[i] = (byte) (65 + i);
			smallArr[i] = (byte) (97 + i);
		}
		big.put(bigArr);
		small.put(smallArr);
		big.flip();
		small.flip();
		
		MeshChunkData data = new MeshChunkData(
				VertexBufferType.INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT,
				MeshBufferType.TRIANGLES_CLOCKWISE_3_INT,
				1,
				small,
				big
		);
		
		CacheManager<ChunkId, MeshChunkData> manager = new CacheManager<>(
				cacheDir,
				ChunkId.createCacheNameFactory("mesh_data" + File.separator),
				MeshChunkData.createCacheFactory());
		
		ChunkId key = new ChunkId(
				new ChunkPosition(0, 0, 100, 100),
				QualityLevel.FIVE_BY_FIVE);
		manager.put(key, data);
		
		System.out.println(manager.isMemoryCached(key));
		System.out.println(manager.isDiskCached(key));
		System.out.println(manager.isCached(key));

		manager.removeMemoryCache(key);
		MeshChunkData data2 = manager.get(key);
		System.out.println(data2);
	}
	
}
