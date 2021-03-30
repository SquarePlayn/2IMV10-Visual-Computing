package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import lombok.SneakyThrows;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.lang.invoke.MethodHandles;

public class Sidebar extends JPanel {

	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Camera camera;
	private final MiniMap miniMap;

	@SneakyThrows
	public Sidebar(Camera camera, MiniMap miniMap) {
		super(new BorderLayout());
		this.camera = camera;
		this.miniMap = miniMap;
		initialize();
	}

	public void initialize() throws FactoryException {
		// Create layout
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridy = 0;
		
		// Wireframe button
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		add(new JLabel("Wireframe rendering mode:"), c);
		JCheckBox buttonWireframe = new JCheckBox();
		buttonWireframe.addItemListener(e -> camera.setWireframe(e.getStateChange() == ItemEvent.SELECTED));
		c.gridx = 1;
		add(buttonWireframe, c);
		
		c.gridy++;
		
		// Follow camera button
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		add(new JLabel("Minimap follows camera:"), c);
		JCheckBox buttonMinimap = new JCheckBox();
		buttonMinimap.addItemListener(e -> miniMap.setFollowCamera(e.getStateChange() == ItemEvent.SELECTED));
		c.gridx = 1;
		add(buttonMinimap, c);

		c.gridy++;
		
		// Camera type selector
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		add(new JLabel("Camera mode:"), c);
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
		c.gridx = 1;
		add(dropdownCameraType, c);
	}
}
