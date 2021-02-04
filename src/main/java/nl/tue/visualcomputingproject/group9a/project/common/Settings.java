package nl.tue.visualcomputingproject.group9a.project.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Global project settings class.
 */
public final class Settings {
	
	// Disallow instantiation.
	private Settings() {
	}

	/**
	 * Number of dedicated threads reserved by various modules in the application.
	 * Reserved by:
	 * - Rendering: 1
	 * - Chart: 1
	 */
	public static final int NUM_DEDICATED_THREADS = 2;
	
	/** The service used for scheduling tasks. */
	static final ExecutorService executorService = Executors.newFixedThreadPool(
			Math.max(1, Runtime.getRuntime().availableProcessors() - Settings.NUM_DEDICATED_THREADS));
	
	
}
