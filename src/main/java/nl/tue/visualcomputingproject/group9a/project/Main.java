package nl.tue.visualcomputingproject.group9a.project;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.ChartingModule;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.PreProcessingModule;
import nl.tue.visualcomputingproject.group9a.project.renderer.RendererModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	final Logger logger = LoggerFactory.getLogger(Main.class);
	
	final Module[] modules = {
		new ChartingModule(),
		new PreProcessingModule(),
		new RendererModule(),
	};
	
	public static void main(String[] args) {
		(new Main()).run(args);
	}
	
	public void run(String[] args) {
		logger.info("Starting up!");
		EventBus bus = new EventBus();
		for(Module mod : modules) {
			mod.startup(bus);
		}
		logger.info("Startup done!");
	}
}
