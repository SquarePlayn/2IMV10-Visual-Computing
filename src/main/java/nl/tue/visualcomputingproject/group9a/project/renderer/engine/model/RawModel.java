package nl.tue.visualcomputingproject.group9a.project.renderer.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.joml.Matrix4f;

import java.util.Collection;

@Data
@AllArgsConstructor
public class RawModel {

	private boolean unloaded;
	private final int vaoId;
	private final int indicesCount;
	private final Matrix4f modelMatrix;
	private final Collection<Integer> vboIds;
	private int texId = -1;

}
