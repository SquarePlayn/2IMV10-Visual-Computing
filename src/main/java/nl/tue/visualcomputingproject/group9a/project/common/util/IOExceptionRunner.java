package nl.tue.visualcomputingproject.group9a.project.common.util;

import java.io.IOException;

@FunctionalInterface
public interface IOExceptionRunner {
	
	void run() throws IOException;
	
}
