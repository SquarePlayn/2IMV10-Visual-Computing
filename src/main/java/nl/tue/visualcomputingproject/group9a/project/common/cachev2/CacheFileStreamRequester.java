package nl.tue.visualcomputingproject.group9a.project.common.cachev2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.FileStreamFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

@Deprecated
@Getter
@AllArgsConstructor
public class CacheFileStreamRequester<T extends FileId> {
	private final DiskCacheFileManager fileManager;
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
		public void write(byte[] b)
				throws IOException {
			os.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len)
				throws IOException {
			os.write(b, off, len);
		}

		@Override
		public void flush()
				throws IOException {
			os.flush();
		}

		@Override
		public void close()
				throws IOException {
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
	
	public InputStream getInputStream(T id)
			throws IOException {
		return streamFactory.read(fileManager.claimFile(id));
	}
	
	public InputStream getInputStreamReleaseOnClose(T id)
			throws IOException {
		return new PostProcessInputStream(
				streamFactory.read(fileManager.claimFile(id)),
				() -> release(id)
		);
	}
	
	public OutputStream getOutputStream(T id)
			throws IOException {
		File dstFile = fileManager.claimFile(id);
		File tmpFile = fileManager.tmpFileOf(id);
		return new PostProcessOutputStream(
				streamFactory.write(tmpFile),
				() -> Files.move(tmpFile.toPath(), dstFile.toPath())
		);
	}
	
	public OutputStream claimAndGetOutputStream(T id)
			throws IOException {
		claim(id);
		return getOutputStream(id);
	}

	public boolean isCached(T id) {
		return fileManager.exists(id);
	}
	
	public boolean isClaimed(T id) {
		return fileManager.isClaimed(id);
	}
	
	public File claim(T id) {
		return fileManager.claimFile(id);
	}
	
	public void release(T id) {
		fileManager.releaseFile(id);
	}
	
	public File fileOf(T id) {
		return fileManager.fileOf(id);
	}
	
	
}
