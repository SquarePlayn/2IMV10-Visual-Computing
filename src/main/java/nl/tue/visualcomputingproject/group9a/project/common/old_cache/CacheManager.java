package nl.tue.visualcomputingproject.group9a.project.common.old_cache;

import nl.tue.visualcomputingproject.group9a.project.common.cache.EOFException;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.BufferedFileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache manager which caches data both memory and on disk.
 * The data can be added and removed for both simultaneously and separately.
 * 
 * @param <K> The (small) class used to uniquely identify each data class.
 * @param <V> The class of the data to store.
 */
@Deprecated
public class CacheManager<K, V> {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** The file extension of the cache files. */
	static protected final String CACHE_EXT = ".cache";
	static protected final String TMP_EXT = ".tmp";
	/** Whether to overwrite old data if there are clashing keys. */
	static protected final boolean DEFAULT_OVERWRITE = true; // TODO: move to {@link Settings}?
	
	/** The root directory to store the cache files in. */
	protected final File cacheDir;
	/** The factory class used generate the names of the cache files. */
	protected final CacheNameFactory<K> keyFactory;
	/** The factory class used to serialized and deserialize the data. */
	protected final ObjectSerializer<V> valueFactory;
	/** The in memory cache. */
	protected final Map<K, V> memoryCache;
	/** Set containing the data cached on disk. */
	protected final Set<K> diskCache;
	/** The factory used to initialize file streams */
	protected final FileStreamFactory streamFactory;

	

	/**
	 * Creates a new cache manager.
	 *
	 * @param cacheDir      The root directory to store the cache files in.
	 * @param keyFactory    The factory class used generate the names of the cache files.
	 * @param valueFactory  The factory class used to serialized and deserialize the data.
	 */
	public CacheManager(
			File cacheDir,
			CacheNameFactory<K> keyFactory,
			ObjectSerializer<V> valueFactory) {
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
	public CacheManager(
			File cacheDir,
			CacheNameFactory<K> keyFactory,
			ObjectSerializer<V> valueFactory,
			FileStreamFactory streamFactory) {
		this.cacheDir = cacheDir;
		this.keyFactory = keyFactory;
		this.valueFactory = valueFactory;
		this.streamFactory = streamFactory;
		memoryCache = new ConcurrentHashMap<>();
		diskCache = ConcurrentHashMap.newKeySet();
	}

	/**
	 * Creates a file which is unique for the given key.
	 * 
	 * @param keyName The name of the key to generate the file for.
	 * 
	 * @return A file unique to the given key.
	 */
	private File keyToFileName(String keyName) {
		return new File(cacheDir.toString() + File.separator +
				keyName + CACHE_EXT);
	}
	
	private File keyToTmpFileName(String keyName) {
		return new File(cacheDir.toString() + File.separator +
				"tmp" + File.separator +
				keyName.replaceAll(File.separator, "____") + TMP_EXT + ".tmp");
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
		try (InputStream is = streamFactory.read(file)) {
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
		String keyName = keyFactory.getString(key);
		File tmpFile = keyToTmpFileName(keyName);
		File file = keyToFileName(keyName);
		try {
			if (file.exists() && overwrite && !file.delete()) {
				return false;
			}

			if (!file.getParentFile().exists()) {
				if (!file.getParentFile().mkdirs()) return false;
			}

			//noinspection ResultOfMethodCallIgnored
			tmpFile.getParentFile().mkdirs();
			//noinspection ResultOfMethodCallIgnored
			tmpFile.delete();
			
			try (OutputStream os = streamFactory.write(tmpFile)) {
				valueFactory.serialize(os, value);
			}
			Files.move(tmpFile.toPath(), file.toPath());
			
			return true;
			
		} finally {
			//noinspection ResultOfMethodCallIgnored
			tmpFile.delete();
		}
	}

	/**
	 * Method used to add a key to the disk cache during indexing.
	 * 
	 * @param key The key to add to the disk cache.
	 * 
	 * @see #indexDiskCache() 
	 */
	protected void addIndexDiskCache(K key) {
		diskCache.add(key);
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

				} else if (file.getName().endsWith(CACHE_EXT)) {
					String path = file.getAbsolutePath();
					String name = path.substring(
							rootDirLength,
							path.length() - CACHE_EXT.length());
					System.out.println(path);
					System.out.println(name);
					try {
						addIndexDiskCache(keyFactory.fromString(name));
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
	 * @return {@code true} if the data is cached in memory.
	 *     {@code false} otherwise.
	 */
	public boolean isMemoryCached(K key) {
		return memoryCache.containsKey(key);
	}

	/**
	 * @param key The key to check for.
	 *
	 * @return {@code true} if the data is cached on disk.
	 *     {@code false} otherwise.
	 */
	public boolean isDiskCached(K key) {
		return diskCache.contains(key);
	}

	/**
	 * @param key The key to check for.
	 *
	 * @return {@code true} if the data is cached in memory or on disk.
	 *     {@code false} otherwise.
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
			V val = fileToValue(keyToFileName(keyFactory.getString(key)));
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
			diskCache.remove(key);
			//noinspection ResultOfMethodCallIgnored
			keyToFileName(keyFactory.getString(key)).delete();
			
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

//	/**
//	 * TODO: to be removed.
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		File cacheDir = new File("cache");
//		System.out.println(cacheDir.getAbsoluteFile());
//		
//		int amt = 3*4*1024;
//		ByteBuffer big = BufferUtils.createByteBuffer(amt);
//		ByteBuffer small = BufferUtils.createByteBuffer(amt);
//		byte[] bigArr = new byte[amt];
//		byte[] smallArr = new byte[amt];
//
//		Random ran = new Random();
//		ran.nextBytes(bigArr);
//		big.put(bigArr);
//		ran.nextBytes(smallArr);
//		small.put(smallArr);
//		
////		for (int i = 0; i < amt; i++) {
////			bigArr[i] = (byte) i;
////			smallArr[i] = (byte) i;
//////			bigArr[i] = (byte) ('A' + (i % 26));
//////			smallArr[i] = (byte) ('a' + (i % 26));
////		}
////		big.put(bigArr);
////		small.put(smallArr);
//		
//		big.flip();
//		small.flip();
//		
//		MeshChunkData data = new MeshChunkData(
//				VertexBufferType.INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT,
//				MeshBufferType.TRIANGLES_CLOCKWISE_3_INT,
//				amt,
//				small,
//				big
//		);
//		
//		CacheManager<ChunkId, MeshChunkData> manager = new CacheManager<>(
//				cacheDir,
//				ChunkId.createCacheNameFactory("mesh_data" + File.separator),
//				MeshChunkData.createCacheFactory(),
//				new ZipBufferedFileStreamFactory());
//		
//		ChunkId key = new ChunkId(
//				new ChunkPosition(0, 0, 100, 100),
//				QualityLevel.FIVE_BY_FIVE);
//		manager.put(key, data);
//		
//		System.out.println(manager.isMemoryCached(key));
//		System.out.println(manager.isDiskCached(key));
//		System.out.println(manager.isCached(key));
//
//		manager.removeMemoryCache(key);
//		MeshChunkData data2 = manager.get(key);
//		System.out.println(data);
//		System.out.println(data2);
//		System.out.println(data.getMeshBufferType() == data2.getMeshBufferType());
//		System.out.println(data.getVertexBufferType() == data2.getVertexBufferType());
//	}

}
