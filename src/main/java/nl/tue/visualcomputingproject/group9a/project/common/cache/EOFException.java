package nl.tue.visualcomputingproject.group9a.project.common.cache;

import java.io.IOException;

public class EOFException
		extends IOException {
	
	public EOFException(String reason) {
		super(reason);
	}
	
	public EOFException(int expected, int got) {
		super("Expected " + expected + " bytes, but got " + got);
	}
	
}
