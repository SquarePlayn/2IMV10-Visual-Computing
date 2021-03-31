package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

public class MouseCaptureAdapter implements MouseMotionListener, FocusListener, KeyListener, MouseListener {
	private boolean captured = false;
	private final Component component;
	private Robot robot = null;
	private Point targetPoint = null;
	private Cursor oldCursor = null;
	private final Cursor customCursor;
	private final Collection<Listener> listeners = new ArrayList<>();
	
	public interface Listener {
		void capturedMouseMoved(MouseEvent event);
	}
	
	public MouseCaptureAdapter(Component component) {
		this.component = component;
		customCursor = component.getToolkit().createCustomCursor(
			new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ),
			new Point(),
			null );
	}
	
	public void attach() {
		component.addMouseMotionListener(this);
		component.addFocusListener(this);
		component.addKeyListener(this);
		component.addMouseListener(this);
	}
	
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public boolean isCaptured() {
		return captured;
	}
	
	public void setCaptured(boolean captured) throws AWTException {
		if (captured && !this.captured) {
			targetPoint = new Point((int)component.getBounds().getCenterX(), (int)component.getBounds().getCenterY());
			SwingUtilities.convertPointToScreen(targetPoint, component);
			robot = new Robot(component.getGraphicsConfiguration().getDevice());
			robot.mouseMove((int)targetPoint.getX(), (int)targetPoint.getY());
			oldCursor = component.getCursor();
			component.setCursor(customCursor);
		} else if (!captured) {
			robot = null;
			targetPoint = null;
			component.setCursor(oldCursor);
			oldCursor = null;
		}
		this.captured = captured;
	}
	
	@Override
	public void mouseDragged(MouseEvent mouseEvent) {
	
	}
	
	@Override
	public void mouseMoved(MouseEvent mouseEvent) {
		if (captured) {
			if (mouseEvent.getXOnScreen() != (int)targetPoint.getX() || mouseEvent.getYOnScreen() != (int)targetPoint.getY()) {
				robot.mouseMove((int) targetPoint.getX(), (int) targetPoint.getY());
				mouseEvent.translatePoint(-(int)component.getBounds().getCenterX(), -(int)component.getBounds().getCenterY());
				for (Listener listener : listeners) {
					listener.capturedMouseMoved(mouseEvent);
				}
			}
		}
	}
	
	@Override
	public void focusGained(FocusEvent focusEvent) {
	
	}
	
	@SneakyThrows
	@Override
	public void focusLost(FocusEvent focusEvent) {
		setCaptured(false);
	}
	
	@Override
	public void keyTyped(KeyEvent keyEvent) {
	
	}
	
	@SneakyThrows
	@Override
	public void keyPressed(KeyEvent keyEvent) {
		if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
			setCaptured(false);
		}
	}
	
	@Override
	public void keyReleased(KeyEvent keyEvent) {
	
	}
	
	@Override
	public void mouseClicked(MouseEvent mouseEvent) {
	
	}
	
	@SneakyThrows
	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
			setCaptured(false);
		}
		if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
			setCaptured(!isCaptured());
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent mouseEvent) {
	
	}
	
	@Override
	public void mouseEntered(MouseEvent mouseEvent) {
	
	}
	
	@SneakyThrows
	@Override
	public void mouseExited(MouseEvent mouseEvent) {
		//Intentionally not de-capturing to handle *extreme* mouse movements.
		//setCaptured(false);
	}
}
