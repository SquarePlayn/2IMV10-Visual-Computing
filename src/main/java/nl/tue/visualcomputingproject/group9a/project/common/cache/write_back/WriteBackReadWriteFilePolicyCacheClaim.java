package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WriteBackReadWriteFilePolicyCacheClaim<T extends CacheableObject>
		implements WriteBackReadWriteCacheClaim<T> {
	private final Lock lock = new ReentrantLock();
	@Getter
	private final FileId id;
	private boolean valid = true;
	@Getter
	private final File file;
	
	public WriteBackReadWriteFilePolicyCacheClaim(FileId id, File cacheDir) {
		this.id = id;
		file = new File(cacheDir, id.getPath() + Settings.CACHE_EXT);
	}
	
	@Override
	public boolean isValid() {
		lock.lock();
		try {
			return valid;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean invalidate() {
		lock.lock();
		try {
			boolean oldValid = valid;
			valid = false;
			return oldValid;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public long size() {
		return file.length();
	}

	@Override
	public boolean exists() {
		return Files.exists(file.toPath());
	}

	@Override
	public void delete() {
		lock.lock();
		try {
			if (!valid) {
				throw new IllegalStateException("Cannot delete file from an invalidated claim!");
			}
			//noinspection ResultOfMethodCallIgnored
			file.delete();
			
		} finally {
			lock.unlock();
		}
	}

	@Override
	public FileStreamFactory getStreamFactory() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ObjectSerializer<T> getSerializer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isInMemory() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isOnDisk() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T get() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void toDisk() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(CacheableObject obj) {
		throw new UnsupportedOperationException();
	}
	
}
