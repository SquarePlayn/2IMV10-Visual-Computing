package nl.tue.visualcomputingproject.group9a.project.common.cache.disk;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.util.PostProcessOutputStream;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Implementation of {@link ReadWriteCacheClaim} for files stored on disk.
 */
public class FileReadWriteCacheClaim
		extends FileReadCacheClaim
		implements ReadWriteCacheClaim {
	
	FileReadWriteCacheClaim(FileId id, File cacheDir) {
		super(id, cacheDir);
	}

	@Override
	public void delete() {
		lock.lock();
		try {
			waitForRead("delete file");
			Files.delete(getFile().toPath());
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			
		} finally {
			lock.unlock();
		}
	}
	
	public OutputStream getOutputStream()
			throws IOException {
		lock.lock();
		try {
			waitForRead("create output stream");

			final File tmpFile = new File(file.getPath() + Settings.TMP_CACHE_EXT);
			return new PostProcessOutputStream(
					new FileOutputStream(tmpFile, false),
					() -> {
						try {
							if (!Thread.currentThread().isInterrupted()) {
									Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
							}
						} finally {
							lock.unlock();
						}
					});
		} catch (InterruptedException e) {
			lock.unlock();
			throw new IllegalStateException(e);
			
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}
	
}
