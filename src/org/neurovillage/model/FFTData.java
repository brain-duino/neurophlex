package org.neurovillage.model;

import org.neurovillage.main.MathBasics;

public interface FFTData
{
	public void updateFFTData(double[][] fftData);
	public void updateFFTData();
	public void setTrainingFactor(double trainingFactor);
	public double getTrainingFactor();
}
