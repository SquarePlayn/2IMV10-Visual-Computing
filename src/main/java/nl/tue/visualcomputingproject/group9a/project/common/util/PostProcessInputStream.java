package nl.tue.visualcomputingproject.group9a.project.common.util;


import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

@AllArgsConstructor
public class PostProcessInputStream
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
