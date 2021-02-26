package nl.tue.visualcomputingproject.group9a.project.chart.assembly;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.chart.ChartingModule;
import nl.tue.visualcomputingproject.group9a.project.chart.events.PartialChunkAvailableEvent;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
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

@SuppressWarnings("UnstableApiUsage")
public class ChunkAssemblyManager {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final Map<ChunkId, ChunkAssemblyJob> assemblyRequests = new HashMap<>();
	private final EventBus eventBus;
	
	public ChunkAssemblyManager(EventBus eventBus) {
		this.eventBus = eventBus;
		eventBus.register(this);
		logger.info("Chunk assembly manager ready!");
	}
	
	public void assembleChunkRequest(ChunkId chunk, Collection<MapSheet> sheets) {
		synchronized (assemblyRequests) {
			if (assemblyRequests.containsKey(chunk)) {
				throw new IllegalStateException("Attempted to request assembly of chunk that's already being assembled.");
			}
			
			ChunkAssemblyJob job = new ChunkAssemblyJob(chunk, new HashSet<>(sheets));
			assemblyRequests.put(chunk, job);
		}
	}
	
	@Subscribe
	public void partialChunkAvailable(PartialChunkAvailableEvent event) {
		ChunkAssemblyJob job;
		synchronized (assemblyRequests) {
			if (!assemblyRequests.containsKey(event.getChunk().getChunkId())) {
				throw new IllegalStateException("Chunk data provided for job that is not in progress!");
			}
			
			//Lookup the job.
			job = assemblyRequests.get(event.getChunk().getChunkId());
			
			logger.info("Partial chunk received for chunk {}.", job.getChunkId());
			
			//Register the partial chunk with the job.
			job.newPartialChunk(event.getChunk(), event.getSheet());
			
			//If we can assemble this now, remove it from the map.
			if (job.isReadyForAssembly()) {
				assemblyRequests.remove(event.getChunk().getChunkId());
			}
		}
		
		//Do assembly/posting if needed.
		if (job.isReadyForAssembly()) {
			logger.info("Chunk {} is ready for assembly!", job.getChunkId());
			
			Chunk<PointCloudChunkData> assembledChunk = event.getChunk();
			
			if (job.getNumberOfPartialChunks() > 1) {
				//This chunk needs assembly.
				assembledChunk = job.assembleChunk();
			}
			
			eventBus.post(new ChartChunkLoadedEvent(assembledChunk));
		}
	}
	
	
}
