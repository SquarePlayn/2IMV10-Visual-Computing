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

public class Sidebar extends JPanel implements Camera.Listener {

	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Camera camera;
	private final MiniMap miniMap;
	private JCheckBox buttonWireframe, buttonMinimap;
	private JComboBox<String> dropdownCameraType;
	private JSlider sensitivitySlider;
	private static final String WALKING = "Walking", FLYING = "Flying", HOVERING = "Hovering";
	
	@SneakyThrows
	public Sidebar(Camera camera, MiniMap miniMap) {
		super(new BorderLayout());
		this.camera = camera;
		this.miniMap = miniMap;
		initialize();
	}

	public void initialize() {
		// Create layout
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridy = 0;
		
		// Wireframe button
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		add(new JLabel("Wireframe rendering mode:"), c);
		buttonWireframe = new JCheckBox();
		buttonWireframe.addItemListener(e -> camera.setWireframe(e.getStateChange() == ItemEvent.SELECTED));
		c.gridx = 1;
		add(buttonWireframe, c);
		
		c.gridy++;
		
		// Follow camera button
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		add(new JLabel("Minimap follows camera:"), c);
		buttonMinimap = new JCheckBox();
		buttonMinimap.addItemListener(e -> miniMap.setFollowCamera(e.getStateChange() == ItemEvent.SELECTED));
		c.gridx = 1;
		add(buttonMinimap, c);

		c.gridy++;
		
		// Camera type selector
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		add(new JLabel("Camera mode:"), c);
		dropdownCameraType = new JComboBox<>(new String[]{HOVERING, FLYING, WALKING});
		dropdownCameraType.addItemListener(e -> {
			LOGGER.info("Setting camera type to: " + e.getItem());
			if (e.getItem().equals(WALKING)) {
				camera.setWalking(true);
				camera.setLockHeight(false);
			} else if (e.getItem().equals(FLYING)) {
				camera.setWalking(false);
				camera.setLockHeight(false);
			} else {
				camera.setWalking(false);
				camera.setLockHeight(true);
			}
		});
		c.gridx = 1;
		add(dropdownCameraType, c);
		
		c.gridy++;
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		add(new JLabel("Mouse sensitivity:"), c);
		sensitivitySlider = new JSlider(JSlider.HORIZONTAL, 0, 50, 10);
		sensitivitySlider.addChangeListener(e -> {
			JSlider source = (JSlider) e.getSource();
			float value = source.getValue();
			camera.setSensitivity(value / 10.0f);
		});
		c.gridx = 1;
		add(sensitivitySlider, c);
		
		camera.getListeners().add(this);
	}
	
	public void updateFromCamera() {
		if (camera.isWalking() && !camera.isLockHeight()) {
			dropdownCameraType.setSelectedItem(WALKING);
		} else if (!camera.isWalking() && !camera.isLockHeight()) {
			dropdownCameraType.setSelectedItem(FLYING);
		} else {
			dropdownCameraType.setSelectedItem(HOVERING);
		}
		
		buttonWireframe.setSelected(camera.isWireframe());
		buttonMinimap.setSelected(miniMap.isFollowCamera());
		sensitivitySlider.setValue((int) (camera.getSensitivity()*10.0f));
	}
	
	@Override
	public void onCameraSettingChange() {
		updateFromCamera();
	}
}
