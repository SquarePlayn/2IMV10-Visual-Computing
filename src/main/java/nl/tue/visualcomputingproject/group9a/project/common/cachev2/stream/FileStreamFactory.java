package nl.tue.visualcomputingproject.group9a.project.common.cachev2.stream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A factory for creating input- and output streams for files.
 */
public interface FileStreamFactory {

	/**
	 * @param file The file to create the output stream for.
	 * 
	 * @return A new output stream for the given file to write to.
	 * 
	 * @throws IOException If some IO exception occurs.
	 */
	OutputStream write(File file)
			throws IOException;

	/**
	 * @param file the file to create the input stream for.
	 *    
	 * @return a new input stream fo the given file to read from.
	 * 
	 * @throws IOException If some IO exception occurs.
	 */
	InputStream read(File file)
			throws IOException;
	
}
