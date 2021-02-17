package nl.tue.visualcomputingproject.group9a.project.common.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

@Getter
@AllArgsConstructor
public class CacheFileStreamRequester<T extends FileId> {
	private final Owner owner;
	private final CacheFileManager fileManager;
	private final FileIdFactory<T> idFactory;
	private final FileStreamFactory streamFactory;
	
	@FunctionalInterface
	private interface IOExceptionRunner {
		void run() throws IOException;
	}
	
	@AllArgsConstructor
	private static class PostProcessOutputStream
			extends OutputStream {
		private final OutputStream os;
		private final IOExceptionRunner run;

		@Override
		public void write(int b)
				throws IOException {
			os.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			os.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			os.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			os.flush();
		}

		@Override
		public void close() throws IOException {
			os.close();
			run.run();
		}
	}
	@AllArgsConstructor
	private static class PostProcessInputStream
			extends InputStream {
		private final InputStream is;
		private final IOExceptionRunner run;

		@Override
		public int read(byte[] b)
				throws IOException {
			return is.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len)
				throws IOException {
			return is.read(b, off, len);
		}

		@Override
		public long skip(long n)
				throws IOException {
			return is.skip(n);
		}

		@Override
		public int available()
				throws IOException {
			return is.available();
		}

		@Override
		public void close()
				throws IOException {
			is.close();
			run.run();
		}

		@Override
		public synchronized void mark(int readlimit) {
			is.mark(readlimit);
		}

		@Override
		public synchronized void reset()
				throws IOException {
			is.reset();
		}

		@Override
		public boolean markSupported() {
			return is.markSupported();
		}

		@Override
		public int read()
				throws IOException {
			return is.read();
		}
	}
	
	
	public void open()
			throws IllegalOwnerException {
		fileManager.registerOwnerResolveUncached(owner, idFactory);
	}
	
	public void close()
			throws IllegalOwnerException {
		fileManager.deregisterOwner(owner);
	}
	
	public InputStream getInputStream(FileId id)
			throws IOException {
		return streamFactory.read(fileManager.claimFile(owner, id));
	}
	
	public InputStream getInputStreamReleaseOnClose(FileId id)
			throws IOException {
		return new PostProcessInputStream(
				streamFactory.read(fileManager.claimFile(owner, id)),
				() -> release(id)
		);
	}
	
	public OutputStream getOutputStream(FileId id)
			throws IOException {
		File dstFile = fileManager.claimFile(owner, id);
		File tmpFile = fileManager.tmpFileOf(owner, id);
		return new PostProcessOutputStream(
				streamFactory.write(tmpFile),
				() -> Files.move(tmpFile.toPath(), dstFile.toPath())
		);
	}
	
	public OutputStream claimAndGetOutputStream(FileId id)
			throws IOException {
		claim(id);
		return getOutputStream(id);
	}

	public boolean isCached(FileId id) {
		return fileManager.fileOf(owner, id).exists();
	}
	
	public boolean isClaimed(FileId id) {
		return fileManager.isClaimed(owner, id);
	}
	
	public File claim(FileId id) {
		return fileManager.claimFile(owner, id);
	}
	
	public void release(FileId id) {
		return; // TODO
	}
	
	public File fileOf(FileId id) {
		return fileManager.fileOf(owner, id);
	}
	
	
}
