package org.neurovillage.model;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.neurovillage.main.output.feedback.Feedback;


public abstract class FeedbackSettings
{
	protected int samplesPerSecond = 256;
	protected int numChannels = 2;
	protected int bins = 4;
	public String[] binLabels;
	protected int[] binRanges ;
	protected int[] binRangesAmount;

	protected double[][] rewardFFTBins = new double[bins][numChannels];
	protected double[][] lastRewardFFTBins = new double[bins][numChannels];
	
	protected float currentFeedback;
	protected float lastFeedback;

	protected boolean[] notBrainwaves = { false, false, false, false };
	protected boolean baseline = false;

	protected long currentTimestamp = System.currentTimeMillis();
//	protected ConcurrentLinkedDeque<double[]> currentData = new ConcurrentLinkedDeque<double[]>();
	protected ReentrantLock lock;
	
	protected ArrayList<Feedback> feedbacks;
	protected DefaultFFTData fftData;
	private ArrayList<Thread> feedbackThreads;
	private Config config;
	
	public String getFeedbackSettingsName()
	{
		return "generic";
	}

	public FeedbackSettings(DefaultFFTData fftData, ReentrantLock lock, Config config)
	{
		this.config = config;
		this.currentFeedback = this.lastFeedback = 0;
		this.feedbackThreads = new ArrayList<>();
		this.feedbacks = new ArrayList<>();
		this.fftData = fftData;
		this.lock = lock;
		
		
		this.init();
	}
	
	public void setFeedbacks(ArrayList<Feedback> feedbacks)
	{
		this.feedbacks = feedbacks;
	}
	
	public DefaultFFTData getFFTData()
	{
		return fftData;
	}
	
	private static float sensitivity = 0.5f;
	protected JFrame frame;
	
	public static float getSensitivity() {
		return sensitivity;
	}

	public void init()
	{
		currentFeedback = 0f;
		
		// FIXME redundant call, should be propagated from a launcher
		boolean proMode = ( Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.pro_mode) ))>0?true:false);
		
		if (proMode)
		{
			frame = new JFrame();
			frame.setSize(70, 600);
			frame.setLayout(new BorderLayout());
			
			frame.setTitle(getFeedbackSettingsName());
			JSlider slider = new JSlider(1,1000,50);
			slider.setOrientation(JSlider.VERTICAL);
			slider.addChangeListener(new ChangeListener() {
				
				@Override
				public void stateChanged(ChangeEvent e) {
					sensitivity = (float)((JSlider)e.getSource()).getValue()/100f;
	//				System.out.println(sensitivity);
					
				}
			});
			frame.add(new JLabel("sensitivity"), BorderLayout.NORTH);
			frame.add(slider, BorderLayout.CENTER);
			
			frame.setVisible(true);
			frame.setLocationRelativeTo(null);
		}
		
		
		
	}

	


	public int getNumChannels()
	{
		return numChannels;
	}

	public void setNumChannels(int numChannels)
	{
		this.numChannels = numChannels;
	}

	public int getBins()
	{
		return bins;
	}

	public void setBins(int bins)
	{
		this.bins = bins;
	}

	public String[] getBinLabels()
	{
		return binLabels;
	}

	public void setBinLabels(String[] binLabels)
	{
		this.binLabels = binLabels;
	}

	public int[] getBinRanges()
	{
		return binRanges;
	}

	public void setBinRanges(int[] binRanges)
	{
		this.binRanges = binRanges;
	}


	public double[][] getRewardFFTBins()
	{
		return fftData.rewardFFTBins;
	}

	public void setRewardFFTBins(double[][] rewardFFTBins)
	{
		this.rewardFFTBins = rewardFFTBins;
	}


	public float getCurrentFeedback()
	{
		return currentFeedback;
	}

	public void setCurrentFeedback(float currentFeedback)
	{
		this.currentFeedback = currentFeedback;
	}
	
	public void updateFeedback()
	{
		for (Feedback feedback : feedbacks)
		{
//			double resFB = 0d;
//			if (currentFeedback>0)
//				resFB = Math.sqrt(Math.sqrt(currentFeedback))*(sensitivity*12800d) ;
//			else
//				resFB = -Math.sqrt(Math.sqrt(Math.abs(currentFeedback)))*(sensitivity*12800d) ;
//			System.out.println("fb  :" + currentFeedback);
//			System.out.println("fbsq:" + resFB);
			feedback.updateCurrentFeedback(Math.sqrt(Math.sqrt(currentFeedback))*(sensitivity*2d));
		}
			
//			feedback.updateCurrentFeedback(Math.sqrt(Math.sqrt(currentFeedback))*sensitivity + currentFeedback*(1-sensitivity));
	}

	public void addFeedback(Feedback feedback)
	{
		if (!this.feedbacks.contains(feedback))
		{
			this.feedbacks.add(feedback);
			if (!feedback.isRunning())
			{
				Thread thread = new Thread(feedback);
				thread.run();
				this.feedbackThreads.add(thread);//TODO
			}
			
		}
		
	}

	public void base()
	{
		if ((fftData.baselineFFTValues!=null) && (fftData.meanFFTBins!=null))
		{
			for (int b = 0; b < fftData.bins; b++)
				fftData.baselineFFTValues[b] = fftData.meanFFTBins[b].clone();
		}
	}
	
	public ArrayList<Feedback> getFeedbacks()
	{
		return feedbacks;
	}

}

