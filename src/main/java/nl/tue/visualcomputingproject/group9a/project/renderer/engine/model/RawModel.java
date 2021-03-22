package nl.tue.visualcomputingproject.group9a.project.renderer.engine.model;

import lombok.Data;
import org.joml.Matrix4f;

import java.util.Collection;

@Data
public class RawModel {

	private final int vaoId;
	private final int indicesCount;
	private final Matrix4f modelMatrix;
	private final Collection<Integer> vboIds;

}
