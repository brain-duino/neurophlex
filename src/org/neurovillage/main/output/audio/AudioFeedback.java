package org.neurovillage.main.output.audio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JSlider;

import org.neurovillage.main.output.feedback.Feedback;
import org.neurovillage.model.Config;
import org.neurovillage.model.FeedbackSettings;
import org.neurovillage.model.FocusFeedbackSettings;
import org.neurovillage.tools.ResourceManager;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.data.FloatSample;
import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.ports.QueueDataCommand;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.VariableRateDataReader;
import com.jsyn.unitgen.VariableRateMonoReader;
import com.jsyn.unitgen.VariableRateStereoReader;
import com.jsyn.util.SampleLoader;

public class AudioFeedback extends Feedback {
	
	private boolean multiModelFeedback = false;
	
	private Synthesizer synth;
	private LineOut lineOut;
	private FloatSample[] sample;
	private VariableRateDataReader[] samplePlayer;
	private int numSamples = 5;
	private float[] volume = {.5f,.5f,.5f,.5f,.7f};
	public float masterVolume = .95f;
	private float[] defaultVolume = {.5f,.5f,.5f,.5f, .7f};
	private String[] soundMixSamples = {"audio/pad_.wav", "audio/pad1.wav", "audio/lownoise.wav", "audio/pad2.wav", "audio/forest.wav" };
//	private String[] soundPlaybackSamples = {"audio/pad_.wav", "audio/pad1.wav", "audio/lownoise.wav", "audio/pad2.wav"};
	
	private JSlider[] volumeSliders;
	
	private BufferedImage feedbackMixImage;
	private Graphics2D feedbackMixGraphics;
	
	private int windowSize = 400;
	
	private int defaultWidth = windowSize;
	private int defaultHeight = windowSize;
	private int defaultOffset = 40;
	private BufferedImage colorImage;
	
	private int maxValX = defaultWidth;
	private int maxValY = defaultHeight;
	
	private Point currentMixPoint = new Point(defaultWidth/2, defaultHeight/2);
	
	private double rates[];
	private Config config;
	
	public AudioFeedback(FeedbackSettings feedbackSettings)
	{
		this(feedbackSettings, null);
	}
	
