package nl.tue.visualcomputingproject.group9a.project.chart;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.chart.assembly.ChunkAssemblyManager;
import nl.tue.visualcomputingproject.group9a.project.chart.download.DownloadManager;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.WFSApi;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkRequestedEvent;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class LookupManager {
	/**
	 * The logger of this class.
	 */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final EventBus eventBus;
	private final DownloadManager downloadManager;
	private final ChunkAssemblyManager assemblyManager;
	@Getter
	private final WFSApi api = new WFSApi();
	
	public LookupManager(EventBus eventBus, DownloadManager downloadManager, ChunkAssemblyManager assemblyManager) throws IOException {
		this.eventBus = eventBus;
		this.downloadManager = downloadManager;
		this.assemblyManager = assemblyManager;
		eventBus.register(this);
		logger.info("Lookup manager is ready!");
	}
	
	@Subscribe
	public void onChunkRequest(ProcessorChunkRequestedEvent event) {
		Settings.executorService.submit(() -> {
			try {
				Collection<MapSheet> sheets = api.query(event.getNewChunksRequested()
						.stream()
						.map(ChunkId::getPosition)
						.collect(Collectors.toSet()));

				logger.info("API returned {} sheets!", sheets.size());

				//Assemble the chunk sheets to prepare the chunk assembly manager.
				Map<ChunkId, List<MapSheet>> chunkSheets = new HashMap<>();

				//Iterate over and collect the sheet requests from the sheets.
				Map<MapSheet, Map<QualityLevel, List<ChunkPosition>>> sheetRequests = new HashMap<>();
				for (MapSheet sheet : sheets) {
					Map<QualityLevel, List<ChunkPosition>> requests = new HashMap<>();
					for (ChunkId requestedChunk : event.getNewChunksRequested()) {
						Geometry chunkGeom = requestedChunk.getPosition().getJtsGeometry(api.getGeometryFactory(), api.getCrs());
						if (chunkGeom.intersects(sheet.getGeom())) {
							//We now know that requestedChunk needs data from sheet.
							//Make sure we cover both the requested and higher qualities.
							QualityLevel q = requestedChunk.getQuality();
							while (q.getOrder() <= Settings.MAX_DOWNLOAD_QUALITY.getOrder()) {
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

								if (q != QualityLevel.getBest()) {
									q = q.next();
								} else {
									break;
								}
							}
						}
					}
					sheetRequests.put(sheet, requests);
				}

				logger.info("Going to request {} chunks!", chunkSheets.size());

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
			} catch (FactoryException | IOException e) {
				e.printStackTrace();
			}
		});
	}
}
