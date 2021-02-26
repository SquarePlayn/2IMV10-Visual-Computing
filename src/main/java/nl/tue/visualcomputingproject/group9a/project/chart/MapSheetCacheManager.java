package nl.tue.visualcomputingproject.group9a.project.chart;

import lombok.AllArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileIdFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileCacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.io.*;
import java.util.Objects;

public class MapSheetCacheManager {
	private final FileCacheManager cacheManager;
//	private final CacheFileStreamRequester<MapSheetFileId> requester;
	
	public MapSheetCacheManager(CachePolicy policy) {
		cacheManager = new FileCacheManager(policy, Settings.CACHE_DIR, "mapsheets");
		cacheManager.indexCache(MapSheetFileId.createFactory());
	}
	
	public boolean isSheetAvailable(MapSheet sheet, QualityLevel level) { // TODO: claim the ID beforehand as this can be expensive.
		MapSheetFileId id = new MapSheetFileId(sheet.getBladnr(), level);
		FileReadCacheClaim read = cacheManager.requestReadClaim(id);
		try {
			return read != null && read.exists();
		} finally {
			if (read != null) cacheManager.releaseCacheClaim(read);
		}
	}
	
	public OutputStream getOutputStream(MapSheet sheet, QualityLevel level)
			throws IOException { // TODO
		return null;
//		return requester.getOutputStream(new MapSheetFileId(sheet, level));
	}
	
	public InputStream getInputStream(MapSheet sheet, QualityLevel level)
			throws IOException { // TODO
		return null;
//		return requester.getInputStreamReleaseOnClose(new MapSheetFileId(sheet, level));
	}
	
	@AllArgsConstructor
	static class MapSheetFileId
			implements FileId {
		private static final String PRE = "ssheet";
		
//		private final MapSheet sheet; TODO: This is not necessary information in the ID. Most of it is data.
		private final String bladnr;
		private final QualityLevel level;
		
		@Override
		public String getPath() {
			return FileId.genPath(PRE, bladnr, level.getOrder()); // TODO: change if needed.
//			return FileId.genPath(String.format("ssheet-%s-%s", bladnr, level.getOrder()));
		}
		
		public static FileIdFactory<MapSheetFileId> createFactory() {
			return (String path) -> {
				String[] split = path.split(FileId.DELIM);
				if (split.length != 3 || !Objects.equals(split[0], PRE)) {
					return null;
				}
				String bladnr = split[1];
				QualityLevel level;
				try {
					level = QualityLevel.fromOrder(Integer.parseInt(split[2]));
				} catch (NumberFormatException e) {
					return null;
				}
				return new MapSheetFileId(bladnr, level);
			};
		}
	}
	
}
