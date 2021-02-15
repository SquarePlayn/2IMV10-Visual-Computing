package nl.tue.visualcomputingproject.group9a.project.chart;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.chart.assembly.ChunkAssemblyManager;
import nl.tue.visualcomputingproject.group9a.project.chart.download.DownloadManager;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.WFSApi;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkRequestedEvent;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LookupManager {
	/**
	 * The logger of this class.
	 */
	static final Logger logger = LoggerFactory.getLogger(LookupManager.class);
	
	private final EventBus eventBus;
	private final MapSheetCacheManager cacheManager;
	private final DownloadManager downloadManager;
	private final ChunkAssemblyManager assemblyManager;
	private final WFSApi api = new WFSApi();
	
	public LookupManager(EventBus eventBus, MapSheetCacheManager cacheManager, DownloadManager downloadManager, ChunkAssemblyManager assemblyManager) throws IOException {
		this.eventBus = eventBus;
		this.cacheManager = cacheManager;
		this.downloadManager = downloadManager;
		this.assemblyManager = assemblyManager;
		eventBus.register(this);
		logger.info("Lookup manager is ready!");
	}
	
	@Subscribe
	public void onChunkRequest(ProcessorChunkRequestedEvent event) throws FactoryException, IOException {
		Collection<MapSheet> sheets = api.query(event.getNewChunksRequested()
			.stream()
			.map(ChunkId::getPosition)
			.collect(Collectors.toSet()));
		
		//Assemble the chunk sheets to prepare the chunk assembly manager.
		Map<ChunkId, List<MapSheet>> chunkSheets = new HashMap<>();
		
		//Iterate over and collect the sheet requests from the sheets.
		Map<MapSheet, Map<QualityLevel, List<ChunkPosition>>> sheetRequests = new HashMap<>();
		for (MapSheet sheet : sheets) {
			Map<QualityLevel, List<ChunkPosition>> requests = new HashMap<>();
			for (ChunkId requestedChunk : event.getNewChunksRequested()) {
				if (requestedChunk.getPosition().getJtsGeometry(api.getGeometryFactory()).overlaps(sheet.getGeom())) {
					//We now know that requestedChunk needs data from sheet.
					
					//Make sure we cover both the requested and higher qualities.
					QualityLevel q = requestedChunk.getQuality();
					while (q != QualityLevel.getBest()) {
						//Make sure the chunk will get registered with the assembly manager.
						ChunkId newChunkId = new ChunkId(requestedChunk.getPosition(), q);
						List<MapSheet> c = chunkSheets.getOrDefault(newChunkId, new ArrayList<>());
						c.add(sheet);
						chunkSheets.put(newChunkId, c);
						
						//Register the chunk with the requests.
						if (requests.containsKey(q)) {
							requests.get(q).add(requestedChunk.getPosition());
						} else {
							List<ChunkPosition> l = new ArrayList<>();
							l.add(requestedChunk.getPosition());
							requests.put(q, l);
						}
						
						q = q.next();
					}
				}
			}
			sheetRequests.put(sheet, requests);
		}
		
		//Register these chunks with the assembly manager.
		for (Map.Entry<ChunkId, List<MapSheet>> e : chunkSheets.entrySet()) {
			assemblyManager.assembleChunkRequest(e.getKey(), e.getValue());
		}
		
		//Iterate over the requests and either send them off for extraction or send it to the download manager.
		for (Map.Entry<MapSheet, Map<QualityLevel, List<ChunkPosition>>> r : sheetRequests.entrySet()) {
			MapSheet sheet = r.getKey();
			Map<QualityLevel, List<ChunkPosition>> requests = r.getValue();
			
			for (Map.Entry<QualityLevel, List<ChunkPosition>> e : requests.entrySet()) {
				QualityLevel qualityLevel = e.getKey();
				List<ChunkPosition> positions = e.getValue();
				
				if (!positions.isEmpty()) {
					//See the commends in requestDownload as to why we always request a dl
					// and never send events directly to the extractor.
					downloadManager.requestDownload(sheet, positions, qualityLevel);
				}
			}
		}
	}
}
