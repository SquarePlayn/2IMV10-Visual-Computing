package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.joml.Vector3d;

/**
 * Data class storing a point with it's index in the vertex buffer.
 */
@Getter
@RequiredArgsConstructor
public class PointIndex {
	final private Vector3d point;
	@Setter
	private int index = -1;

	public String toString() {
		return "[" + index + ": " + point + "]";
	}

}
