package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.AllArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy.LRUCachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.BufferedFileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CacheExample {

	@AllArgsConstructor
	static class TestFileId
			extends FileId {
		private final ChunkPosition position;

		@Override
		public String getPath() {
			return genPath(
					position.getX(),
					position.getY(),
					position.getWidth(),
					position.getHeight());
		}
	}

	public static void main(String[] args) {
		CacheFileManager cacheManager = new CacheFileManager(
				new LRUCachePolicy(5 * CachePolicy.SIZE_GB),
				Settings.CACHE_DIR);
		// ------------------------------------------------------
		// Initialization
		CacheFileStreamRequester<TestFileId> requester = new CacheFileStreamRequester<>(
				new Owner("test"),
				cacheManager,
				(String path) -> {
					String[] parts = path.split("_");
					return new TestFileId(new ChunkPosition(
							Double.parseDouble(parts[0]),
							Double.parseDouble(parts[0]),
							Double.parseDouble(parts[0]),
							Double.parseDouble(parts[0])));
				},
				new BufferedFileStreamFactory()
		);
		requester.open();
		
		// Writing
		TestFileId id = new TestFileId(new ChunkPosition(0, 1, 2, 3));
		File file = requester.claim(id); // Claimed files won't be removed by the cache manager unless released.
		// Note: the data is first written to a temp file, and when the stream is
		// closed it is moved to the returned file.
		if (!requester.isCached(id)) {
			try (OutputStream os = requester.getOutputStream(id)) {
				os.write("Hello there".getBytes(StandardCharsets.UTF_8));

			} catch (IOException | IllegalOwnerException e) {
				e.printStackTrace();
			}
		}

		// Reading (equivalent api is commented).
		try (InputStream is = requester.getInputStreamReleaseOnClose(id)) {
//		try (InputStream is = requester.getInputStream(id)) {
			StringBuilder sb = new StringBuilder();
			byte[] buf = new byte[1024];
			int count;
			while ((count = is.read(buf)) != -1) {
				sb.append(new String(buf, 0, count, StandardCharsets.UTF_8));
			}
			System.out.println(sb.toString());

		} catch (IOException | IllegalOwnerException e) {
			e.printStackTrace();
		}
//		finally {
//			requester.release(id);
//		}
		
		// Graceful termination (not required).
		requester.close();
	}
}
