package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

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
import java.awt.event.ItemEvent;
import java.awt.geom.Point2D;
import java.lang.invoke.MethodHandles;

public class Sidebar extends JPanel {

	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	final Camera camera;

	private JMapPane mapPane;
	private final CoordinateReferenceSystem crs = CRS.decode("EPSG:28992");
	private MathTransform2D transformFromCRSToPane;

	@SneakyThrows
	Sidebar(Camera camera) throws FactoryException {
		super(new BorderLayout());
		this.camera = camera;
		this.initialize();
	}

	public void initialize() throws FactoryException {
		// Create layout
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// Wireframe button
		JToggleButton buttonWireframe = new JToggleButton("Wireframe");
		buttonWireframe.addItemListener(e -> camera.setWireframe(e.getStateChange() == ItemEvent.SELECTED));
		add(buttonWireframe);

		// Camera type selector
		JComboBox<String> dropdownCameraType = new JComboBox<>(new String[]{"Hovering", "Flying", "Walking"});
		dropdownCameraType.addItemListener(e -> {
			LOGGER.info("Setting camera type to: " + e.getItem());
			if (e.getItem() == "Walking") {
				camera.setWalking(true);
				camera.setLockHeight(false);
			} else if (e.getItem() == "Flying") {
				camera.setWalking(false);
				camera.setLockHeight(false);
			} else {
				camera.setWalking(false);
				camera.setLockHeight(true);
			}
		});
		add(dropdownCameraType);

		// Map pane
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
	public void setCameraPosition(DirectPosition2D position) {
		MathTransform2D transform = (MathTransform2D) CRS.findMathTransform(position.getCoordinateReferenceSystem(), crs);
		Point2D p = transform.transform((Point2D) position, null);
		camera.setPosition(new Vector3f(
				(float) p.getX(),
				Settings.INITIAL_POSITION.y,
				-(float) p.getY())
		);
	}

	public void centerMapOnCameraPosition(Vector3f position) throws TransformException {

		DirectPosition2D mapPos = new DirectPosition2D(crs, position.x, -position.z);
		DirectPosition2D corner = new DirectPosition2D(
				crs,
				mapPos.getX() - 100,
				mapPos.getY() + 100
		);

		// I would prefer to offset the new map based on the cursor but this matches
		// the current zoom in/out tools.

		Point2D mapPos2 = transformFromCRSToPane.transform((Point2D) mapPos, null);
		Point2D corner2 = transformFromCRSToPane.transform((Point2D) corner, null);

		Envelope2D newMapArea = new Envelope2D();
		newMapArea.setFrameFromCenter(mapPos2, corner2);
		mapPane.setDisplayArea(newMapArea);
	}
}
