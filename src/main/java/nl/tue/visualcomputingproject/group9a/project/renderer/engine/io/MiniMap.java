package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.swing.JMapPane;
import org.geotools.swing.event.MapMouseAdapter;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.osm.OSMService;
import org.geotools.tile.util.TileLayer;
import org.joml.Vector3f;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.invoke.MethodHandles;

public class MiniMap extends JPanel {
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String SETTINGS_FOLLOW = "minimap.follow";
	
	private final JMapPane mapPane;
	private final CoordinateReferenceSystem crs = CRS.decode("EPSG:28992");
	private final MathTransform2D transformFromCRSToPane;
	private final Camera camera;
	
	@Getter
	private boolean followCamera = Settings.SETTINGS.getValue(SETTINGS_FOLLOW, false);
	
	public MiniMap(Camera camera) throws FactoryException {
		super(new BorderLayout());
		this.camera = camera;
		String baseURL = "http://tile.openstreetmap.org/";
		TileService service = new OSMService("OSM", baseURL);
		MapContent map = new MapContent();
		map.addLayer(new TileLayer(service));
		mapPane = new JMapPane(map);
		mapPane.setVisible(true);
		mapPane.addMouseListener(new MiniMapMouseAdapter(mapPane));
		add(mapPane);
		
		transformFromCRSToPane = (MathTransform2D) CRS.findMathTransform(crs, mapPane.getDisplayArea().getCoordinateReferenceSystem());
		
		mapPane.addMouseListener(new MapMouseAdapter() {
			@Override
			public void onMouseClicked(MapMouseEvent ev) {
				super.onMouseClicked(ev);
				LOGGER.info("Minimap clicked at world position " + ev.getWorldPos());
				setCameraPosition(ev.getWorldPos());
			}
		});
	}
	
	@SneakyThrows
	public void update() {
		if (followCamera) {
			centerMapOnCameraPosition(camera.getPosition(), false);
			//updateMapFollowPosition();
		}
	}
	
	@SneakyThrows
	public void setCameraPosition(DirectPosition2D position) {
		MathTransform2D transform = (MathTransform2D) CRS.findMathTransform(position.getCoordinateReferenceSystem(), crs);
		Point2D p = transform.transform((Point2D) position, null);
		camera.setPosition(new Vector3f(
			(float) p.getX(),
			Settings.INITIAL_POSITION.y,
			-(float) p.getY())
		);
	}
	
	public void centerMapOnCameraPosition(Vector3f position, boolean forceZoom) throws TransformException {
		
		DirectPosition2D mapPos = new DirectPosition2D(crs, position.x, -position.z);
		
		// I would prefer to offset the new map based on the cursor but this matches
		// the current zoom in/out tools.
		
		Point2D mapPos2 = transformFromCRSToPane.transform((Point2D) mapPos, null);
		
		Rectangle paneArea = mapPane.getVisibleRect();

		Point2D corner =
			new DirectPosition2D(
				mapPos2.getX() - 0.5d * paneArea.getWidth() / mapPane.getWorldToScreenTransform().getScaleX(),
				mapPos2.getY() + 0.5d * paneArea.getHeight() / mapPane.getWorldToScreenTransform().getScaleY());
		
		if (forceZoom) {
			corner = transformFromCRSToPane.transform((Point2D) new DirectPosition2D(crs, mapPos.getX() - 100, mapPos.getY() + 100), null);
		}
		
		Envelope2D newMapArea = new Envelope2D();
		newMapArea.setFrameFromCenter(mapPos2, corner);
		if (!mapPane.getDisplayArea().boundsEquals2D(newMapArea, 0.0001)) {
			mapPane.setDisplayArea(newMapArea);
		}
	}

	public void setFollowCamera(boolean followCamera) {
		if (this.followCamera != followCamera) {
			this.followCamera = followCamera;
			Settings.SETTINGS.updateValue("minimap.follow", followCamera);
		}
	}
	
}
