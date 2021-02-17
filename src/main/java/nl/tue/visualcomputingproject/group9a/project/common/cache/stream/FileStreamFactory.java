package nl.tue.visualcomputingproject.group9a.project.common.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileStreamFactory {
	
	OutputStream write(File file)
			throws IOException;
	
	InputStream read(File file)
			throws IOException;
	
}
