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
	
	private static final String PDOKDOWNLOADURL = "https://download.pdok.nl/rws/ahn3/v1_0";
	
	public String getDownloadUrl(QualityLevel level) {
		switch (level) {
			case FIVEBYFIVE:
				return String.format("%s/5m_dsm/R5_%s.ZIP", PDOKDOWNLOADURL, bladnr.toUpperCase());
			case HALFBYHALF:
				return String.format("%s/05m_dsm/R_%s.ZIP", PDOKDOWNLOADURL, bladnr.toUpperCase());
			case LAS:
				return String.format("%s/laz/C_%s.LAZ", PDOKDOWNLOADURL, bladnr.toUpperCase());
		}
		throw new IllegalArgumentException();
	}
	
}
