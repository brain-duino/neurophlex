package org.neurovillage.model;

import java.util.Iterator;

import org.jtransforms.fft.DoubleFFT_1D;
import org.neurovillage.main.BCISettings;
import org.neurovillage.main.MathBasics;
import org.neurovillage.main.NFBServer;
import org.neurovillage.tools.WindowFunction;

import com.jsyn.unitgen.FFT;

public class FFTPreprocessor implements Task
{
	private BCISettings bciSettings;
	private DoubleFFT_1D fft;
	protected DefaultFFTData fftData;
	private double[] filterWindow;
	
	private int numFIRFilterSamples = 10;
	private double avgValCount = 5;

	public FFTPreprocessor(DefaultFFTData fftData, BCISettings bciSettings)
	{
		this.bciSettings = bciSettings;
		this.fftData = fftData;
		init();
	}
	
	public void setNumFIRFilterSamples(int numFIRFilterSamples) {
		this.numFIRFilterSamples = numFIRFilterSamples;
	}
	public int getNumFIRFilterSamples() {
		return numFIRFilterSamples;
	}
	
	public void init()
	{
//		this.coeffs = MathBasics.getLPCoefficientsButterworth2Pole(bciSettings.getSamplesPerSecond(), 36d);
		this.fft  = new DoubleFFT_1D(bciSettings.getSamplesPerSecond());
		this.fftData.init(bciSettings.getSamplesPerSecond(), bciSettings.getBins(), bciSettings.getNumChannels());
		System.out.println("window size: " + this.fftData.windowSize + " / samples per second: " + bciSettings.getSamplesPerSecond());
		this.filterWindow = WindowFunction.generate(this.fftData.windowSize, WindowFunction.FunctionType.BLACKMAN);
	}
	
	public DefaultFFTData getFFTData()
	{
		return fftData;
	}
	
	@Override
	public void run()
	{
//		nfbServer.getC

		Iterator<double[]> currentDataIterator = bciSettings.getCurrentData().iterator();
//		System.out.println("num data points:" + bciSettings.getCurrentData().size());
//		if (bciSettings.getCurrentData().size()>200)
//			System.exit(02);

		int s = 0;
		double diff = 0;
		fftData.numChannels = bciSettings.getNumChannels();

		fftData.notBrainwaves = new boolean[bciSettings.getNumChannels()];
		while (currentDataIterator.hasNext())
		{
			double[] currentSamples = currentDataIterator.next();
			for (int c = 0; c < fftData.numChannels; c++)
			{
				
				fftData.windows[c][s] = currentSamples[c];
				if (numFIRFilterSamples<0)
				{
					if ((s > 10) && !fftData.notBrainwaves[c])
					{
						diff = Math.abs( (fftData.windows[c][s - 9]+fftData.windows[c][s - 8]+fftData.windows[c][s - 7]+fftData.windows[c][s - 6]+fftData.windows[c][s - 5] )/5
								        -(fftData.windows[c][s - 4]+fftData.windows[c][s - 3]+fftData.windows[c][s - 2]+fftData.windows[c][s - 1]+fftData.windows[c][s] )/5);
						
	//					diff = Math.abs(Math.abs(fftData.windows[c][s - 1] - fftData.windows[c][s]));
	//					diff = Math.abs(Math.max(fftData.windows[c][s - 3], fftData.windows[c][s]) - Math.min(fftData.windows[c][s - 2], fftData.windows[c][s]));
						if (diff > fftData.peakToPeakLimit)
						{
							fftData.notBrainwaves[c] = true;
							fftData.packagePenalty[c] = 16;
						}
					}
				}
			}
			s++;
		}

		for (int c = 0; c < fftData.numChannels; c++)
		{
//			System.out.println(fftData.windows[c].length + " length");
			double[] convData = null;
			if (numFIRFilterSamples>0)
			{
				double [] firFiltered = new double[fftData.windows[c].length];
				for (int i = 0; i < fftData.windows[c].length; i++)
				{
					for (int n = 0; n < Math.min(i,numFIRFilterSamples); n++)
						firFiltered[i] += fftData.windows[c][i-n];
					firFiltered[i] /= (double)Math.min(i+1, numFIRFilterSamples);
					
					if ((i>numFIRFilterSamples*2) && !fftData.notBrainwaves[c])
					{
//						diff = Math.abs( );
						for (int n = 0; n < avgValCount; n++)
							diff+=firFiltered[i-numFIRFilterSamples*2+n] - firFiltered[i-n];
						diff/=avgValCount*2;
							
						if (diff > fftData.peakToPeakLimit)
						{
							fftData.notBrainwaves[c] = true;
							fftData.packagePenalty[c] = 16;
						}
					}
					
	//				firFiltered
				}
				convData = WindowFunction.convolve(firFiltered,filterWindow);
			}
			else
				convData = WindowFunction.convolve(fftData.windows[c],filterWindow);
			
//			double[] convData = WindowFunction.convolve(MathBasics.filter(fftData.windows[c], this.coeffs),filterWindow);
//			double[] convData = fftData.windows[c].clone();
//			fftData.currentFFTs[c] = fftData.windows[c].clone();
//			fft.realForward(fftData.windows[c]);
//			fftData.currentFFTs[c] = fftData.windows[c].clone();
//			fftData.currentFFTs[c] = WindowFunction.convolveDefault(fftData.windows[c]).clone();
			
			fft.realForward(convData);
//			fftData.currentFFTs[c] = fftData.windows[c];
			fftData.currentFFTs[c] = convData.clone();

			int h = fftData.currentFFTs[c].length /2 ;

			fftData.currentFFTValue[c] = 0.1d;
			for (int v = 0; v < h-2; v++)
			{
				// calculate magnitude
				fftData.currentFFTs[c][v] = Math.sqrt(fftData.currentFFTs[c][v*2] * fftData.currentFFTs[c][v*2] + fftData.currentFFTs[c][(v*2) + 1] * fftData.currentFFTs[c][(v*2) + 1]);
				fftData.currentFFTPhases[c][v] = Math.atan(fftData.currentFFTs[c][v+1] / fftData.currentFFTs[c][v]);
				if ((v >= fftData.valueMin) && (v <= fftData.valueMax))
					fftData.currentFFTValue[c] += fftData.currentFFTs[c][v];
			}
		}
		fftData.updateFFTData();
	}

	@Override
	public void stop()
	{
		// TODO Auto-generated method stub
		
	}

	public void enableFIRFilter(boolean enable) {
		numFIRFilterSamples=Math.abs(numFIRFilterSamples);
		if (!enable)
			numFIRFilterSamples*=-1;
	}

}
