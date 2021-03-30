package nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager;

import lombok.*;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;

import java.nio.ByteBuffer;

@Getter
@AllArgsConstructor
class Model {
	@NonNull
	private final ChunkPosition position;
	private MeshChunkData data = null;
	private QualityLevel quality = QualityLevel.FLAT;
	private ByteBuffer image = null;
	private int width = -1;
	private int height = -1;
	
	public Model(@NonNull ChunkPosition position) {
		this.position = position;
	}

	public Model(@NonNull ChunkPosition position, MeshChunkData data, QualityLevel quality) {
		this.position = position;
		this.data = data;
		this.quality = quality;
	}

	public Model(@NonNull ChunkPosition position, ByteBuffer image, int width, int height) {
		this.position = position;
		this.image = image;
		this.width = width;
		this.height = height;
	}
	
	public Model extract() {
		Model newModel = new Model(position, data, quality, image, width, height);
		data = null;
		image = null;
		return newModel;
	}

	public boolean hasNewData() {
		return data != null;
	}
	
	public boolean hasNewImage() {
		return image != null;
	}

	public boolean hasImage() {
		return width > 0 && height > 0;
	}

	public void setImage(ByteBuffer image, int width, int height) {
		this.image = image;
		this.width = width;
		this.height = height;
	}

	public boolean hasData() {
		return quality != QualityLevel.FLAT;
	}
	
	public void setData(MeshChunkData data, QualityLevel quality) {
		this.data = data;
		this.quality = quality;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Model)) return false;
		Model m = (Model) obj;
		return m.position.equals(position);
	}
	
	@Override
	public int hashCode() {
		return position.hashCode();
	}

}
