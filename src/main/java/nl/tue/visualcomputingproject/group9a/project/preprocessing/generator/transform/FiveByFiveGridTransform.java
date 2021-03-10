package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform;

public class FiveByFiveGridTransform
		extends ScaleGridTransform
		implements GridTransform {
	
	public FiveByFiveGridTransform(double rootX, double rootZ) {
		super(5, 5, rootX, rootZ);
	}
	
}
