package nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities;

import lombok.Data;
import org.joml.Vector3f;

@Data
public class Light {
	private Vector3f position;
	private Vector3f color;

	public Light(Vector3f position, Vector3f color) {
		this.position = position;
		this.color = color;
	}
}
