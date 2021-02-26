package nl.tue.visualcomputingproject.group9a.project.common;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;

import java.io.IOException;

/**
 * Module component interface.
 */
@SuppressWarnings("UnstableApiUsage")
public interface Module {

	/**
	 * Starts the module.
	 *
	 * @param eventBus     The event bus used in the system.
	 * @param diskPolicy   The policy used for the disk cache.
	 * @param memoryPolicy The policy used for the memory cache.
	 */
	void startup(EventBus eventBus, CachePolicy diskPolicy, CachePolicy memoryPolicy)
			throws IOException;
	
}
