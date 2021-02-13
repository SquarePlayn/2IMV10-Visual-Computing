package nl.tue.visualcomputingproject.group9a.project.preprocessing;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointCloudChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

	CacheManager<ChunkId, MeshChunkData> cache;
	
	private Collection<ChunkId> pendingChunks;
	private Collection<ChunkId> loadedChunks;
	
	@Override
	public void startup(EventBus eventBus) {
		logger.info("Preprocessing starting up!");
		this.eventBus = eventBus;
		eventBus.register(this);
		cache = new CacheManager<>(
				Settings.CACHE_DIR,
				ChunkId.createCacheNameFactory("mesh_data" + File.separator),
				MeshChunkData.createCacheFactory());
	}
	
	@Subscribe
	public void rendererStatus(RendererChunkStatusEvent e) {
		pendingChunks = e.getPendingChunks();
		loadedChunks = e.getLoadedChunks();
	}
	
	@Subscribe
	public void chunkLoaded(ChartChunkLoadedEvent e) {
		final ChunkId key = e.getChunk().getChunkId();
		if (!pendingChunks.contains(key) &&
				!loadedChunks.contains(key)) {
			// Ignore event since the chunk is not needed anymore.
			return;
		}
		
		if (cache.isCached(key)) {
			MeshChunkData data = cache.get(key);
			if (data != null) {
				eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
						e.getChunk().getPosition(),
						e.getChunk().getQualityLevel(),
						cache.get(key))));
				return;
			}
		}
		
		Settings.executorService.submit(() -> {
			Generator<PointCloudChunkData> gen = Generator
					.createGeneratorFor(e.getChunk().getQualityLevel());
			MeshChunkData data = gen.generateChunkData(e.getChunk());
			eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
					e.getChunk().getPosition(),
					e.getChunk().getQualityLevel(),
					data)));
			cache.put(key, data);
		});
	}
	
}
