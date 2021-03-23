package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

@Getter
@Setter
@AllArgsConstructor
public class PointIndexData {
	private Vector3d vec;
	private int index;
	
	public PointIndexData() {
		this(new Vector3d());
	}
	
	public PointIndexData(Vector3d vec) {
		this.vec = vec;
		index = -1;
	}
	
}
