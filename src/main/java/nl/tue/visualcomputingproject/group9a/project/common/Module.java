package nl.tue.visualcomputingproject.group9a.project.common;

import com.google.common.eventbus.EventBus;

/**
 * Module component interface.
 */
public interface Module {

	/**
	 * Starts the module.
	 * 
	 * @param eventBus The event bus used in the system.
	 */
	void startup(EventBus eventBus);
	
}
