package org.neurovillage.main.output.visual;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.neurovillage.main.NFBServer;
import org.neurovillage.model.DefaultFFTData;
import org.neurovillage.model.FeedbackSettings;
import org.neurovillage.model.Task;
import org.neurovillage.tools.ColorMap;

import com.jogamp.graph.font.FontFactory;

public class LongtermFFTVisualizer implements Task
{

	private JFrame window;
	private DefaultFFTData fftData;

	private BufferedImage ltVisImage;
	private Graphics2D ltVisGraphics;

	private BufferedImage[] channelImages;
	private Graphics2D[] channelGraphics;
	
	
	private int windowWidth = 800;
	private int windowHeight = 600;

	private int topOffset = 50;
	private int displayHeight = 550;

	private int numChannels = 2;
	private int heightPerChannel = displayHeight / numChannels;
	
	private int sideBorder = 150;
	private NFBServer nfbServer;
	
	private int fftHeight = 256;
	private int nSteps = 8;
	private int step = fftHeight/nSteps;
	
	private int playbackSpeed = 1;
	private int numPlaybackModes = 3;
	
	private int freqZoom = 4;
	private double maxFFT = 250;
	private double minFFT = 0;
//	private double maxFFT = Double.MIN_VALUE;
//	private double minFFT = Double.MAX_VALUE;


	private long lastTimestamp = 0l;
	private FeedbackSettings fbSettings;
	
	public LongtermFFTVisualizer(DefaultFFTData fftData, NFBServer nfbServer)
	{
		this.sampleCounter = new int[numPlaybackModes];
		this.nfbServer = nfbServer;
		this.fftData = fftData;
		this.numChannels = nfbServer.getNumChannels(); 
		this.displayHeight = windowHeight - topOffset;

		this.ltVisImage = new BufferedImage(windowWidth, displayHeight, BufferedImage.TYPE_INT_ARGB);
		this.ltVisGraphics = (Graphics2D) ltVisImage.getGraphics();
		
		channelImages = new BufferedImage[numChannels*numPlaybackModes];
		channelGraphics = new Graphics2D[numChannels*numPlaybackModes];
		
		this.fbSettings = nfbServer.getCurrentFeedbackSettings();
		
		for (int c=0; c < numChannels*numPlaybackModes ; c++)
		{
			channelImages[c] = new BufferedImage(windowWidth-sideBorder, displayHeight, BufferedImage.TYPE_INT_ARGB);
			channelGraphics[c] = (Graphics2D) channelImages[c].getGraphics();
			
//			channelGraphics[c].setColor(Color.PINK);
//			channelGraphics[c].fillRect(30, 30, 100+c*20, 100);
		}
		
		Font smallFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
		Font bigFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
		
		window = new JFrame()
		{
			
			


			@Override
			public void paint(Graphics g)
			{
				long currentTimestamp = System.currentTimeMillis();
				
				if ((currentTimestamp-lastTimestamp)<75)
					return;
				
				ltVisGraphics.setColor(Color.WHITE);
				ltVisGraphics.fillRect(0, topOffset, windowWidth, displayHeight);

				if (fftData != null) {
					numChannels = fftData.numChannels;
					heightPerChannel = displayHeight / numChannels;
					fftHeight = (fftData.windowSize/freqZoom)*2;
					step = (fftHeight / freqZoom) / nSteps;
				}
				
//				for (int c=numChannels*playbackSpeed; c < numChannels*(playbackSpeed+1) ; c++)
				for (int c=0; c< numChannels ; c++)
				{
//					ltVisGraphics.setColor(Color.GRAY);
//					ltVisGraphics.fillRect(0, topOffset + heightPerChannel*c, windowWidth-sideBorder, heightPerChannel);
//					ltVisGraphics.drawRect(0, topOffset + heightPerChannel*c, windowWidth-sideBorder, heightPerChannel);
					ltVisGraphics.drawImage(channelImages[c+(numChannels*playbackSpeed)], 0, topOffset+heightPerChannel*c, null);
					
					ltVisGraphics.setFont(bigFont);
					ltVisGraphics.setColor(Color.BLACK);
					ltVisGraphics.drawString("channel " + (c+1), 20, topOffset+heightPerChannel*c-7);
					
					ltVisGraphics.setFont(smallFont);
					for (int n = 0; n < 16; n++)
					{
						ltVisGraphics.drawString((n*step + 2) + " Hz",windowWidth-sideBorder/2+5,topOffset+heightPerChannel*c + (n*(fftHeight/8))+2);
						ltVisGraphics.drawLine(0,topOffset+heightPerChannel*c + (n*(fftHeight/8))+2,windowWidth-sideBorder/2,2+topOffset+heightPerChannel*c+ (n*(fftHeight/8)));
					}
					
					for (int n = 0; n < 16; n++)
					{
//						ltVisGraphics.drawString((n*step + 2) + " Hz",windowWidth-sideBorder/2+5,topOffset+heightPerChannel*c + (n*(fftHeight/8))+2);
						ltVisGraphics.drawLine(n*windowWidth/8, 25 + c*heightPerChannel, n*windowWidth/8, 25+ (c+1)*heightPerChannel);
					}

					
//					ltVisGraphics.drawLine(windowWidth-sideBorder,topOffset+heightPerChannel*c,windowWidth-sideBorder/2,topOffset+heightPerChannel*c);
					
					
				}
				g.drawImage(ltVisImage, 0, 0, null);
//				nfbServer.getNumSamples()

				// super.paint(g);
				lastTimestamp = currentTimestamp;
			}

		};
		window.setBackground(Color.WHITE);
		window.setSize(windowWidth, windowHeight);
		window.move(0, nfbServer.height + 120);
		window.setVisible(true);
		window.setResizable(false);
		
		window.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
			}
			@Override
			public void mousePressed(MouseEvent e)
			{
				
			}
			
