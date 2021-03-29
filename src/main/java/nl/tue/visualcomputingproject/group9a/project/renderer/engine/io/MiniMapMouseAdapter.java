package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import lombok.Getter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.swing.MapPane;
import org.geotools.swing.event.MapMouseAdapter;
import org.geotools.swing.event.MapMouseEvent;

import javax.swing.*;
import java.awt.*;

public class MiniMapMouseAdapter extends MapMouseAdapter {
	@Getter
	private final MapPane mapPane;
	
	/** The default zoom increment */
	public static final double DEFAULT_ZOOM_FACTOR = 1.5;
	
	/** The working zoom increment */
	private double zoom;
	
	private Point panePos;
	private boolean panning;
	
	public MiniMapMouseAdapter(MapPane mapPane) {
		this.mapPane = mapPane;
		setZoom(DEFAULT_ZOOM_FACTOR);
	}
	
	/**
	 * Get the current areal zoom increment.
	 *
	 * @return the current zoom increment as a double
	 */
	public double getZoom() {
		return zoom;
	}
	
	/**
	 * Set the zoom increment
	 *
	 * @param newZoom the new zoom increment; values &lt;= 1.0 will be ignored
	 * @return the previous zoom increment
	 */
	public double setZoom(double newZoom) {
		double old = zoom;
		if (newZoom > 1.0d) {
			zoom = newZoom;
		}
		return old;
	}
	
	@Override
	public void onMouseWheelMoved(MapMouseEvent ev) {
		
		Rectangle paneArea = ((JComponent) getMapPane()).getVisibleRect();
		
		DirectPosition2D mapPos = ev.getWorldPos();
		
		double scaleX = getMapPane().getWorldToScreenTransform().getScaleX();
		double scaleY = getMapPane().getWorldToScreenTransform().getScaleY();
		int clicks = ev.getWheelAmount();
		
		double actualZoom = 1;
		// positive clicks are down - zoom out
		
		if (clicks > 0) {
			actualZoom = -1.0 / (clicks * getZoom());
		} else {
			actualZoom = clicks * getZoom();
		}
		double newScaleX = scaleX * actualZoom;
		double newScaleY = scaleY * actualZoom;
		
		DirectPosition2D corner =
			new DirectPosition2D(
				mapPos.getX() - 0.5d * paneArea.getWidth() / newScaleX,
				mapPos.getY() + 0.5d * paneArea.getHeight() / newScaleY);
		
		// I would prefer to offset the new map based on the cursor but this matches
		// the current zoom in/out tools.
		
		Envelope2D newMapArea = new Envelope2D();
		newMapArea.setFrameFromCenter(mapPos, corner);
		getMapPane().setDisplayArea(newMapArea);
	}
	
	/**
	 * Respond to a mouse button press event from the map mapPane. This may signal the start of a
	 * mouse drag. Records the event's window position.
	 *
	 * @param ev the mouse event
	 */
	@Override
	public void onMousePressed(MapMouseEvent ev) {
		panePos = ev.getPoint();
		panning = true;
	}
	
	/**
	 * Respond to a mouse dragged event.
	 *
	 * @param ev the mouse event
	 */
	@Override
	public void onMouseDragged(MapMouseEvent ev) {
		if (panning) {
			Point pos = ev.getPoint();
			if (!pos.equals(panePos)) {
				getMapPane().moveImage(pos.x - panePos.x, pos.y - panePos.y);
				panePos = pos;
			}
		}
	}
	
	/**
	 * If this button release is the end of a mouse dragged event, requests the map mapPane to
	 * repaint the display
	 *
	 * @param ev the mouse event
	 */
	@Override
	public void onMouseReleased(MapMouseEvent ev) {
		panning = false;
	}
	
}
