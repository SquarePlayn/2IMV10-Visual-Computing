package nl.tue.visualcomputingproject.group9a.project.common.util;


import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;

@AllArgsConstructor
public class PostProcessOutputStream
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
