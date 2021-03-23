package nl.tue.visualcomputingproject.group9a.project.renderer.engine.model;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;

public class Skybox {

	private static final float SIZE = Settings.FAR_PLANE * 0.5f;

	/**
	 * Vertex positions in triangles that make a cube
	 */
	private static final float[] VERTICES = {
			-SIZE, SIZE, -SIZE, // Front
			-SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE,
			-SIZE, SIZE, -SIZE,

			-SIZE, -SIZE, SIZE, // Right
			-SIZE, -SIZE, -SIZE,
			-SIZE, SIZE, -SIZE,
			-SIZE, SIZE, -SIZE,
			-SIZE, SIZE, SIZE,
			-SIZE, -SIZE, SIZE,

			SIZE, -SIZE, -SIZE, // Left
			SIZE, -SIZE, SIZE,
			SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE,
			SIZE, SIZE, -SIZE,
			SIZE, -SIZE, -SIZE,

			-SIZE, -SIZE, SIZE, // Back
			-SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE,
			SIZE, -SIZE, SIZE,
			-SIZE, -SIZE, SIZE,

			-SIZE, SIZE, -SIZE, // Top
			SIZE, SIZE, -SIZE,
			SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE,
			-SIZE, SIZE, SIZE,
			-SIZE, SIZE, -SIZE,

			-SIZE, -SIZE, -SIZE, // Bottom
			-SIZE, -SIZE, SIZE,
			SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, -SIZE,
			-SIZE, -SIZE, SIZE,
			SIZE, -SIZE, SIZE
	};

	/**
	 * ID of the buffer the textures are in
	 */
	@Getter
	private final int texture;

	@Getter
	private final RawModel model;

	public Skybox(String[] textureFiles) {
		model = Loader.loadSkyboxModel(VERTICES);
		texture = Loader.loadCubeMapTextures(textureFiles);
	}

}
