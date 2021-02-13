package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import java.nio.Buffer;

public interface BufferManager {
	
	Buffer finalizeBuffer();
	
	int size();
	
}
