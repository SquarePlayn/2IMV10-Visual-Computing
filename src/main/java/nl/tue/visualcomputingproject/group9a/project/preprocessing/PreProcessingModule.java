package nl.tue.visualcomputingproject.group9a.project.preprocessing;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreProcessingModule implements Module {
	final Logger logger = LoggerFactory.getLogger(PreProcessingModule.class);
	
	@Override
	public void startup(EventBus eventBus) {
		logger.info("Preprocessing starting up!");
	}
}
