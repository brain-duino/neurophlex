package org.neurovillage.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;

import org.neurovillage.main.NFBServer.DrawTask;
import org.neurovillage.tools.ResourceManager;

public class PlaybackStream implements InputInterface
{

	static DataReceiver receiver;
	static int currentIndex = 0;
	private int numberOfChannels;
	private boolean loopPlayback = false;
	private Timer timer;
	static Random random = new Random();
	static ArrayList<ArrayList<double[]>> data;

	public PlaybackStream(DataReceiver receiver, int numberOfChannels, String playbackFile, boolean loop)
	{
		this.receiver = receiver;
		this.numberOfChannels = numberOfChannels;
		this.setPlaybackFile(playbackFile, loop);

	}
	
	@Override
	public boolean sendCommand(String string) {
		return true;
	}

	public void setPlaybackFile(String file, boolean loop)
	{
		System.out.println("file:" + file);
		File playbackFile = ResourceManager.getInstance().getResource(file);

		this.loopPlayback = loop;
		this.data = new ArrayList<ArrayList<double[]>>();

		if (!playbackFile.exists())
		{
			for (int i = 0; i < 256; i++)
			{
				System.out.println("i:" + i);
				ArrayList<double[]> currentSamples = new ArrayList<double[]>();
				double[] currentSample = new double[4];
				for (int c = 0; c < this.numberOfChannels; c++)
				{
					// currentSample[c] = -100d + random.nextDouble() * 200d;
					// x[i]+= 10d*Math.sin(((double)i / (double)width) * Math.PI
					// * comp2*2d);
					currentSample[c]  = 12.5d * Math.sin(((double) (i + c * 16) / 128d) * Math.PI * 6d);
//					currentSample[c] += 12.5d * Math.sin(((double) (i + c * 16) / 128d) * Math.PI * 1d);
//					currentSample[c] += 12.5d * Math.sin(((double) (i + c * 16) / 128d) * Math.PI * 10d);
					currentSample[c] += 12.5d * Math.sin(((double) (i + c * 16) / 128d) * Math.PI * 25d);
					
					// currentSample[c] += random.nextDouble()*50d;
					// currentSample[c] = random.nextDouble()*150d;
				}
				currentSamples.add(currentSample.clone());
				this.data.add(currentSamples);
			}
		} else
		{
//			System.out.println("got the file! " + playbackFile.toString());
			try
			{
				BufferedReader br = new BufferedReader(new FileReader(playbackFile.toString()));
				String line;
				while ((line = br.readLine()) != null)
				{
					ArrayList<double[]> currentSamples = new ArrayList<double[]>();
					double[] currentSample = new double[4];
					String[] values = line.split(",");
					if (values.length<3)
						continue;

					for (int c = 0; c < this.numberOfChannels; c++)
						currentSample[c] = Double.valueOf(values[c + 1]);

					currentSamples.add(currentSample.clone());
					this.data.add(currentSamples);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run()
	{
		timer = new Timer();
//		timer.schedule(new AppendDataTask(this), 500, 2);
		timer.schedule(new AppendDataTask(this), 500, 1);
		// timer.schedule(new AppendDataTask(this), 150, 100);
	}

	@Override
	public int shutDown()
	{
		return 0;
	}

	class AppendDataTask extends TimerTask
	{

		private PlaybackStream playbackStream;

		AppendDataTask(PlaybackStream playbackStream)
		{
			this.playbackStream = playbackStream;

		}

		public void run()
		{
			this.playbackStream.receiver.appendData(data.get(currentIndex));
			currentIndex++;
			if (currentIndex > data.size() - 1)
				currentIndex = 0;
		}
	}

	@Override
	public boolean isConnectionSuccessful()
	{
		return true;
	}

	@Override
	public boolean record(String filename) {
		return false;
	}

	@Override
	public void stopRecording() {
	}

	@Override
	public boolean isRecording() {
		return false;
	}

}
