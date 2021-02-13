package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public interface BufferManager {
	
	ByteBuffer finalizeBuffer();
	
	int size();
	
}