			@Override
			public void mouseExited(MouseEvent e)
			{
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e)
			{
				playbackSpeed++;
				if (playbackSpeed>=numPlaybackModes)
					playbackSpeed = 0;
				window.repaint();
			}
		});
		
		JFrame controlPanel = new JFrame();
		controlPanel.setSize(80, windowHeight);
		controlPanel.setLocationRelativeTo(window);
		controlPanel.move(windowWidth, 0);
		controlPanel.setLayout(new BoxLayout(controlPanel.getContentPane(), BoxLayout.X_AXIS));
		
		JSlider sliderMin = new JSlider(SwingConstants.VERTICAL, -1000, 1000, 0);
		JSlider sliderMax = new JSlider(SwingConstants.VERTICAL, -1000, 5000, 30);
		
		controlPanel.add(sliderMin);
		controlPanel.add(sliderMax);
		
		sliderMin.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				minFFT = (double)sliderMin.getValue();
			}
		});
		sliderMax.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				maxFFT = (double)sliderMax.getValue();
			}
		});
		
		controlPanel.setVisible(false);
		ColorMap.getJet(256);

	}

	@Override
	public void init()
	{
		

	}
	
	int [] sampleCounter ;

	@Override
	public void run()
	{
		if (nfbServer.getNumSamples() < 1)
			return;
		
		int numSamples = nfbServer.getNumSamples();
		for (int i = 0; i < numPlaybackModes; i++)
		{
			sampleCounter[i]+=numSamples;
//			System.out.println("samples["+i+"]: " + sampleCounter[i]);
		}
		
		
//		if (playbackSpeed==0)
		{
		
			for (int c=0; c < numChannels ; c++)
				channelGraphics[c].copyArea(0, 0, windowWidth-sideBorder-1, displayHeight, 1, 0);
			
			for (int s=0; s < numSamples; s++)
			{
				for (int c=0; c < numChannels ; c++)
				{
					
	//				System.out.println(fftData.currentFFTValue[c]);
					for (int f = 0; f < fftData.currentFFTs[c].length/8 ; f++)
					{
//						System.out.println(fftData.currentFFTs[c][f]);
						
//						maxFFT = Math.max(maxFFT, fftData.currentFFTs[c][f]);
//						minFFT = Math.min(minFFT, fftData.currentFFTs[c][f]);
						
						int colorVal = (int)(((fftData.currentFFTs[c][f]-minFFT)/maxFFT)*255d);
						
						if (colorVal>255)
							colorVal = 255;
						else if (colorVal<0)
							colorVal = 0;
						
						channelGraphics[c].setColor(new Color(ColorMap.getColor(colorVal)));
						channelGraphics[c].fillRect(0, f*freqZoom*2, 1,freqZoom*2);
						
						
					}
				}
				if (this.fbSettings!=null)
				{
					int colorVal = (int)(this.fbSettings.getCurrentFeedback()*1024d);
					
					if (colorVal>255)
						colorVal = 255;
					else if (colorVal<0)
						colorVal = 0;
					channelGraphics[0].setColor(new Color(colorVal/2,colorVal,colorVal));
//					channelGraphics[0].fillRect(0, 0, 1,15);
					
				}
			}
		}
//		else
		for (int p = 0; p < numPlaybackModes; p++)
		{
			if (sampleCounter[p] >= (p+1)*nfbServer.getSamplesPerSecond()/4)
			{
//				if (p>0)
//					System.out.println("p>0: " + sampleCounter[p]);
				
//				for (int c=numChannels*playbackSpeed; c < numChannels*(playbackSpeed+1) ; c++)
				for (int c=0; c < numChannels ; c++)
				{
					channelGraphics[c+(numChannels*p)].copyArea(0, 0, windowWidth-sideBorder-1, displayHeight, 1, 0);
					
					for (int f = 0; f < fftData.currentFFTs[c].length/(freqZoom*2) ; f++)
					{
						int colorVal = (int)(((fftData.meanFFTs[c][f]-minFFT)/maxFFT)*255d);
						if (colorVal>255)
							colorVal = 255;
						else if (colorVal<0)
							colorVal = 0;
						
						channelGraphics[c+(numChannels*p)].setColor(new Color(ColorMap.getColor(colorVal)));
						channelGraphics[c+(numChannels*p)].fillRect(0, f*freqZoom*2, 1, freqZoom*2);
					}
				}
				if (this.fbSettings!=null)
				{
					int colorVal = (int)(this.fbSettings.getCurrentFeedback()*1024d);
					
					if (colorVal>255)
						colorVal = 255;
					else if (colorVal<0)
						colorVal = 0;
					channelGraphics[0+(numChannels*p)].setColor(new Color(colorVal/2,colorVal,colorVal));
//					channelGraphics[0+(numChannels*p)].fillRect(0, 0, 1,15);
					
				}
				
				sampleCounter[p] = sampleCounter[p] - ((p+1)*nfbServer.getSamplesPerSecond()/4);
			}
		}
		
		window.repaint();

	}

	@Override
	public void stop()
	{

	}

}
