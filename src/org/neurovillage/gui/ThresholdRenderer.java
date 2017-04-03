package org.neurovillage.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class ThresholdRenderer extends JPanel{
	
	
	private float min;
	private float max;
	private float range;
	private float cur;
	private float thresh;
	
	private int panelWidth;
	private int panelHeight;
	private BufferedImage renderImage;
	private Graphics2D renderGraphics;
	private Stroke stroke;
	private int pos;
	
	public synchronized void setRange(float min, float max) {
		this.min = min;
		this.max = max;
		this.range = Math.max(min,max) - Math.min(min,max);
		if (renderImage!=null)
			this.repaint();
	}
	
	public synchronized void setCur(float cur) {
		this.cur = cur;
		repaint();
	}
	
	public ThresholdRenderer(Dimension windowSize, float min, float max, float cur) {
		this.panelWidth = windowSize.width-16;
		this.panelHeight = windowSize.height;
		setRange(min, max);
		this.cur = cur;
		thresh = 0.5f;
		renderImage = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);
		renderGraphics = (Graphics2D) renderImage.getGraphics();
		stroke = new BasicStroke(2);
		renderGraphics.setStroke(stroke);
		pos = 0;
	}
	
	
	@Override
	public void paint(Graphics g) {
		
		renderGraphics.setColor(Color.WHITE);
		renderGraphics.fillRect(0, 0, panelWidth, panelHeight);
		renderGraphics.setColor(Color.BLACK);
		renderGraphics.drawRect(1, 1, panelWidth-1, panelHeight-1);
		
		
		pos = (int)((float)panelWidth*((cur-min)/range));
		renderGraphics.drawLine(pos, 0, pos, panelHeight);
		
//		thresh = (int)((float)panelWidth*((cur-min)/range));
//		renderGraphics.setColor(Color.BLUE);
//		renderGraphics.drawLine(pos, 0, pos, panelHeight);
		
		g.drawImage(renderImage, 0, 0, null);
	}

	public synchronized void setMin(float min) {
		setRange(min, max);
	}
	public synchronized void setMax(float max) {
		setRange(min, max);
	}


}
