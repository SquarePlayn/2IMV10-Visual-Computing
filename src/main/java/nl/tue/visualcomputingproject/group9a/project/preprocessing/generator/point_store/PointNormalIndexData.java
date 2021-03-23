package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

@Getter
@Setter
public class PointNormalIndexData
		extends PointIndexData {
	private Vector3d normal;

	public PointNormalIndexData(Vector3d vec, Vector3d normal, int index) {
		super(vec, index);
		this.normal = normal;
	}
	
	public PointNormalIndexData(Vector3d vec, Vector3d normal) {
		super(vec);
		this.normal = normal;
	}

	public PointNormalIndexData(Vector3d vec) {
		this(vec, new Vector3d());
	}
	
}
