package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Getter
@AllArgsConstructor
public class CacheFileStreamRequester<T extends FileId> {
	private final Owner owner;
	private final CacheFileManager fileManager;
	private final FileIdFactory<T> idFactory;
	private final FileStreamFactory streamFactory;
	
	
	public void open()
			throws IllegalOwnerException {
		fileManager.registerOwnerResolveUnclaimed(owner, idFactory);
	}
	
	public void close()
			throws IllegalOwnerException {
		fileManager.deregisterOwner(owner);
	}
	
	public InputStream getInputStream(FileId id)
			throws IllegalOwnerException, IOException {
		return streamFactory.read(fileManager.claimFile(owner, id));
	}
	
	public OutputStream getOutputStream(FileId id)
			throws IllegalOwnerException, IOException {
		return streamFactory.write(fileManager.claimFile(owner, id));
	}
	
	public boolean isClaimed(FileId id) {
		return fileManager.isClaimedBy(owner, id);
	}
	
	public File fileOf(FileId id) {
		return fileManager.fileOf(owner, id);
	}
	
	
}
