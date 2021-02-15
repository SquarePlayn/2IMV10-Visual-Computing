package nl.tue.visualcomputingproject.group9a.project.common;

import com.google.common.eventbus.EventBus;

import java.io.IOException;

/**
 * Module component interface.
 */
public interface Module {

	/**
	 * Starts the module.
	 * 
	 * @param eventBus The event bus used in the system.
	 */
	void startup(EventBus eventBus) throws IOException;
	
}
