package org.neurovillage.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.ImageGraphicAttribute;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import org.neurovillage.model.FeedbackSettings;
import org.neurovillage.model.Task;

public class NFBGraph implements Task {

	private FeedbackSettings feedbackSettings;
	private int windowWidth = 400;
	private int windowHeight = 600;
	private int segX = 4;
	private int segY = 3;
	private int stepX = 100;
	private int stepY = 40;
	
	private int additionalFeedbackHeight = 150;


	private BufferedImage feedbackImage;
	private Graphics2D feedbackGraphics;
	
	private BufferedImage graphImage;
	private Graphics2D graphics;
	
	private JFrame window;
	
	private double feedbackSamples[][][];
	private double totalFeedbackSamples[][][];
	private int pointer;
	private int[][] colorBins = {{255,0,0},{0,200,0},{0,0,200}, {255,180,0} };
	
	private int bins;
	private int channels;
	private int binHeight;
	private NFBServer nfbServer;
	private int sideBorder = 10;
	private int s;
	private long ts = -1;
	private boolean slowMode = false;

	public NFBGraph(NFBServer nfbServer, FeedbackSettings feedbackSettings) {

		this.feedbackSettings = feedbackSettings;
		this.nfbServer = nfbServer;
		this.init();
		

		window = new JFrame() {
			
			

			@Override
			public void paint(Graphics g) {
				
				feedbackGraphics.setColor(Color.darkGray);
				feedbackGraphics.fillRect(0, 0, windowWidth, windowHeight + additionalFeedbackHeight);
				feedbackGraphics.drawImage(graphImage, 0, 0, null);
				
				for (int p = 0; p < windowWidth; p++)
				{
					for (int b = 0; b < bins; b++)
					{
						
						
						feedbackGraphics.setColor(Color.white);
						for (int h = 0; h < segY+1; h++)
						{
							feedbackGraphics.drawLine(0, 30 +(30+binHeight)*b + h*stepY, windowWidth, 30 +(30+binHeight)*b + h*stepY);
						}
						for (int w = 0; w < segX; w++)
						{
//							feedbackGraphics.setColor(Color.darkGray);
							feedbackGraphics.drawLine(w*stepX, 30 +(30+binHeight)*b, w*stepX, 30 +(30+binHeight)*b + binHeight);
						}
						feedbackGraphics.setColor(Color.darkGray);
						feedbackGraphics.drawString("" + feedbackSettings.getBinLabels()[b], 20, 46+(30+binHeight)*b);
						feedbackGraphics.setColor(Color.white);
						feedbackGraphics.drawString("" + feedbackSettings.getBinLabels()[b], 20, 45+(30+binHeight)*b);
						
//						for (int c = 0; c < channels; c++)
//						{
//						}
					}
				}
				feedbackGraphics.setColor(Color.darkGray);
				feedbackGraphics.drawString("feedback", 20, 46+(30+binHeight)*bins);
				feedbackGraphics.setColor(Color.white);
				feedbackGraphics.drawString("feedback" , 20, 45+(30+binHeight)*bins);
				

				
//				feedbackGraphics.setColor(Color.darkGray);
//				feedbackGraphics.drawString("feedback", 20, 11+(30+binHeight)*(bins+1));
//				feedbackGraphics.setColor(Color.white);
//				feedbackGraphics.drawString("feedback" , 20, 10+(30+binHeight)*(bins+1));
				
//				super.paint(g);
				g.drawImage(feedbackImage, 0, 0, null);
				
			}

		};
		window.setSize(windowWidth, windowHeight + additionalFeedbackHeight);
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		
		window.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			@Override
			public void mousePressed(MouseEvent e) {
			}
			@Override
			public void mouseExited(MouseEvent e) {
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				slowMode = !slowMode;
			}
		});

	}

	@Override
	public void init() {

		this.feedbackImage = new BufferedImage(windowWidth, windowHeight + additionalFeedbackHeight, BufferedImage.TYPE_INT_ARGB);
		this.feedbackGraphics = (Graphics2D) feedbackImage.getGraphics();
		this.graphImage = new BufferedImage(windowWidth, windowHeight + additionalFeedbackHeight, BufferedImage.TYPE_INT_ARGB);
		this.graphics = (Graphics2D) graphImage.getGraphics();

		bins = feedbackSettings.getBins();
		System.out.println("fb settings bins: " + bins);
		channels = feedbackSettings.getNumChannels();
		feedbackSamples = new double[windowWidth][bins][channels];
		totalFeedbackSamples = new double[windowWidth][bins][channels];
		pointer = 0;
		binHeight = windowHeight/(bins+1) - (10*(bins+1));
		s = 255/(bins*channels);
		stepX = windowWidth / segX;
		stepY = binHeight / segY;

	}

	@Override
	public void run() {
		if ((slowMode) && (System.currentTimeMillis()-ts<75))
			return;
					
		if (nfbServer.getNumSamples() < 1)
			return;
		
		int numSamples = 1;
		
					
		ts = System.currentTimeMillis();
	
		graphics.copyArea(0, 0, windowWidth-sideBorder-numSamples, windowHeight+additionalFeedbackHeight, numSamples, 0);
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 30 + (30+binHeight)*bins, numSamples, binHeight + additionalFeedbackHeight);
		
		for (int b = 0; b < bins; b++)
		{
			graphics.setColor(Color.BLACK);
//			graphics.fillRect(0, 30 + (30+binHeight)*b, numSamples, binHeight + (b==bins-1?additionalFeedbackHeight:0));
			graphics.fillRect(0, 30 + (30+binHeight)*b, numSamples, binHeight);
//			System.out.println("bin range " + b + ":" + feedbackSettings.getFFTData().binRanges[(b*2)] + " - " + feedbackSettings.getFFTData().binRanges[(b*2)+1]);
			for (int c = 0; c < channels; c++) 
			{
				int lastVal = (int)feedbackSamples[pointer][b][c];
				
				pointer++;
				if (pointer>windowWidth-1)
					pointer = 0;
//				feedbackSamples[pointer][b][c] = feedbackSettings.getRewardFFTBins()[b][c];
				feedbackSamples[pointer][b][c] = feedbackSettings.getFFTData().currentFFTBins[b][c]/(feedbackSettings.getFFTData().bins-(float)b);
				
//				int val = (binHeight/3) + (int)(feedbackSamples[pointer][b][c]*(binHeight*5));
//				float frequencyFactor = Math.sqrt(40f-feedbackSettings.getFFTData().binRanges[b]/40d);
				int val = Math.min(binHeight,lastVal/2 + (int)feedbackSamples[pointer][b][c]/2);
//				System.out.println(val);
//				val = Math.max(0, val);
//				System.out.println(val+"");
//				System.out.println(feedbackSamples[pointer][b][c]);

				graphics.setColor(new Color(colorBins[b][0],colorBins[b][1],colorBins[b][2],170));
				graphics.fillRect(0, 30 + (30+binHeight)*b + (binHeight-val), numSamples, val);
				
				val  = Math.min(binHeight,(binHeight/3) + (int)(feedbackSettings.getRewardFFTBins()[b][c]*(binHeight*5)));
				
				graphics.setColor(new Color(colorBins[b][0],colorBins[b][1],colorBins[b][2],80));
				graphics.fillRect(0, 30 + (30+binHeight)*bins + (binHeight-val), numSamples, val);
				
//				graphics.fillRect(0, (30+binHeight)*b + (30+binHeight-val), numSamples, val);
				
			}
			if (b < bins-1)
			{
				graphics.setColor(Color.darkGray);
				graphics.fillRect(0, 30 + (30+binHeight)*b + (binHeight), numSamples, 30);
			}
		}
		
//		int val = Math.min(binHeight,(int)(feedbackSettings.getCurrentFeedback()*binHeight*5));
		int val = (int)(feedbackSettings.getCurrentFeedback()*binHeight*3);
		if (val>0)
		{
			graphics.setColor(new Color(0,255,100,230));
			graphics.fillRect(0, 30 + (30+binHeight)*bins + (binHeight-val), numSamples, val);
		}
		else
		{
			graphics.setColor(new Color(0,255,100,100));
			graphics.fillRect(0, 30 + (30+binHeight)*bins + binHeight, numSamples, -val);
		}
			
		
		
		window.repaint();

	}

	@Override
	public void stop() {

	}

}
