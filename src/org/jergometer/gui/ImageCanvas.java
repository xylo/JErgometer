package org.jergometer.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Component that paints an image.
 */
public class ImageCanvas extends JComponent {
	private Image image;

	public ImageCanvas() {
	}

	public ImageCanvas(Image image) {
		setImage(image);
	}

	public ImageCanvas(Image image, Dimension d) {
		this.image = image.getScaledInstance((int) d.getWidth(), (int) d.getHeight(), Image.SCALE_DEFAULT);
		setPreferredSize(d);
	}

	protected void paintComponent(Graphics g) {
		g.drawImage(image,0,0,this);
	}


// getters and setters

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
		setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
	}
}
