package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import org.lwjgl.opengl.awt.GLData;

import javax.swing.*;
import java.awt.*;

public class SwingWindow {
	@Getter
	private final JFrame frame = new JFrame(Settings.WINDOW_NAME);
	@Getter
	private final SwingCanvas canvas;
	
	public SwingWindow(EventBus eventBus) {
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setPreferredSize(new Dimension(Settings.INITIAL_WINDOW_SIZE.x, Settings.INITIAL_WINDOW_SIZE.y));
		
		GLData data = new GLData();
		data.samples = 4;
		data.swapInterval = 0;
		canvas = new SwingCanvas(data, eventBus);
		
		frame.add(canvas);
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
	}
}
