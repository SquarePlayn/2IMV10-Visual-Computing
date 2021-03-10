package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform;

public class HalfByHalfGridTransform 
		extends ScaleGridTransform
		implements GridTransform {
	
	public HalfByHalfGridTransform(double rootX, double rootZ) {
		super(0.5, 0.5, rootX, rootZ);
	}
	
}
