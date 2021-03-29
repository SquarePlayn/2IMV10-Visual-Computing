package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
import lombok.SneakyThrows;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import org.lwjgl.opengl.awt.GLData;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.invoke.MethodHandles;

public class SwingWindow {
	@Getter
	private final JFrame frame = new JFrame(Settings.WINDOW_NAME);
	@Getter
	private final SwingCanvas canvas;
	private final Sidebar sidebar;

	/**
	 * The logger object of this class.
	 */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public SwingWindow(EventBus eventBus) throws FactoryException {
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setPreferredSize(new Dimension(Settings.INITIAL_WINDOW_SIZE.x, Settings.INITIAL_WINDOW_SIZE.y));

		GLData data = new GLData();
		data.samples = 4;
		data.swapInterval = 0;

		canvas = new SwingCanvas(data, eventBus);
		canvas.setMinimumSize(new Dimension(100, 100));
		sidebar = new Sidebar(canvas.getCamera());
		canvas.setVisible(true);
		sidebar.setMinimumSize(new Dimension(100, 100));
		sidebar.setVisible(true);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, sidebar);
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
			@SneakyThrows
			@Override
			public void run() {
				sidebar.centerMapOnCameraPosition(Settings.INITIAL_POSITION);
			}
		});
	}
}
