package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import org.lwjgl.opengl.awt.GLData;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.FPS;

@SuppressWarnings("UnstableApiUsage")
public class SwingWindow implements ActionListener {
	@Getter
	private final JFrame frame = new JFrame(Settings.WINDOW_NAME);
	@Getter
	private final SwingCanvas canvas;
	private final JPanel glPanel;
	private final Sidebar sidebar;
	private final MiniMap miniMap;
	private final Timer timer;

	/**
	 * The logger object of this class.
	 */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public SwingWindow(EventBus eventBus) throws FactoryException {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setPreferredSize(new Dimension(Settings.INITIAL_WINDOW_SIZE.x, Settings.INITIAL_WINDOW_SIZE.y));

		GLData data = new GLData();
		data.samples = 4;
		data.swapInterval = 0;
		
		glPanel = new JPanel(new BorderLayout());

		canvas = new SwingCanvas(data, eventBus);
		canvas.setMinimumSize(new Dimension(100, 100));
		canvas.setVisible(true);
		
		glPanel.add(canvas, BorderLayout.CENTER);
		glPanel.setVisible(true);
		
		miniMap = new MiniMap(canvas.getCamera());
		miniMap.setMinimumSize(new Dimension(100, 100));
		miniMap.setVisible(true);

		sidebar = new Sidebar(canvas.getCamera(), miniMap);

		sidebar.setMinimumSize(new Dimension(100, 100));
		sidebar.setVisible(true);
		
		JScrollPane scrollPane = new JScrollPane(sidebar);
		scrollPane.setVisible(true);
		
		JSplitPane sidePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, miniMap);
		sidePane.setVisible(true);
		sidePane.setContinuousLayout(true);
		sidePane.setOneTouchExpandable(true);
		sidePane.setResizeWeight(0.75);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, glPanel, sidePane);
		splitPane.setVisible(true);
		splitPane.setContinuousLayout(false);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.75);
		frame.add(splitPane);

		frame.pack();
		frame.setVisible(true);
		frame.transferFocus();

		timer = new Timer(1000/FPS, this);
		timer.setRepeats(true);
		timer.start();

		SwingUtilities.invokeLater(() -> {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			try {
				miniMap.centerMapOnCameraPosition(Settings.INITIAL_POSITION, true);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		});
	}
	
	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		if (canvas.isValid()) {
			canvas.render();
			miniMap.update();
		}
	}
}
