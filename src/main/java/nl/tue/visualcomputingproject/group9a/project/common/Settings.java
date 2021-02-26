package nl.tue.visualcomputingproject.group9a.project.common;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Global project settings class.
 */
public final class Settings {

	// Disallow instantiation.
	private Settings() {
	}
	
	public static final int CHUNK_WIDTH = 100;
	public static final int CHUNK_HEIGHT = 100;

	/**
	 * Number of dedicated threads reserved by various modules in the application.
	 * Reserved by:
	 * - Rendering: 1
	 * - Chart: 1
	 */
	public static final int NUM_DEDICATED_THREADS = 2;
	
	/** The service used for scheduling tasks. */
	public static final ExecutorService executorService = Executors.newFixedThreadPool(
			Math.max(1, Runtime.getRuntime().availableProcessors() - Settings.NUM_DEDICATED_THREADS),
			r -> {
				Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setDaemon(true);
				return t;
			});
	
	/** The directory used for caching. */
	public static File CACHE_DIR = new File("cache");
	/** The file extension of the cache files. */
	public static final String CACHE_EXT = ".cache";
	/** The file extension of the temporary cache files. */
	public static final String TMP_CACHE_EXT = ".part";
	
	/**
	 * The maximum quality of map sheets the chart module will download.
	 */
	public static final QualityLevel MAX_DOWNLOAD_QUALITY = QualityLevel.FIVE_BY_FIVE;
}
