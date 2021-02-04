package nl.tue.visualcomputingproject.group9a.project.renderer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class for the rendering module.
 */
public class RendererModule
		extends Thread
		implements Module {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(RendererModule.class);
	/** Message queue from the event queue. */ 
	final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();
	
	
	@Override
	public void startup(EventBus eventBus) {
		logger.info("Rendering starting up!");
		this.start();
		eventBus.register(this);
	}
	
	@Override
	public void run() {
		// Here is your thread
	}
	
	@Subscribe
	public void someEvent(String event) {
		messages.add(event);
	}
}
