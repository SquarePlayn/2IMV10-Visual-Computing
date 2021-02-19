package nl.tue.visualcomputingproject.group9a.project;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.ChartingModule;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.DiskCacheFileManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy.LRUCachePolicy;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.PreProcessingModule;
import nl.tue.visualcomputingproject.group9a.project.renderer.RendererModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The main class and start point of the application.
 */
public class Main {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	/** The modules of this application. */
	static final Module[] modules = {
		new ChartingModule(),
		new PreProcessingModule(),
		new RendererModule(),
	};
	
	/**
	 * The entrypoint of the application.
	 * 
	 * @param args The commandline arguments.
	 */
	public static void main(String[] args) {
		new Main().run(args);
	}

	/**
	 * The non-static entry point of the application.
	 * 
	 * @param args The commandline arguments.
	 */
	public void run(String[] args) {
		try {
			logger.info("Setting up cache manager...");
			CacheFileManager<File> cacheManager = new DiskCacheFileManager(
				new LRUCachePolicy<>(5 * CachePolicy.SIZE_GB),
				Settings.CACHE_DIR);
			cacheManager.indexCache();
			logger.info("Starting up modules...");
			EventBus bus = new AsyncEventBus(Settings.executorService);
			for (Module mod : modules) {
				mod.startup(bus, cacheManager);
			}
			logger.info("Finished starting up modules!");
		} catch (Exception e) {
			logger.error("An exception happened!", e);
		}
	}
}
