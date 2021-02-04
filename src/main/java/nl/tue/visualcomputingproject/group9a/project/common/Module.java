package nl.tue.visualcomputingproject.group9a.project.common;

import com.google.common.eventbus.EventBus;

public interface Module {
	void startup(EventBus eventBus);
}
