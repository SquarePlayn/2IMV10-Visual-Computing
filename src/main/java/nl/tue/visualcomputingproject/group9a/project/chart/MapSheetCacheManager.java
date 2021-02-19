package nl.tue.visualcomputingproject.group9a.project.chart;

import lombok.AllArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileStreamRequester;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.BufferedFileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MapSheetCacheManager {
	private final CacheFileManager<File> cacheManager;
	private final CacheFileStreamRequester<MapSheetFileId> requester;
	
	public MapSheetCacheManager(CacheFileManager<File> cacheManager) {
		this.cacheManager = cacheManager;
		this.requester = new CacheFileStreamRequester<>(
			cacheManager,
			new BufferedFileStreamFactory()
		);
	}
	
	public boolean isSheetAvailable(MapSheet sheet, QualityLevel level) {
		return cacheManager.exists(new MapSheetFileId(sheet, level));
	}
	
	public OutputStream getOutputStream(MapSheet sheet, QualityLevel level) throws IOException {
		return requester.getOutputStream(new MapSheetFileId(sheet, level));
	}
	
	@AllArgsConstructor
	static class MapSheetFileId
		extends FileId {
		private final MapSheet sheet;
		private final QualityLevel level;
		
		@Override
		public String getPath() {
			return genPath(String.format("mapsheets%ssheet-%s-%s", File.separator, sheet.getBladnr(), level.toString()));
		}
	}
}
