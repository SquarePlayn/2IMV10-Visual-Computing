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
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkRequestedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class for the pre-processing module.
 */
@SuppressWarnings("UnstableApiUsage")
public class PreProcessingModule
		implements Module {
	/** The logger of this class. */
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	/** The event bus used in the application. */
	private EventBus eventBus; // TODO maybe store this in {@link Settings}?

	/** The cache manager used to store the mesh chunk data. */
	CacheManager<ChunkId, MeshChunkData> cache;
	
	/** The current pending chunks in the renderer. */
	private Collection<ChunkId> pendingChunks;
	/** The current loaded chunks in the renderer. */
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
		cache.indexDiskCache();
	}

	/**
	 * Method handling the renderer status events.
	 * 
	 * @param e The renderer status event.
	 */
	@Subscribe
	public void rendererStatus(final RendererChunkStatusEvent e) {
		pendingChunks = e.getPendingChunks();
		loadedChunks = e.getLoadedChunks();
		
		if (!e.getNewChunks().isEmpty()) {
			Settings.executorService.submit(() -> {
				List<ChunkId> diskCached = new ArrayList<>();
				List<ChunkId> request = new ArrayList<>();
				
				// First return the chunks which are already in memory.
				for (ChunkId id : e.getNewChunks()) {
					MeshChunkData data;
					if ((data = cache.getFromMemory(id)) != null) {
						eventBus.post(new ProcessorChunkLoadedEvent(
								new Chunk<>(id, data)));
						
					} else if (cache.isDiskCached(id)) {
						diskCached.add(id);
					} else {
						request.add(id);
					}
				}
				
				// Then request the missing chunks from the chart module.
				if (!request.isEmpty()) {
					eventBus.post(new ProcessorChunkRequestedEvent(request));
					request = new ArrayList<>();
				}
				
				// Then load the chunks from disk.
				for (ChunkId id : diskCached) {
					MeshChunkData data = cache.get(id);
					if (data == null) {
						request.add(id);
					} else {
						eventBus.post(new ProcessorChunkLoadedEvent(
								new Chunk<>(id, data)));
					}
				}
				
				// Finally request any unexpected cache-misses from the chart module.
				if (!request.isEmpty()) {
					eventBus.post(new ProcessorChunkRequestedEvent(request));
				}
			});
		}
		
		if (!e.getUnloadedChunks().isEmpty()) {
			for (ChunkId id : e.getUnloadedChunks()) {
				cache.removeMemoryCache(id);
			}
		}
	}

	/**
	 * Method handling the chunk loaded event.
	 * 
	 * @param e The chunk loaded event.
	 */
	@Subscribe
	public void chunkLoaded(ChartChunkLoadedEvent e) {
		final ChunkId id = e.getChunk().getChunkId();
		// Ignore event since the chunk is not needed anymore.
		if (!pendingChunks.contains(id) &&
				!loadedChunks.contains(id)) {
			// TODO: maybe store raw data?
			return;
		}
		
		// Check if the chunk is already cached.
		if (cache.isCached(id)) {
			MeshChunkData data = cache.get(id);
			if (data != null) {
				eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
						e.getChunk().getPosition(),
						e.getChunk().getQualityLevel(),
						cache.get(id))));
				return;
			}
		}
		
		// Pre-process the chunk.
		Settings.executorService.submit(() -> {
			Generator<PointCloudChunkData> gen = Generator
					.createGeneratorFor(e.getChunk().getQualityLevel());
			MeshChunkData data = gen.generateChunkData(e.getChunk());
			eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
					e.getChunk().getChunkId(),
					data)));
			cache.put(id, data);
		});
	}
	
}
