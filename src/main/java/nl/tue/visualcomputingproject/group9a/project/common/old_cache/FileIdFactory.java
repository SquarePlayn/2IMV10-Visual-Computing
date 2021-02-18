package nl.tue.visualcomputingproject.group9a.project.common.old_cache;

import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;

@FunctionalInterface
public interface FileIdFactory<T extends FileId> {
	
	T fromPath(String path);
	
}
