package nl.tue.visualcomputingproject.group9a.project.common.old_cache;

import java.io.IOException;

/**
 * Exception thrown when there is a mismatch between the amount of
 * data expected and found during IO operations.
 */
public class EOFException
		extends IOException {

	/**
	 * Creates a new exception using the given message.
	 * 
	 * @param reason The message of the exception.
	 */
	public EOFException(String reason) {
		super(reason);
	}

	/**
	 * Creates a new exception which specifies the amount of expected and found bytes.
	 * 
	 * @param expected The amount of expected bytes.
	 * @param got      The amount of found bytes.
	 */
	public EOFException(int expected, int got) {
		super("Expected " + expected + " bytes, but got " + got);
	}
	
}
