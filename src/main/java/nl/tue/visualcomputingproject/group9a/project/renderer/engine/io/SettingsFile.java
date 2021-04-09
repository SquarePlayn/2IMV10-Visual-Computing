package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.Properties;

public class SettingsFile {
	private Properties props;
	private final File path;
	private final Camera camera;
	private final MiniMap miniMap;
	
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	public SettingsFile(File path, Camera camera, MiniMap miniMap) {
		this.camera = camera;
		this.miniMap = miniMap;
		this.props = new Properties();
		this.path = path;
	}
	
	public void loadCurrentValues() {
		props.setProperty("camera.wireframe", String.valueOf(camera.isWireframe()));
		props.setProperty("camera.lockheight", String.valueOf(camera.isLockHeight()));
		props.setProperty("camera.walking", String.valueOf(camera.isWalking()));
		props.setProperty("camera.sensitivity", String.valueOf(camera.getSensitivity()));
		props.setProperty("chunk.loaddistance", String.valueOf(Settings.CHUNK_LOAD_DISTANCE));
		props.setProperty("chunk.unloaddistance", String.valueOf(Settings.CHUNK_UNLOAD_DISTANCE));
		props.setProperty("minimap.follow", String.valueOf(miniMap.isFollowCamera()));
	}
	
	public void applyValues() {
		camera.setWireframe(Boolean.parseBoolean(props.getProperty("camera.wireframe")));
		camera.setLockHeight(Boolean.parseBoolean(props.getProperty("camera.lockheight")));
		camera.setWalking(Boolean.parseBoolean(props.getProperty("camera.walking")));
		camera.setSensitivity(Float.parseFloat(props.getProperty("camera.sensitivity")));
		Settings.CHUNK_LOAD_DISTANCE = Double.parseDouble(props.getProperty("chunk.loaddistance"));
		Settings.CHUNK_UNLOAD_DISTANCE = Double.parseDouble(props.getProperty("chunk.unloaddistance"));
		miniMap.setFollowCamera(Boolean.parseBoolean(props.getProperty("minimap.follow")));
	}
	
	public void readFile() {
		try {
			try (FileInputStream inputStream = new FileInputStream(path)) {
				props.load(inputStream);
			}
		} catch(FileNotFoundException e) {
			LOGGER.info("No settings file found. Nothing is changing.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeFile() {
		try {
			try (FileOutputStream outputStream = new FileOutputStream(path)) {
				props.store(outputStream, "Settings");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
