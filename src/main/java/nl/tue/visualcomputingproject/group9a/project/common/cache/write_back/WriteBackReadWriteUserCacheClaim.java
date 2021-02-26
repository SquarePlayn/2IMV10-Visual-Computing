package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.*;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class WriteBackReadWriteUserCacheClaim<T extends CacheableObject>
		extends WriteBackReadCacheClaim<T>
		implements WriteBackReadWriteCacheClaim<T> {

	public WriteBackReadWriteUserCacheClaim(
			FileId id,
			MemoryStore<T> store,
			File cacheDir,
			FileStreamFactory streamFactory,
			ObjectSerializer<T> serializer) {
		super(id, store, cacheDir, streamFactory, serializer);
	}

	@Override
	public void delete() {
		lock.lock();
		try {
			if (inMemory) {
				store.set(null);
				inMemory = false;
			}
			if (isOnDisk()) {
				//noinspection ResultOfMethodCallIgnored
				file.delete();
			}
			
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void toDisk() {
		lock.lock();
		try {
			final File tmpFile = new File(file.getPath() + Settings.TMP_CACHE_EXT);
			try (OutputStream os = streamFactory.write(tmpFile)) {
				serializer.serialize(os, store.get());
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				
			} catch (IOException e) {
				e.printStackTrace();
				//noinspection ResultOfMethodCallIgnored
				tmpFile.delete();
			}

		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void set(T obj) {
		lock.lock();
		try {
			store.set(obj);
			inMemory = true;
			
			// Delete invalid disk cached file.
			if (isOnDisk()) {
				//noinspection ResultOfMethodCallIgnored
				file.delete();
			}
			
		} finally {
			lock.unlock();
		}
	}
	
}
