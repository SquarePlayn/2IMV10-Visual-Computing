package nl.tue.visualcomputingproject.group9a.project.chart.extractor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.chart.MapSheetCacheManager;
import nl.tue.visualcomputingproject.group9a.project.chart.events.ExtractionRequestEvent;
import nl.tue.visualcomputingproject.group9a.project.chart.events.PartialChunkAvailableEvent;
import nl.tue.visualcomputingproject.group9a.project.common.Point;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointCloudChunkData;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("UnstableApiUsage")
public class Extractor {
	/**
	 * The logger of this class.
	 */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final EventBus eventBus;
	private final MapSheetCacheManager cacheManager;
	
	public Extractor(EventBus eventBus, MapSheetCacheManager cacheManager) {
		this.eventBus = eventBus;
		this.cacheManager = cacheManager;
		eventBus.register(this);
		logger.info("Extractor is ready!");
	}
	
	public List<Chunk<PointCloudChunkData>> handleGeotiffFile(ExtractionRequestEvent event) throws IOException, TransformException {
		List<Chunk<PointCloudChunkData>> chunks = new ArrayList<>();
		
		for (ChunkPosition pos : event.getPositions()) {
			chunks.add(new Chunk<>(pos, event.getLevel(), new PointCloudChunkData()));
		}
		
		try (InputStream inputStream = event.getClaim().getInputStream()) {
			ZipInputStream zipInputStream = new ZipInputStream(inputStream);
			ZipEntry entry = zipInputStream.getNextEntry();
			logger.info("Zip contents: {}", entry);
			
			GeoTiffReader reader = new GeoTiffReader(zipInputStream);
			GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
			
			//Assumption: CRS of the geotiff is EPSG:28992
			//If not, useful link: https://gis.stackexchange.com/questions/278350/obtaining-longitude-and-latitude-from-geotiff-with-geotools
			
			long count = 0;
			for (Chunk<PointCloudChunkData> chunk : chunks) {
				DirectPosition2D bl = new DirectPosition2D(chunk.getPosition().getX(), chunk.getPosition().getY());
				DirectPosition2D tr = new DirectPosition2D(chunk.getPosition().getX() + chunk.getPosition().getWidth(), chunk.getPosition().getY() + chunk.getPosition().getHeight());
				GridCoordinates2D blg = coverage.getGridGeometry().worldToGrid(bl);
				GridCoordinates2D trg = coverage.getGridGeometry().worldToGrid(tr);
				GridEnvelope2D range = coverage.getGridGeometry().getGridRange2D();
				logger.info("Chunk: {} {} {} {}", bl, tr, blg, trg);
				
				for (int i = (int) Math.max(blg.getX(), range.getLow(0)); i < Math.min(trg.getX(), range.getHigh(0)); i++) {
					for (int j = (int) Math.max(trg.getY(), range.getLow(1)); j < Math.min(blg.getY(), range.getHigh(1)); j++) {
						GridCoordinates2D coord = new GridCoordinates2D(i, j);
						DirectPosition p = coverage.getGridGeometry().gridToWorld(coord);
						
						double[] vals = new double[1];
						coverage.evaluate(p, vals);
						double x = p.getOrdinate(0);
						double y = p.getOrdinate(1);
						
						chunk.getData().addPoint(x, y, vals[0]);
						count++;
					}
				}
			}
			
			logger.info("Extracted {} points into {} chunks.", count, chunks.size());
			
			return chunks;
		}
	}
	
	@Subscribe
	public void request(ExtractionRequestEvent event) throws IOException, TransformException {
		logger.info("Extracting {}...", event);
		
		List<Chunk<PointCloudChunkData>> chunks = new ArrayList<>();
		switch (event.getLevel()) {
			case FIVE_BY_FIVE:
			case HALF_BY_HALF:
				chunks = handleGeotiffFile(event);
				break;
			case LAS:
				throw new UnsupportedOperationException("Unimplemented: LAZ");
		}
		
		for (Chunk<PointCloudChunkData> chunk : chunks) {
			eventBus.post(new PartialChunkAvailableEvent(chunk, event.getSheet()));
		}
		
		cacheManager.releaseClaim(event.getClaim());
	}
}