package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
import lombok.SneakyThrows;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
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
import org.lwjgl.opengl.awt.GLData;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

public class SwingWindow {
	@Getter
	private final JFrame frame = new JFrame(Settings.WINDOW_NAME);
	@Getter
	private final SwingCanvas canvas;
	private final JPanel sidebar, glPanel;
	private JMapPane mapPane;
	private final CoordinateReferenceSystem crs = CRS.decode("EPSG:28992");
	private MathTransform2D transformFromCRSToPane;
	
	public SwingWindow(EventBus eventBus) throws FactoryException {
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setPreferredSize(new Dimension(Settings.INITIAL_WINDOW_SIZE.x, Settings.INITIAL_WINDOW_SIZE.y));
		
		GLData data = new GLData();
		data.samples = 4;
		data.swapInterval = 0;
		
		glPanel = new JPanel(new BorderLayout());
		sidebar = new JPanel(new BorderLayout());

		canvas = new SwingCanvas(data, eventBus);
		canvas.setMinimumSize(new Dimension(100, 100));
		canvas.setVisible(true);

		glPanel.add(canvas, BorderLayout.CENTER);
		glPanel.setVisible(true);

		sidebar.setMinimumSize(new Dimension(100, 100));
		setupSidebar();
		sidebar.setVisible(true);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, glPanel, sidebar);
		splitPane.setVisible(true);
		splitPane.setContinuousLayout(false);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.75);
		frame.add(splitPane);
		
		frame.pack();
		frame.setVisible(true);
		frame.transferFocus();
		
		Runnable renderLoop = new Runnable() {
			public void run() {
				if (!canvas.isValid())
					return;
				canvas.render();
				SwingUtilities.invokeLater(this);
			}
		};
		SwingUtilities.invokeLater(renderLoop);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					centerMapOnCameraPosition(Settings.INITIAL_POSITION);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});
	}
	
	@SneakyThrows
	private void setupSidebar() {
		String baseURL = "http://tile.openstreetmap.org/";
		TileService service = new OSMService("OSM", baseURL);
		MapContent map = new MapContent();
		map.addLayer(new TileLayer(service));
		mapPane = new JMapPane(map);
		mapPane.setVisible(true);
		mapPane.addMouseListener(new MiniMapMouseAdapter(mapPane));
		sidebar.add(mapPane, BorderLayout.CENTER);
		
		transformFromCRSToPane = (MathTransform2D) CRS.findMathTransform(crs, mapPane.getDisplayArea().getCoordinateReferenceSystem());
		
		mapPane.addMouseListener(new MapMouseAdapter() {
			@Override
			public void onMouseClicked(MapMouseEvent ev) {
				super.onMouseClicked(ev);
				setCameraPosition(ev.getWorldPos());
			}
		});
	}
	
	public void setCameraPosition(DirectPosition2D position) {
		try {
			MathTransform2D transform = (MathTransform2D) CRS.findMathTransform(position.getCoordinateReferenceSystem(), crs);
			Point2D p = transform.transform((Point2D) position, null);
			canvas.getCamera().setPosition(new Vector3f((float) p.getX(),
				Settings.INITIAL_POSITION.y,
				(float) p.getY()));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void centerMapOnCameraPosition(Vector3f position) throws FactoryException, TransformException {
		
		DirectPosition2D mapPos = new DirectPosition2D(crs, position.x, -position.z);
		DirectPosition2D corner =
			new DirectPosition2D(crs,
				mapPos.getX() - 100,
				mapPos.getY() + 100);
		
		// I would prefer to offset the new map based on the cursor but this matches
		// the current zoom in/out tools.
		
		Point2D mapPos2 = transformFromCRSToPane.transform((Point2D) mapPos, null);
		Point2D corner2 = transformFromCRSToPane.transform((Point2D) corner, null);
		
		Envelope2D newMapArea = new Envelope2D();
		newMapArea.setFrameFromCenter(mapPos2, corner2);
		mapPane.setDisplayArea(newMapArea);
	}
}