package nl.tue.visualcomputingproject.group9a.project.common.cache.disk;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.util.PostProcessInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link ReadCacheClaim} for files stored on disk.
 */
public class FileReadCacheClaim
		implements ReadCacheClaim {
	@Getter
	protected final FileId id;
	
	protected final ReentrantLock lock = new ReentrantLock();
	protected final Condition awaitReading = lock.newCondition();
	protected int numReading = 0;
	protected boolean valid = true;
	
	@Getter
	protected final File file;
	
	FileReadCacheClaim(FileId id, File cacheDir) {
		this.id = id;
		file = new File(cacheDir, id.getPath() + Settings.CACHE_EXT);
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
		try {
			return Files.size(file.toPath());
			
		} catch (IOException e) {
			return 0L;
		}
	}

	@Override
	public boolean exists() {
		return Files.exists(file.toPath());
	}
	
	public InputStream getInputStream()
			throws IOException {
		lock.lock();
		try {
			checkValid("create input stream");
			++numReading;
		} finally {
			lock.unlock();
		}

		return new PostProcessInputStream(new FileInputStream(file), () -> {
			lock.lock();
			try {
				if (--numReading == 0) {
					awaitReading.signal();
				}
			} finally {
				lock.unlock();
			}
		});
	}
	
	
}
