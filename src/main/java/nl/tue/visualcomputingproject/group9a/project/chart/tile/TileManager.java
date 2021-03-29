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
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class TileManager {
	private static final Logger logger = LoggerFactory.getLogger(TileManager.class);
	private final EventBus eventBus;
	private final Map<TextureType, TileRenderer> rendererMap;
	private final CoordinateReferenceSystem crs;
	private final FileCacheManager cacheManager;
	private final static String AERIALURL = "https://services.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/WMTS/1.0.0/WMTSCapabilities.xml";
	private final static String PDOKWMS = "https://service.pdok.nl/hwh/luchtfotorgb/wms/v1_0?&request=GetCapabilities&service=wms";
	private final static String PDOKWMTS = "https://service.pdok.nl/hwh/luchtfotorgb/wmts/v1_0?&request=GetCapabilities&service=wmts";
	
	public TileManager(EventBus eventBus, CoordinateReferenceSystem crs, CachePolicy policy) throws IOException, ServiceException {
		this.eventBus = eventBus;
		this.crs = crs;
		
		eventBus.register(this);
		
		rendererMap = new HashMap<>();
		//rendererMap.put(TextureType.Aerial, new TileRenderer(new WMTSTileProvider(new URL(AERIALURL), "World_Imagery")));
		rendererMap.put(TextureType.Aerial, new TileRenderer(new WMSTileProvider(new URL(PDOKWMS), "Actueel_ortho25")));
		//rendererMap.put(TextureType.Aerial, new TileRenderer(new WMTSTileProvider(new URL(PDOKWMTS),"Actueel_ortho25")));
		rendererMap.put(TextureType.OpenStreetMap, new TileRenderer(new OSMTileProvider()));
		cacheManager = new FileCacheManager(policy, Settings.CACHE_DIR, "textures");
		cacheManager.indexCache(TextureFileId.createFactory());
	}
	
	@Subscribe
	public void onChunkStatus(RendererChunkStatusEvent event) {
		for (ChunkPosition position : event.getNewChunks()) {
			//eventBus.post(new TextureRequestEvent(TextureType.OpenStreetMap, position));
			eventBus.post(new TextureRequestEvent(TextureType.Aerial, position));
		}
	}
	
	@Subscribe
	public void onRequest(TextureRequestEvent event) {
		Settings.executorService.submit(() -> {
			try {
				logger.info("Loading texture of type {} for chunk {}...", event.getType(), event.getPosition());
				TextureFileId id = new TextureFileId(event.getType(), event.getPosition());
				FileReadCacheClaim readClaim = cacheManager.requestReadClaim(id);
				if (readClaim != null) {
					logger.info("Found in cache!");
					//File available for reading!
					try (InputStream stream = readClaim.getInputStream()) {
						BufferedImage image = ImageIO.read(stream);
						ByteBuffer buffer = convertImageData(image);
						eventBus.post(new ChartTextureAvailableEvent(
								event.getPosition(),
								event.getType(),
								buffer,
								image.getWidth(),
								image.getHeight())
						);
					}
					cacheManager.releaseCacheClaim(readClaim);
					
				} else {
					logger.info("Downloading...");
					FileReadWriteCacheClaim writeClaim = cacheManager.requestReadWriteClaim(id);
					if (writeClaim != null) {
						ReferencedEnvelope envelope = event.getPosition()
								.transformedAddBorder(Settings.CHUNK_TILE_BORDER)
								.getReferencedEnvelope(crs);
						int image_width = (int) envelope.getWidth() * 2;
						int image_height = (int) envelope.getHeight() * 2;
						BufferedImage image = rendererMap
								.get(event.getType())
								.render(envelope, image_width, image_height);
						try (OutputStream stream = writeClaim.getOutputStream()) {
							ImageIO.write(image, "png", stream);
						}
						ByteBuffer buffer = convertImageData(image);
						eventBus.post(new ChartTextureAvailableEvent(
								event.getPosition(),
								event.getType(),
								buffer,
								image.getWidth(),
								image.getHeight())
						);
					} else {
						throw new RuntimeException("Unable to get cache worked out for texture event!");
					}
				}
			} catch (TransformException | FactoryException | IOException e) {
				e.printStackTrace();
			}
		});
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

	/**
	 * Convert the buffered image to a texture
	 */
	private static ByteBuffer convertImageData(BufferedImage bufferedImage) {
		ByteBuffer imageBuffer;
		WritableRaster raster;
		BufferedImage texImage;

		ColorModel glAlphaColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 8},
				true, false,
				Transparency.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);

		raster = Raster.createInterleavedRaster(
				DataBuffer.TYPE_BYTE,
				bufferedImage.getWidth(),
				bufferedImage.getHeight(),
				4,
				null
		);
		texImage = new BufferedImage(glAlphaColorModel, raster, true, null);

		// copy the source image into the produced image
		Graphics g = texImage.getGraphics();
		Color color = new Color(0f, 1f, 0f, 0f);
		g.setColor(color);
		g.fillRect(0, 0, 256, 256);
		g.drawImage(bufferedImage, 0, 0, null);

		// build a byte buffer from the temporary image that be used by OpenGL to produce a texture.
		byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer()).getData();

		imageBuffer = ByteBuffer.allocateDirect(data.length);
		imageBuffer.order(ByteOrder.nativeOrder());
		imageBuffer.put(data, 0, data.length);
		imageBuffer.flip();

		return imageBuffer;
	}
	
}
