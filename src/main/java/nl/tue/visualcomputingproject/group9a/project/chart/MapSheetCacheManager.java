package nl.tue.visualcomputingproject.group9a.project.chart;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileIdFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileCacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.io.*;
import java.util.Objects;

public class MapSheetCacheManager {
	private final FileCacheManager cacheManager;
	
	public MapSheetCacheManager(CachePolicy policy) {
		cacheManager = new FileCacheManager(policy, Settings.CACHE_DIR, "mapsheets");
		cacheManager.indexCache(MapSheetFileId.createFactory());
	}
	
	public void releaseClaim(FileReadCacheClaim claim) {
		cacheManager.releaseCacheClaim(claim);
	}
	
	public FileReadCacheClaim attemptClaimSheet(MapSheet sheet, QualityLevel level) {
		MapSheetFileId id = new MapSheetFileId(sheet.getBladnr(), level);
		FileReadCacheClaim read = cacheManager.requestReadClaim(id);
		return read;
	}
	
	public FileReadWriteCacheClaim attemptClaimSheetWrite(MapSheet sheet, QualityLevel level) {
		MapSheetFileId id = new MapSheetFileId(sheet.getBladnr(), level);
		return cacheManager.requestReadWriteClaim(id);
	}
	
	public FileReadCacheClaim downgradeClaim(FileReadWriteCacheClaim claim) {
		return cacheManager.degradeClaim(claim);
	}
	
	@ToString
	@EqualsAndHashCode
	@AllArgsConstructor
	static class MapSheetFileId
			implements FileId {
		private static final String PRE = "sheet";
		
		private final String bladnr;
		private final QualityLevel level;
		
		@Override
		public String getPath() {
			return FileId.genPath(PRE, bladnr, level.getOrder(), ".LAZ");
		}
		
		public static FileIdFactory<MapSheetFileId> createFactory() {
			return (String path) -> {
				String[] split = path.split(FileId.DELIM);
				if (split.length != 4 || !Objects.equals(split[0], PRE)) {
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
