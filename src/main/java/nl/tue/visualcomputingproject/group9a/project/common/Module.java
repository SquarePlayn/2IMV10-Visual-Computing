package nl.tue.visualcomputingproject.group9a.project.common;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileManager;

import java.io.File;
import java.io.IOException;

/**
 * Module component interface.
 */
public interface Module {

	/**
	 * Starts the module.
	 *
	 * @param eventBus The event bus used in the system.
	 * @param cacheManager
	 */
	void startup(EventBus eventBus, CacheFileManager<File> cacheManager) throws IOException;
	
}
