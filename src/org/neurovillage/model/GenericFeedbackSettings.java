package org.neurovillage.model;

import java.util.concurrent.locks.ReentrantLock;

import org.neurovillage.main.MathBasics;
import org.neurovillage.main.NFBServer;

public class GenericFeedbackSettings extends FeedbackSettings
{
	protected int[] binRanges = { 4,7, 8, 11, 12,17, 18,35 };
	protected int[] binRangesAmount = { 4, 4, 6, 18 };
	
	@Override
	public String getFeedbackSettingsName()
	{
		return "generic";
	}

	public GenericFeedbackSettings(DefaultFFTData fftData, ReentrantLock lock, Config config)
	{
		super(fftData, lock, config);
		this.frame.move(NFBServer.width + (int)(NFBServer.border*1.5),0);
		
		this.binLabels = new String[]{ "theta", "lowalpha", "highalpha", "beta" };
		this.fftData.setBinRanges(binRanges);
		this.fftData.binLabels = this.binLabels;
		
	}

	@Override
	public void updateFeedback()
	{
		
		for (int c = 0; c < fftData.numChannels; c++)
			for (int b = 0; b < fftData.bins; b++)
				fftData.rewardFFTBins[b][c] = MathBasics.getZScore(fftData.shortMeanFFTBins[b][c], fftData.meanFFTBins[b][c], Math.sqrt(fftData.varFFTBins[b][c]));

		currentFeedback = 0;
		
		for (int c = 0; c < fftData.numChannels; c++)
		{
			for (int b = 0; b < binRangesAmount.length; b++)
			{
				double rewardBin = fftData.rewardFFTBins[b][c]*-1d;
				if (b==1)
					rewardBin*=-2d;
				currentFeedback += rewardBin;
			}
			currentFeedback /= (double) binRangesAmount.length;
			currentFeedback /= (float) (fftData.numChannels);
			lastFeedback = currentFeedback;
		}
		super.updateFeedback();
	}

}
