package nl.tue.visualcomputingproject.group9a.project.chart.tile;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.tue.visualcomputingproject.group9a.project.chart.events.TextureRequestEvent;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.TextureType;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileIdFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileCacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartTextureAvailableEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TileManager {
	private static final Logger logger = LoggerFactory.getLogger(TileManager.class);
	private final EventBus eventBus;
	private final Map<TextureType, TileRenderer> rendererMap;
	private final CoordinateReferenceSystem crs;
	private final FileCacheManager cacheManager;
	private final static String AERIALURL = "https://services.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/WMTS/1.0.0/WMTSCapabilities.xml";
	
	public TileManager(EventBus eventBus, CoordinateReferenceSystem crs, CachePolicy policy) throws IOException, ServiceException {
		this.eventBus = eventBus;
		this.crs = crs;
		
		eventBus.register(this);
		
		rendererMap = new HashMap<>();
		rendererMap.put(TextureType.Aerial, new TileRenderer(new WMTSTileProvider(new URL(AERIALURL))));
		rendererMap.put(TextureType.OpenStreetMap, new TileRenderer(new OSMTileProvider()));
		cacheManager = new FileCacheManager(policy, Settings.CACHE_DIR, "mapsheets");
		cacheManager.indexCache(TextureFileId.createFactory());
	}
	
	@Subscribe
	public void onChunkStatus(RendererChunkStatusEvent event) {
		for (ChunkPosition position : event.getNewChunks()) {
			eventBus.post(new TextureRequestEvent(TextureType.OpenStreetMap, position));
			eventBus.post(new TextureRequestEvent(TextureType.Aerial, position));
		}
	}
	
	@Subscribe
	public void onRequest(TextureRequestEvent event) throws TransformException, FactoryException, IOException {
		logger.info("Loading texture of type {} for chunk {}...", event.getType(), event.getPosition());
		TextureFileId id = new TextureFileId(event.getType(), event.getPosition());
		FileReadCacheClaim readClaim = cacheManager.requestReadClaim(id);
		if (readClaim != null) {
			logger.info("Found in cache!");
			//File available for reading!
			try (InputStream stream = readClaim.getInputStream()) {
				BufferedImage image = ImageIO.read(stream);
				eventBus.post(new ChartTextureAvailableEvent(event.getPosition(), event.getType(), image));
			}
			cacheManager.releaseCacheClaim(readClaim);
		} else {
			logger.info("Downloading...");
			FileReadWriteCacheClaim writeClaim = cacheManager.requestReadWriteClaim(id);
			if (writeClaim != null) {
				ReferencedEnvelope envelope = event.getPosition().getReferencedEnvelope(crs);
				int image_width = (int) envelope.getWidth();
				int image_height = (int) envelope.getHeight();
				BufferedImage image = rendererMap.get(event.getType()).render(envelope, image_width, image_height);
				try (OutputStream stream = writeClaim.getOutputStream()) {
					ImageIO.write(image, "png", stream);
				}
				eventBus.post(new ChartTextureAvailableEvent(event.getPosition(), event.getType(), image));
			} else {
				throw new RuntimeException("Unable to get cache worked out for texture event!");
			}
		}
	}
	
	@ToString
	@EqualsAndHashCode
	@AllArgsConstructor
	static class TextureFileId implements FileId {
		private static final String PRE = "texture";
		
		private final TextureType type;
		private final ChunkPosition position;
		
		@Override
		public String getPath() {
			return FileId.genPath(PRE, position.getX(), position.getY(), position.getWidth(), position.getHeight(), type.getOrder());
		}
		
		public static FileIdFactory<TextureFileId> createFactory() {
			return (String path) -> {
				String[] split = path.split(FileId.DELIM);
				if (split.length != 6 || !Objects.equals(split[0], PRE)) {
					return null;
				}
				TextureType type;
				ChunkPosition position;
				try {
					position = new ChunkPosition(
						Double.parseDouble(split[1]),
						Double.parseDouble(split[2]),
						Double.parseDouble(split[3]),
						Double.parseDouble(split[4])
					);
					type = TextureType.fromOrder(Integer.parseInt(split[5]));
				} catch (NumberFormatException e) {
					return null;
				}
				return new TextureFileId(type, position);
			};
		}
	}
}
