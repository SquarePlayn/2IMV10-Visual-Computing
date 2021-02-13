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

public class CacheManager<K, V> {
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	static private final String EXT = "cache";
	static private final boolean DEFAULT_OVERWRITE = true;
	
	private final File cacheDir;
	private final CacheNameFactory<K> keyFactory;
	private final CacheFactory<V> valueFactory;
	private final Map<K, V> memoryCache;
	private final Set<K> diskCache;
	
	
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
	
	private File keyToFileName(K key) {
		return new File(cacheDir.toString() + File.separator + keyFactory.getString(key) + "." + EXT);
	}
	
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
	
	public boolean isMemoryCached(K key) {
		return memoryCache.containsKey(key);
	}
	
	public boolean isDiskCached(K key) {
		return diskCache.contains(key);
	}
	
	public boolean isCached(K key) {
		return isMemoryCached(key) || isDiskCached(key);
	}
	
	public V getFromMemory(K key) {
		return memoryCache.get(key);
	}
	
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
	
	public V get(K key) {
		V val = getFromMemory(key);
		if (val != null) return val;
		return getFromDisk(key);
	}
	
	public boolean putMemoryCache(K key, V value) {
		if (!DEFAULT_OVERWRITE && memoryCache.containsKey(key)) {
			return false;
		}

		memoryCache.put(key, value);
		return true;
	}
	
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
	
	public void put(K key, V value) {
		putMemoryCache(key, value);
		putDiskCache(key, value);
	}
	
	public void removeMemoryCache(K key) {
		try {
			memoryCache.remove(key);
			
		} catch (Exception e) {
			LOGGER.error("Exception occurred while removing " + key +
					" from memory cache.");
		}
	}
	
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
	
	public void remove(K key) {
		removeDiskCache(key);
		removeMemoryCache(key);
	}
	
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
