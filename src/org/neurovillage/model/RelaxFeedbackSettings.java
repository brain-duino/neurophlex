package org.neurovillage.model;

import java.util.concurrent.locks.ReentrantLock;

import org.neurovillage.main.MathBasics;

public class RelaxFeedbackSettings extends FeedbackSettings
{
	private float lastFeedback;

	
	public double trainingFactor = 0.1;
	
	@Override
	public String getFeedbackSettingsName()
	{
		return "relax";
	}
	
	public RelaxFeedbackSettings(DefaultFFTData fftData, ReentrantLock lock, Config config)
	{
		super(fftData, lock, config);
		this.binLabels = new String[]{ "low theta", "theta", "alpha", "beta" };		
		this.binRanges = new int[]{ 3,4, 5,9, 10,12, 15,35 };
		this.binRangesAmount = new int[]{ 2, 5, 3, 21 };
		
//		for (int i = 0; i < this.binRanges.length; i++)
//			this.binRanges[i]*=2;
//		for (int i = 0; i < this.binRangesAmount.length; i++)
//			this.binRangesAmount[i]*=2;
		
//		this.binRanges = new int[]{ 3,4, 5,9, 10,12, 15,35 };
//		this.binRangesAmount = new int[]{ 2, 5, 3, 21 };
		this.fftData.setBinRanges(binRanges);
		this.fftData.binLabels = this.binLabels;
		
//		lastRewardFFTBins = new double[bins][numChannels];

	}
	
	@Override
	public void updateFeedback()
	{
		
		for (int c = 0; c < fftData.numChannels; c++)
			for (int b = 0; b < fftData.bins; b++)
			{
				double oldValue = fftData.rewardFFTBins[b][c];
				if (fftData.packagePenalty[c]>0)
				{
					fftData.rewardFFTBins[b][c] = oldValue * .15d;
					continue;
				}
				fftData.rewardFFTBins[b][c] = MathBasics.getZScore(fftData.shortMeanFFTBins[b][c], fftData.meanFFTBins[b][c], Math.sqrt(fftData.varFFTBins[b][c]));
//				if (fftData.rewardFFTBins[b][c]>1.25)
//					fftData.rewardFFTBins[b][c] = oldValue * .5d;
			}
		
		if (!Float.isNaN(currentFeedback))
			lastFeedback = currentFeedback;
		
		currentFeedback = 0;
		double totalReward = 0d;
		for (int c = 0; c < fftData.numChannels; c++)
		{
			for (int b = 0; b < binRangesAmount.length; b++)
			{
				double rewardBin = fftData.rewardFFTBins[b][c]*-1d;
				totalReward+=rewardBin;
				if (b==0)
					rewardBin*=0.25d;
				if ((b==1)||(b==2))
					rewardBin*=-3.75d;
				if (b==3)
					rewardBin*=6.75d;
				currentFeedback += rewardBin;
			}
		}
//		System.out.println(currentFeedback);
//		currentFeedback /= 8d;
		currentFeedback /= (double) binRangesAmount.length;
		currentFeedback /= (float) (fftData.numChannels);
		
//		System.out.println("sens:" + getSensitivity());
		currentFeedback *= getSensitivity();
		
//		currentFeedback = (float)totalReward*-1f;
//		if (currentFeedback>.05)
//			lastFeedback = lastFeedback*.9f + currentFeedback*.1f;
//		else
//		if (currentFeedback>.05)
//			currentFeedback = lastFeedback*.75f + currentFeedback*.25f;
//		else
//			currentFeedback = lastFeedback*.98f + currentFeedback*.02f;
			
		lastFeedback = currentFeedback;
		
/*
		lastFeedback = currentFeedback;
		currentFeedback = 0;
		for (int c = 0; c < numChannels; c++)
		{
			for (int b = 1; b < 3; b++)
			{
				if (Math.abs(lastRewardFFTBins[b][c]-rewardFFTBins[b][c])>.05)
					continue;
				currentFeedback += rewardFFTBins[b][c];
				lastRewardFFTBins[b][c] = rewardFFTBins[b][c];
			}
			currentFeedback/=(float)(numChannels);
		}
		if (currentFeedback>.05f)
			currentFeedback = lastFeedback*.85f + currentFeedback*.1f + .05f;
*/
		
//		currentFeedback = (float)Math.sqrt(Math.max(Math.min(1f, currentFeedback), 0f));
		super.updateFeedback();
	}
	

}
