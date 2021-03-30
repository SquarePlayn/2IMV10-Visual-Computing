package nl.tue.visualcomputingproject.group9a.project.chart.extractor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.chart.MapSheetCacheManager;
import nl.tue.visualcomputingproject.group9a.project.chart.events.ExtractionRequestEvent;
import nl.tue.visualcomputingproject.group9a.project.chart.events.PartialChunkAvailableEvent;
import nl.tue.visualcomputingproject.group9a.project.common.Point;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
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

import java.awt.geom.Point2D;
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
	
	public void handleGeotiffFile(ExtractionRequestEvent event) throws IOException, TransformException {
		try (InputStream inputStream = event.getClaim().getInputStream()) {
			ZipInputStream zipInputStream = new ZipInputStream(inputStream);
			ZipEntry entry = zipInputStream.getNextEntry();
			logger.info("Zip contents: {}", entry);
			
			GeoTiffReader reader = new GeoTiffReader(zipInputStream);
			GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
			
			//Assumption: CRS of the geotiff is EPSG:28992
			//If not, useful link: https://gis.stackexchange.com/questions/278350/obtaining-longitude-and-latitude-from-geotiff-with-geotools
			
			logger.info("Sheet info: {}x{} - {}x{}", coverage.getEnvelope2D().getMinX(), coverage.getEnvelope2D().getMinY(), coverage.getEnvelope2D().getMaxX(), coverage.getEnvelope2D().getMaxY());
			
			long count = 0;
			double[] vals = new double[1];
			double[] coord = new double[coverage.getGridGeometry().getGridToCRS2D().getSourceDimensions()];
			double[] p = new double[coverage.getGridGeometry().getGridToCRS2D().getTargetDimensions()];
			GridCoordinates2D c = new GridCoordinates2D();
			for (ChunkPosition pos : event.getPositions()) {
				Chunk<ChunkId, PointCloudChunkData> chunk = new Chunk<>(new ChunkId(pos, event.getLevel()), new PointCloudChunkData());
				DirectPosition2D bl = new DirectPosition2D(chunk.getPosition().getX(), chunk.getPosition().getY());
				DirectPosition2D tr = new DirectPosition2D(chunk.getPosition().getX() + chunk.getPosition().getWidth(), chunk.getPosition().getY() + chunk.getPosition().getHeight());
				GridCoordinates2D blg = coverage.getGridGeometry().worldToGrid(bl);
				GridCoordinates2D trg = coverage.getGridGeometry().worldToGrid(tr);
				GridEnvelope2D range = coverage.getGridGeometry().getGridRange2D();
				logger.info("Chunk: {} {} {} {} - {} {} {} {}", bl, tr, blg, trg, range.getLow(0), range.getHigh(0), range.getLow(1), range.getHigh(1));
				
				int w = (int) ((Math.min(trg.getX(), range.getHigh(0)) - (int) Math.max(blg.getX(), range.getLow(0))) + 1);
				int h = (int) (Math.min(blg.getY(), range.getHigh(1)) - Math.max(trg.getY(), range.getLow(1)) + 1);
				
				double[] points = new double[w*h*3];
				int ctr = 0;
				
				for (int i = (int) Math.max(blg.getX(), range.getLow(0)); i <= Math.min(trg.getX(), range.getHigh(0)); i++) {
					for (int j = (int) Math.max(trg.getY(), range.getLow(1)); j <= Math.min(blg.getY(), range.getHigh(1)); j++) {
						coord[0] = i;
						coord[1] = j;
						c.setLocation(i, j);
						coverage.getGridGeometry().getGridToCRS2D().transform(coord, 0, p, 0, 1);
						
						coverage.evaluate(c, vals);
						
						points[ctr++] = p[0];
						points[ctr++] = p[1];
						points[ctr++] = vals[0];
						count++;
					}
				}
				
				chunk.getData().setInterleavedPoints(points);
				
				eventBus.post(new PartialChunkAvailableEvent(chunk, event.getSheet()));
			}
			
			logger.info("Extracted {} points.", count);
		}
	}
	
	@Subscribe
	public void request(ExtractionRequestEvent event) {
		Settings.executorService.submit(() -> {
			try {
				logger.info("Extracting {}...", event);

				switch (event.getLevel()) {
					case FIVE_BY_FIVE:
					case HALF_BY_HALF:
						handleGeotiffFile(event);
						break;
					case LAS:
						throw new UnsupportedOperationException("Unimplemented: LAZ");
				}

			} catch (IOException | TransformException e) {
				e.printStackTrace();
			}
			cacheManager.releaseClaim(event.getClaim());
		});
	}
}
