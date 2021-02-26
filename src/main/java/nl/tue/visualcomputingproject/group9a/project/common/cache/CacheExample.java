package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileCacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.memory.MemoryCacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.memory.MemoryReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.memory.MemoryReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.LRUCachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CacheExample {

	@Value
	private static class TestFileId
			implements FileId {
		ChunkPosition position;

		@Override
		public String getPath() {
			return FileId.genPath(
					"test" + File.separator +
							position.getX(),
					position.getY(),
					position.getWidth(),
					position.getHeight());
		}
		
		public static FileIdFactory<TestFileId> createFactory() {
			return (String path) -> {
				String[] parts = path.split("_");
				if (parts.length != 4) {
					return null;
				}

				try {
					double x = Double.parseDouble(parts[0]);
					double y = Double.parseDouble(parts[1]);
					double w = Double.parseDouble(parts[2]);
					double h = Double.parseDouble(parts[3]);
					return new TestFileId(new ChunkPosition(x, y, w, h));
					
				} catch (NumberFormatException e) {
					return null;
				}
			};
		}
	}
	
	@Value
	private static class Str
			implements CacheableObject {
		String str;
		@Override
		public long memorySize() {
			return 2L*str.length();
		}
	}
	
	public static void main(String[] args) {
		// Initial setup.
		CachePolicy policy = new LRUCachePolicy(2 * CachePolicy.SIZE_GiB);
		FileCacheManager fileManager = new FileCacheManager(policy, Settings.CACHE_DIR, "test");
		MemoryCacheManager<Str> memoryManager = new MemoryCacheManager<>(policy);
		
		// Local initialisation.
		fileManager.indexCache(TestFileId.createFactory());
		
		diskExample(fileManager);
		memoryExample(memoryManager);
	}
	
	public static void diskExample(FileCacheManager manager) {
		// Implementation.
		TestFileId id = new TestFileId(new ChunkPosition(1, 2, 3, 4));

		// Atomically checks if file exists/reads are allowed.
		FileReadCacheClaim read = manager.requestReadClaim(id);
		if (read == null) {
			// If not, atomically request a write claim and write data.
			FileReadWriteCacheClaim write = manager.requestReadWriteClaim(id);
			if (write == null) {
				// The file is already being written to.
				return;
			}
			try (OutputStream os = new BufferedOutputStream(write.getOutputStream())) {
				os.write("Hello there".getBytes(StandardCharsets.UTF_8));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Atomically convert the write claim to a read claim
			read = manager.degradeClaim(write);
			if (read == null) {
				// The file is empty.
				return;
			}
		}
		
		try (InputStream is = new BufferedInputStream(read.getInputStream())) {
			byte[] buffer = new byte[1024];
			StringBuilder sb = new StringBuilder();
			int amt;
			while ((amt = is.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, amt, StandardCharsets.UTF_8));
			}
			System.out.println(sb.toString());
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		manager.releaseCacheClaim(read);
	}
	
	public static void memoryExample(MemoryCacheManager<Str> manager) {
		// Implementation.
		TestFileId id = new TestFileId(new ChunkPosition(1, 2, 3, 4));

		// Atomically checks if file exists/reads are allowed.
		MemoryReadCacheClaim<Str> read = manager.requestReadClaim(id);
		if (read == null) {
			// If not, atomically request a write claim and write data.
			MemoryReadWriteCacheClaim<Str> write = manager.requestReadWriteClaim(id);
			if (write == null) {
				// The file is already being written to.
				return;
			}
			
			write.set(new Str("Hello there"));
			
			// Atomically convert the write claim to a read claim
			read = manager.degradeClaim(write);
			if (read == null) {
				// The file is empty.
				return;
			}
		}
		
		Str str = read.get();
		System.out.println("Memory: " + str.toString());

		manager.releaseCacheClaim(read);
	}
	
}