	public AudioFeedback(FeedbackSettings feedbackSettings, Config config) {
		super(feedbackSettings);
		this.config = config;
		
		if (this.config!=null)
		{
			for (int i = 0; i < numSamples; i++)
			{
				soundMixSamples[i] = this.config.getPref(Config.audiofeedback, String.valueOf(Config.audiofeedback_params.sample) + i);
				volume[i] = Float.valueOf(config.getPref(Config.audiofeedback, String.valueOf(Config.audiofeedback_params.volume) + i));
			}
			int x = Integer.valueOf(config.getPref(Config.audiofeedback, String.valueOf(Config.audiofeedback_params.x) ));
			int y = Integer.valueOf(config.getPref(Config.audiofeedback, String.valueOf(Config.audiofeedback_params.y) ));
			currentMixPoint.setLocation(x,y);
		}
		
		try
		{
			colorImage = ImageIO.read(ResourceManager.getInstance().getResource("4colors.jpg"));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		feedbackMixImage = new BufferedImage(defaultWidth, defaultHeight+defaultOffset, BufferedImage.TYPE_INT_ARGB);
		feedbackMixGraphics = (Graphics2D)feedbackMixImage.getGraphics();
		
		audioFeedbackFrame = new JFrame()
		{
			@Override
			public void paint(Graphics g)
			{
//				super.paint(g);
				feedbackMixGraphics.setStroke(new BasicStroke(2f));
				feedbackMixGraphics.setColor(Color.BLACK);
				feedbackMixGraphics.drawImage(colorImage, 0, 0, null);
				feedbackMixGraphics.drawOval(currentMixPoint.x-5, currentMixPoint.y-5, 10, 10);
				feedbackMixGraphics.setColor(Color.GRAY);
				feedbackMixGraphics.fillRect(0, defaultHeight, defaultWidth, defaultOffset);
				
//				feedbackMixGraphics.fillRect(0,0,defaultWidth,defaultHeight);
				g.drawImage(feedbackMixImage, 0, 0, null);
				
			}
		};
//		audioFeedbackFrame.setUndecorated(true);
		audioFeedbackFrame.setSize(defaultWidth, defaultHeight);
		audioFeedbackFrame.setVisible(true);
		audioFeedbackFrame.setResizable(false);
		audioFeedbackFrame.addMouseMotionListener(new MouseMotionListener()
		{
			
			@Override
			public void mouseMoved(MouseEvent e)
			{
			}
			
			@Override
			public void mouseDragged(MouseEvent e)
			{
				int newX = e.getX();
				int newY = e.getY();
				
				if (newX>maxValX)
					newX=maxValX;
				else if (newX<0)
					newX=0;
				if (newY>maxValY)
					newY=maxValY;
				else if (newY<0)
					newY=0;
				
				volume[0] = (float)Math.max(windowSize-(Math.sqrt(newX*newX + newY*newY + 1)),0.01f)/(float)windowSize;
				volume[1] = (float)Math.max(windowSize-(Math.sqrt((windowSize-newX)*(windowSize-newX) + newY*newY + 1)),0.01f)/(float)windowSize;
				volume[2] = (float)Math.max(windowSize-(Math.sqrt(newX*newX + (windowSize-newY)*(windowSize-newY) + 1)),0.01f)/(float)windowSize;
				volume[3] = (float)Math.max(windowSize-(Math.sqrt((windowSize-newX)*(windowSize-newX) + (windowSize-newY)*(windowSize-newY) + 1)),0.01f)/(float)windowSize;
				
				for (int i = 0; i < numSamples; i++)
				{
					samplePlayer[i].amplitude.set(volume[i]);
				}
				
				if (AudioFeedback.this.config!=null)
				{
					for (int i = 0; i < numSamples; i++)
						AudioFeedback.this.config.setPref(Config.audiofeedback, String.valueOf(Config.audiofeedback_params.volume) + i, String.valueOf(volume[i]));
					AudioFeedback.this.config.setPref(Config.audiofeedback, String.valueOf(Config.audiofeedback_params.x), ""+newX);
					AudioFeedback.this.config.setPref(Config.audiofeedback, String.valueOf(Config.audiofeedback_params.y), ""+newY);					
					AudioFeedback.this.config.store();
				}
				currentMixPoint.setLocation(newX, newY);
				audioFeedbackFrame.repaint();
			}
		});
		
		
		// JSynThread jsynThread = new JSynThread();
		// jsynThread.run();
	}
	
	public void displayGui(boolean show)
	{
		audioFeedbackFrame.setVisible(show);
	}
	
	public JFrame getGui()
	{
		return audioFeedbackFrame;
	}

	@Override
	public void run() {
		
		sample = new FloatSample[numSamples];
		synth = JSyn.createSynthesizer(AudioDeviceFactory.createAudioDeviceManager(true));
		rates = new double[numSamples];
		
		volume = new float[numSamples];
		samplePlayer = new VariableRateDataReader[numSamples];
		lineOut = new LineOut();
		this.synth.add(lineOut);
		
		int loopStartFrame = Integer.MAX_VALUE;
		int loopSize = Integer.MAX_VALUE;
		
		
		for (int i = 0; i < numSamples; i++)
		{
			// this.synth = jsynThread.getSynth();
	
			try {
	
				this.sample[i] = SampleLoader.loadFloatSample(ResourceManager.getInstance().getResource(soundMixSamples[i]));
			} catch (IOException e) {
				e.printStackTrace();
			}
	
			if (sample[i].getChannelsPerFrame() == 1) {
				synth.add(samplePlayer[i] = new VariableRateMonoReader());
				samplePlayer[i].output.connect(0, lineOut.input, 0);
			} else if (sample[i].getChannelsPerFrame() == 2) {
				synth.add(samplePlayer[i] = new VariableRateStereoReader());
				samplePlayer[i].output.connect(0, lineOut.input, 0);
				samplePlayer[i].output.connect(1, lineOut.input, 1);
			} else {
				throw new RuntimeException("Can only play mono or stereo samples.");
			}
	
			loopStartFrame = (int) (sample[i].getNumFrames() * 0.2);
			loopSize = (int) (sample[i].getNumFrames() * 0.7);
//			loopStartFrame = Math.min((int) (sample[i].getNumFrames() * 0.2), loopStartFrame);
//			loopSize = Math.min((int) (sample[i].getNumFrames() * 0.7), loopStartFrame);
	
			samplePlayer[i].rate.set(rates[i] = sample[i].getFrameRate() / 2);
			// Start at arbitrary position near beginning of sample.
	
			this.synth.start();
			// Start the LineOut. It will pull data from the oscillatorrftrfggftrf.
			lineOut.start();
	
//			samplePlayer[i].amplitude.set(0);
	
			// Queue attack portion of sample.
			samplePlayer[i].dataQueue.queue(sample[i], 0, loopStartFrame);
	
//			if ((loopStartFrame + loopSize) > sample[i].getNumFrames()) {
//				loopSize = sample[i].getNumFrames() - loopStartFrame;
//			}
			
			samplePlayer[i].amplitude.set(volume[i]*masterVolume);
			volume[i] = defaultVolume[i];
			
			int crossFadeSize = (int) (2000);
			
			// For complex queuing operations, create a command and then customize
			// it.
			QueueDataCommand command = samplePlayer[i].dataQueue.createQueueDataCommand(sample[i], loopStartFrame, loopSize);
			command.setNumLoops(-1);
			command.setSkipIfOthers(true);
			command.setCrossFadeIn(crossFadeSize);
			
			System.out.println("Queue: " + loopStartFrame + ", #" + loopSize + ", X=" + crossFadeSize);
			synth.queueCommand(command);
		}
//		oldFeedbacks = new double[this.feedbackSettings.getFFTData().binRangesAmount.length];
//		if (this.feedbackSettings.getFFTData().binRangesAmount.length<numSamples)
//		{
//			for (i = numS)
		samplePlayer[3].amplitude.set(0);
//		}


		running = true;

	}
	
	double oldFeedbacks[];
	
	double lastFeedback = 0d;
	public JFrame audioFeedbackFrame;
	private double oldValue = 0d;
	
	long lastTimestamp = 0;
	int minimumTime = 20;

	@Override
	public void updateCurrentFeedback(double currentFeedback) {
		
		super.updateCurrentFeedback(currentFeedback);
		
//		if (currentFeedback>oldValue)
			currentFeedback = oldValue *.9d + currentFeedback*.1d;	
//		else
//			currentFeedback = oldValue *.9995d + currentFeedback*.0005d;

		if (multiModelFeedback)
		{
			
			for (int b = 0; b < this.feedbackSettings.getFFTData().binRangesAmount.length; b++)
			{
				double feedback = 0;
				for (int c = 0; c < this.feedbackSettings.getFFTData().numChannels; c++)
				{
					double rewardBin = this.feedbackSettings.getFFTData().rewardFFTBins[b][c];
					feedback += rewardBin;
				}
				feedback /= (double) this.feedbackSettings.getFFTData().numChannels;
				feedback = Math.max(Math.min(.95d, feedback),0d);
				feedback = oldFeedbacks[b] * .9d + feedback*.1d;
//				System.out.println("feedback " + b + ":" + feedback);
				if (!Double.isNaN(feedback))
					feedback = oldFeedbacks[b] * .9d + feedback*.1d;
//				if (feedback>0d)
//					feedback = Math.sqrt(feedback);
//				samplePlayer[b].amplitude.set( feedback * this.feedbackSettings.getSensitivity() );
				samplePlayer[b].amplitude.set( Math.sqrt(feedback)*masterVolume  );
				oldFeedbacks[b] = feedback;
			}
//			FocusFeedbackSettings fs = (FocusFeedbackSettings)this.feedbackSettings;
		}
		else
		{
			for (int i = 0; i < 4; i++)
			{
	//			System.out.println(currentFeedback);
				if (!Double.isNaN(currentFeedback))
				{
//					samplePlayer[i].amplitude.set( ( 1d-Math.max(Math.sqrt(currentFeedback), .05d))*volume[i] );
					samplePlayer[i].amplitude.set(Math.max(Math.sqrt(currentFeedback), .003125d)*volume[i]*masterVolume);
				}
				else
				{
//					samplePlayer[i].amplitude.set( ( 1d-Math.max(Math.sqrt(oldValue*.9999d), .05d))*volume[i] );
					samplePlayer[i].amplitude.set( ( Math.max(Math.sqrt(oldValue*.9999d), .003125d))*volume[i]*masterVolume);
//					samplePlayer[i].amplitude.set(Math.max(Math.sqrt(currentFeedback), .003125d)*volume[i]);
				}
			}
			
		}
		if (currentFeedback>.2)
		for (int i = 0; i < numSamples; i++)
		{
			rates[i]+=1.1;
			samplePlayer[i].rate.set(rates[i]);
		}
	
//		System.out.println(currentFeedback + " - " + oldValue);
		if (!Double.isNaN(currentFeedback))
			oldValue = currentFeedback;
		else
			oldValue = oldValue *.995;
		// samplePlayer.amplitude.set(Math.max(Math.min(1d, currentFeedback),
		// 0d));
	}
	
	public void setMasterVolume(float masterVolume) {
		this.masterVolume = masterVolume;
		samplePlayer[4].amplitude.set( masterVolume * volume[4]);
	}

}
