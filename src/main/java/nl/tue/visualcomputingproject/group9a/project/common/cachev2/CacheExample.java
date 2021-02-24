package nl.tue.visualcomputingproject.group9a.project.common.cachev2;

import lombok.AllArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy.LRUCachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.stream.BufferedFileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CacheExample {

	@AllArgsConstructor
	static class TestFileId
			implements FileId {
		private final ChunkPosition position;

		@Override
		public String getPath() {
			return FileId.genPath(
					"test" + File.separator +
					position.getX(),
					position.getY(),
					position.getWidth(),
					position.getHeight());
		}
	}

	public static void main(String[] args) {
		DiskCacheFileManager cacheManager = new DiskCacheFileManager(
				new LRUCachePolicy(5 * CachePolicy.SIZE_GB),
				Settings.CACHE_DIR);
		cacheManager.indexCache();
		// ------------------------------------------------------
		// Initialization
		CacheFileStreamRequester<TestFileId> requester = new CacheFileStreamRequester<>(
				cacheManager,
				new BufferedFileStreamFactory()
		);
		
		// Dynamically update cache memory consumption.
		cacheManager.getPolicy().setMaxSize(5 * CachePolicy.SIZE_KB);
		
		// Writing
		TestFileId id = new TestFileId(new ChunkPosition(0, 1, 2, 3));
		File file = requester.claim(id); // Claimed files won't be removed by the cache manager unless released.
		// Note: the data is first written to a temp file, and when the stream is closed,
		// it is moved to the returned file.
		if (!requester.isCached(id)) {
			try (OutputStream os = requester.getOutputStream(id)) {
				os.write("Hello there".getBytes(StandardCharsets.UTF_8));

			} catch (IOException e) {
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

		} catch (IOException e) {
			e.printStackTrace();
		}
//		finally {
//			requester.release(id);
//		}
	}
}
