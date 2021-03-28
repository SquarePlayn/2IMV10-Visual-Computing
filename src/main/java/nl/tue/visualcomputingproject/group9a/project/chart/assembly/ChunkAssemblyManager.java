package nl.tue.visualcomputingproject.group9a.project.chart.assembly;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.chart.ChartingModule;
import nl.tue.visualcomputingproject.group9a.project.chart.events.PartialChunkAvailableEvent;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointCloudChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartChunkLoadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class ChunkAssemblyManager {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final Map<ChunkId, ChunkAssemblyJob> assemblyRequests = new ConcurrentHashMap<>();
	private final EventBus eventBus;
	
	public ChunkAssemblyManager(EventBus eventBus) {
		this.eventBus = eventBus;
		eventBus.register(this);
		logger.info("Chunk assembly manager ready!");
	}
	
	public void assembleChunkRequest(final ChunkId chunk, Collection<MapSheet> sheets) {
		assemblyRequests.compute(chunk, (key, old) -> {
			if (old != null) {
				throw new IllegalStateException("Attempted to request assembly of chunk that's already being assembled.");
			}
			return new ChunkAssemblyJob(chunk, new HashSet<>(sheets));
		});
//		synchronized (assemblyRequests) {
//			if (assemblyRequests.containsKey(chunk)) {
//				throw new IllegalStateException("Attempted to request assembly of chunk that's already being assembled.");
//			}
//			
//			ChunkAssemblyJob job = new ChunkAssemblyJob(chunk, new HashSet<>(sheets));
//			assemblyRequests.put(chunk, job);
//			
//			logger.info("New assembly request! {}", job);
//		}
	}
	
	@Subscribe
	public void partialChunkAvailable(PartialChunkAvailableEvent event) {
		// Retrieve the job.
		ChunkAssemblyJob job = assemblyRequests.get(event.getChunk().getChunkId());
		if (job == null) {
			throw new IllegalStateException("Chunk data provided for job that is not in progress!");
		}
		logger.info("Partial chunk received for chunk {}.", job.getChunkId());
		
		// Add the sub-chunk to the job, and remove
		// the job from the map if it is ready for assembly.
		// Notice that a job cannot be removed if it has pending chunks,
		// i.e. only the last partial chunk event can remove it.
		boolean isReadyForAssembly = (assemblyRequests.computeIfPresent(event.getChunk().getChunkId(), (id, j) -> {
			j.newPartialChunk(event.getChunk(), event.getSheet());
			return (j.isReadyForAssembly() ? null : j);
		}) == null);
		
		// Do assembly / posting only if needed.
		if (!isReadyForAssembly) return;
		
		Settings.executorService.submit(() -> {
			logger.info("Chunk {} is ready for assembly!", job.getChunkId());
			
			Chunk<ChunkId, PointCloudChunkData> assembledChunk = event.getChunk();
			
			if (job.getNumberOfPartialChunks() > 1) {
				// This chunk needs assembly.
				assembledChunk = job.assembleChunk();
			}
			
			eventBus.post(new ChartChunkLoadedEvent(assembledChunk));
		});
	}
	
	
}
