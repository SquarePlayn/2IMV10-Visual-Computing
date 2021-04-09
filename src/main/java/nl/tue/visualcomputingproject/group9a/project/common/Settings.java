package nl.tue.visualcomputingproject.group9a.project.common;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshBufferType;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.VertexBufferType;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Global project settings class.
 */
public final class Settings {

	// Disallow instantiation.
	private Settings() {
	}
	
	public static final int CHUNK_WIDTH = 100;
	public static final int CHUNK_HEIGHT = 100;
	public static final int CHUNK_VERTEX_BORDER = 10;
	public static final int CHUNK_TILE_BORDER = 5;

	/**
	 * Number of dedicated threads reserved by various modules in the application.
	 * Reserved by:
	 * - Rendering: 1
	 * - Chart: 1
	 */
	public static final int NUM_DEDICATED_THREADS = 3;
	
	/** The service used for scheduling tasks. */
	public static final ExecutorService executorService = Executors.newFixedThreadPool(
			Math.max(1, Runtime.getRuntime().availableProcessors() - Settings.NUM_DEDICATED_THREADS),
			r -> {
				Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setDaemon(true);
				t.setUncaughtExceptionHandler((Thread thread, Throwable e) -> {
					e.printStackTrace();
				});
				return t;
			});
	
	/** The service used for scheduling texture tasks. */
	public static final ExecutorService textureExecutorService = Executors.newFixedThreadPool(
		Math.max(1, Runtime.getRuntime().availableProcessors() - Settings.NUM_DEDICATED_THREADS),
		r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			t.setUncaughtExceptionHandler((Thread thread, Throwable e) -> {
				e.printStackTrace();
			});
			return t;
		});
	
	
	/** The directory used for caching. */
	public static File CACHE_DIR = new File("cache");
	public static File SETTINGS_FILE = new File("settings.properties");

	/** Chunk update settings. */
	public static final SettingsFile SETTINGS = new SettingsFile(SETTINGS_FILE);
	public static final String SETTINGS_CHUNK_LOAD = "chunk.loaddistance";
	public static final double CHUNK_LOAD_DISTANCE_MIN = 500.0;
	public static final double CHUNK_LOAD_DISTANCE_MAX = 1000.0;
	private static double chunkLoadDistance;
	
	public static void setChunkLoadDistance(double dist) {
		chunkLoadDistance = Math.min(CHUNK_LOAD_DISTANCE_MAX, Math.max(CHUNK_LOAD_DISTANCE_MIN, dist));
		SETTINGS.updateValue(SETTINGS_CHUNK_LOAD, chunkLoadDistance);
	}
	static {
		setChunkLoadDistance(SETTINGS.getValue(SETTINGS_CHUNK_LOAD, 1000.0));
	}

	public static double getChunkLoadDistance() {
		return chunkLoadDistance;
	}
	
	public static double getChunkUnloadDistance() {
		double d = chunkLoadDistance;
		return d + Math.max(250, d / 10.);
	}
	
	/** The file extension of the cache files. */
	public static final String CACHE_EXT = ".cache";
	/** The file extension of the temporary cache files. */
	public static final String TMP_CACHE_EXT = ".part";
	
	/** The maximum quality of map sheets the chart module will download. */
	public static final QualityLevel MAX_DOWNLOAD_QUALITY = QualityLevel.HALF_BY_HALF;

	/** The type used in the vertex buffer. */
	public static final VertexBufferType VERTEX_TYPE = VertexBufferType.INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT;
	/** The type used in the mesh buffer. */
	public static final MeshBufferType MESH_TYPE = MeshBufferType.TRIANGLES_COUNTER_CLOCKWISE_3_INT;

	/**
	 * Window settings
	 */
	public static final String WINDOW_NAME = "3D terrain reconstruction";
	public static final Vector2i INITIAL_WINDOW_SIZE = new Vector2i(1000, 800);
	public static final int FPS = 30;

	/**
	 * Camera settings
	 */
	public static final Vector3f INITIAL_POSITION = new Vector3f(162000, 100, -384300);
	public static final float FOV = 70;
	public static final float ZOOM_FACTOR = 3;
	public static final float NEAR_PLANE = 0.1f;
	public static final float FAR_PLANE = 40_000;
	public static final float MOVE_SPEED = 100;
	public static final float GROUND_MOVE_SPEED_PERCENTAGE = 0.2f;
	public static final float LOOK_SPEED = 30f;
	public static final float WALK_HEIGHT = 5;

	/**
	 * Shader settings
	 */
	public static final Vector3f LIGHT_COLOR = new Vector3f(1, 1, 1);

	/**
	 * Skybox textures, in order left, right, top, bottom, back, front
	 */
	public static final String[] SKYBOX_TEXTURE_FILES = new String[]{
		"left", "right", "top", "bottom", "back", "front"
	};
	
}
