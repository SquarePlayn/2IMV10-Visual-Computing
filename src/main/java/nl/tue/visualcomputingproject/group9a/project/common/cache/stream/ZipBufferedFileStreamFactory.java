package nl.tue.visualcomputingproject.group9a.project.common.cache.stream;


import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class ZipBufferedFileStreamFactory
		implements FileStreamFactory {
	@Override
	public OutputStream write(File file)
			throws IOException {
		return new DeflaterOutputStream(new FileOutputStream(file, false));
	}

	@Override
	public InputStream read(File file)
			throws IOException {
		return new InflaterInputStream(new FileInputStream(file));
	}
}
