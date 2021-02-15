package nl.tue.visualcomputingproject.group9a.project.chart.wfs;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import org.locationtech.jts.geom.MultiPolygon;

@Data
@AllArgsConstructor
public class MapSheet {
	MultiPolygon geom;
	String bladnr;
	boolean has_data_05m_dsm, has_data_5m_dsm, has_data_laz;
	
	private static final String PDOK_DOWNLOAD_URL = "https://download.pdok.nl/rws/ahn3/v1_0";
	
	public String getDownloadUrl(QualityLevel level) {
		switch (level) {
			case FIVE_BY_FIVE:
				return String.format("%s/5m_dsm/R5_%s.ZIP", PDOK_DOWNLOAD_URL, bladnr.toUpperCase());
			case HALF_BY_HALF:
				return String.format("%s/05m_dsm/R_%s.ZIP", PDOK_DOWNLOAD_URL, bladnr.toUpperCase());
			case LAS:
				return String.format("%s/laz/C_%s.LAZ", PDOK_DOWNLOAD_URL, bladnr.toUpperCase());
		}
		throw new IllegalArgumentException();
	}
	
}
