package nl.tue.visualcomputingproject.group9a.project.common.cache.stream;

import java.io.*;

/**
 * Simple buffered file stream factory.
 */
public class BufferedFileStreamFactory
		implements FileStreamFactory {
	@Override
	public OutputStream write(File file)
			throws IOException {
		return new BufferedOutputStream(new FileOutputStream(file, false));
	}

	@Override
	public InputStream read(File file)
			throws IOException {
		return new BufferedInputStream(new FileInputStream(file));
	}
}
