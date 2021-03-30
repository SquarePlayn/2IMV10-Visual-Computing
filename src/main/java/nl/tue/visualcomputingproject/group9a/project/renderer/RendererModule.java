package nl.tue.visualcomputingproject.group9a.project.renderer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.SneakyThrows;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager.ChunkManager;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Light;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.SwingWindow;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Loader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Skybox;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.render.Renderer;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.SkyboxShader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.StaticShader;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;

/**
 * Class for the rendering module.
 */
@SuppressWarnings("UnstableApiUsage")
public class RendererModule
		extends Thread
		implements Module {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private EventBus eventBus;
	private SwingWindow window;
	
	public RendererModule() {
		super("render-thread");
	}
	
	@SneakyThrows
	@Override
	public void startup(EventBus eventBus, CachePolicy diskPolicy, CachePolicy memoryPolicy) {
		LOGGER.info("Rendering starting up!");
		this.eventBus = eventBus;
		window = new SwingWindow(eventBus);
		window.getCanvas().setBackgroundColor(new Vector3f(1.0f, 0.0f, 0.0f));
	}
	
}
