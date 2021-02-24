package nl.tue.visualcomputingproject.group9a.project.common.cache;

@FunctionalInterface
public interface FileIdFactory<T extends FileId> {
	
	T fromPath(String path);
	
}
