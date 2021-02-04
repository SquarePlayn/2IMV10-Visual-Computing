package nl.tue.visualcomputingproject.group9a.project.preprocessing;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for the pre-processing module.
 */
public class PreProcessingModule
		implements Module {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(PreProcessingModule.class);
	
	@Override
	public void startup(EventBus eventBus) {
		logger.info("Preprocessing starting up!");
	}
}
