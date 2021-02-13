package nl.tue.visualcomputingproject.group9a.project.preprocessing;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointCloudChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 * Class for the pre-processing module.
 */
@SuppressWarnings("UnstableApiUsage")
public class PreProcessingModule
		implements Module {
	/** The logger of this class. */
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private EventBus eventBus;
	
	private Collection<ChunkPosition> pendingChunks;
	private Collection<ChunkPosition> loadedChunks;
	
	@Override
	public void startup(EventBus eventBus) {
		logger.info("Preprocessing starting up!");
		this.eventBus = eventBus;
		eventBus.register(this);
	}
	
	@Subscribe
	public void rendererStatus(RendererChunkStatusEvent e) {
		pendingChunks = e.getPendingChunks();
		loadedChunks = e.getLoadedChunks();
	}
	
	@Subscribe
	public void chunkLoaded(ChartChunkLoadedEvent e) {
		if (!pendingChunks.contains(e.getChunk().getPosition()) &&
				!loadedChunks.contains(e.getChunk().getPosition())) {
			// Ignore event since the chunk is not needed anymore.
			return;
		}
		
		Settings.executorService.submit(() -> {
			Generator<PointCloudChunkData> gen = Generator
					.createGeneratorFor(e.getChunk().getQualityLevel());
			MeshChunkData data = gen.generateChunkData(e.getChunk());
			eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
					e.getChunk().getPosition(),
					e.getChunk().getQualityLevel(),
					data)));
			// TODO: cache
		});
	}
	
}
