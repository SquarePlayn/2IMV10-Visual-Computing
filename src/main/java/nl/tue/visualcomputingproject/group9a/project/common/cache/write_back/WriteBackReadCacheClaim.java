package nl.tue.visualcomputingproject.group9a.project.common.cache.write_back;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.MemoryStore;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class WriteBackReadCacheClaim<T extends CacheableObject>
		implements ReadCacheClaim {
	@Getter
	private final FileId id;
	
	protected final ReentrantLock lock = new ReentrantLock();
	protected final Condition awaitReading = lock.newCondition();
	protected int numReading = 0;
	protected boolean valid = true;

	protected final MemoryStore<T> store;
	@Getter
	protected final File file;
	protected boolean inMemory;

	@Getter
	protected final FileStreamFactory streamFactory;
	@Getter
	protected final ObjectSerializer<T> serializer;
	
	public WriteBackReadCacheClaim(
			FileId id,
			MemoryStore<T> store,
			File cacheDir,
			FileStreamFactory streamFactory,
			ObjectSerializer<T> serializer) {
		this.id = id;
		this.store = store;
		this.streamFactory = streamFactory;
		this.serializer = serializer;
		file = new File(cacheDir, id.getPath() + Settings.CACHE_EXT);
		inMemory = (store != null);
	}

	protected void waitForRead(String msg)
			throws InterruptedException {
		checkValid(msg);
		while (numReading > 0) {
			awaitReading.await();
			checkValid(msg);
		}
	}

	protected void checkValid(String msg) {
		if (!valid) {
			throw new IllegalStateException("Cannot " + msg + " for an invalidated claim!");
		}
	}
	
	public boolean isInMemory() {
		lock.lock();
		try {
			return inMemory;
		} finally {
			lock.unlock();
		}
	}
	
	public boolean isOnDisk() {
		return Files.exists(file.toPath());
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
			while (numReading > 0) {
				awaitReading.await();
			}
			return oldValid;

		} catch (InterruptedException e) {
			throw new IllegalStateException(e);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public long size() {
		if (inMemory) {
			return store.memorySize();
		} else {
			try {
				return Files.size(file.toPath());

			} catch (IOException e) {
				return 0L;
			}
		}
	}

	@Override
	public boolean exists() {
		return isInMemory() || isOnDisk();
	}

	protected void fetch() {
		lock.lock();
		try {
			try (InputStream is = streamFactory.read(file)) {
				store.set(serializer.deserialize(is));
				inMemory = true;
				
			} catch (IOException e) {
				e.printStackTrace();
			}

		} finally {
			lock.unlock();
		}
	}
	
	public T get() {
		lock.lock();
		try {
			if (!inMemory && isOnDisk()) {
				fetch();
			}
			if (inMemory) {
				return store.get();
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}
	
}
