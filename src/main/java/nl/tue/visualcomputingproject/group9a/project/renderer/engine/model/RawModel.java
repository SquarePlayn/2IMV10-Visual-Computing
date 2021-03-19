package nl.tue.visualcomputingproject.group9a.project.renderer.engine.model;

import lombok.Data;
import org.joml.Matrix4f;

@Data
public class RawModel {

	private final int vaoID;
	private final int indicesCount;
	private final Matrix4f modelMatrix;

}
