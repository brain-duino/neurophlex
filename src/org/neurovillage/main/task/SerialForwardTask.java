package org.neurovillage.main.task;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JSlider;
import javax.swing.JTextField;

import org.neurovillage.gui.SerialForwardMask;
import org.neurovillage.main.NFBServer;
import org.neurovillage.main.network.SerialForwarder;
import org.neurovillage.model.Config;
import org.neurovillage.model.DefaultFFTData;
import org.neurovillage.model.FFTData;
import org.neurovillage.model.OSCForwardMask;
import org.neurovillage.model.Task;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

public class SerialForwardTask implements Task {

	private float defaultMin = -2f;
	private float defaultMax = 3f;

	private int messageInterval = 75;

	private DatagramSocket socket = null;
	private String address;
	private String baudRate;
	private NFBServer nfbServer;
//	private String[] outputs;

	private float minValues[];
	private float maxValues[];
	private float rangeValues[];

	private DefaultFFTData fftData;

	long currentTimestamp = -1;
	long nextRenderTimestamp = -1;

	private int mode = 0;

	public synchronized void setMinVal(int index, float value) {
		minValues[index] = value;
		rangeValues[index] = maxValues[index] - minValues[index];
	}

	public synchronized void setMaxVal(int index, float value) {
		maxValues[index] = value;
		rangeValues[index] = maxValues[index] - minValues[index];
	}

//	public synchronized void setOutputString(int index, String string) {
//		outputs[index] = string;
//	}
	
	public synchronized void setCurrentFeedbackValues() {

	}

	// 0 = per channel per bin
	// 1 = per channel per bin
	private int forwardMode = 0;

	private SerialForwarder serialForwarder;
	private SerialForwardMask serialForwardMask;
	private boolean connected = false;
	private Config config;
	private int[] messages;
	private float[] vals;

	public SerialForwardTask(NFBServer nfbServer) {
		this.nfbServer = nfbServer;
		this.config = nfbServer.getConfig();
		this.serialForwarder = new SerialForwarder();
		this.serialForwardMask = new SerialForwardMask(this, nfbServer.getFftData(), nfbServer.getConfig());
	}

	public boolean connect(String address, String baudrate) {
		this.address = address;
		this.baudRate = baudrate;
		return connected = serialForwarder.connect(address, Integer.valueOf(baudrate));
	}

	@Override
	public void init() {
		System.err.println("init osc forward");
	}

	public boolean disconnect() {
		return serialForwarder.disconnect();
	}

	@Override
	public void run() {
		currentTimestamp = System.currentTimeMillis();
		if (!serialForwardMask.isVisible()) // INIT
		{
			this.fftData = nfbServer.getFftData();
			this.serialForwardMask.init(nfbServer.getFftData());
			int size = fftData.numChannels * fftData.bins;
			System.out.println("size:" + size);
			minValues = new float[size];
			maxValues = new float[size];
			rangeValues = new float[size];
//			outputs = new String[size];
			messages = new int[fftData.bins + 1];
			vals = new float[fftData.bins+1];

			for (int b = 0; b < fftData.bins; b++) {
				// for (int c = 0; c < fftData.numChannels; c++)
				{
					int cb = b;
					float minVal = Float.valueOf(config.getPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.message) + cb + "min"));
					float maxVal = Float.valueOf(config.getPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.message) + cb + "max"));
					minValues[cb] = minVal;
					maxValues[cb] = maxVal;
					rangeValues[cb] = maxVal - minVal;
//					outputs[cb] = serialForwardMask.getOutputs()[cb].getText();
				}
			}

		}

		if (connected) {
			if (currentTimestamp < nextRenderTimestamp)
				return;

			try {
				for (int b = 0; b < fftData.bins; b++)
				{
					vals[b] = (float)(fftData.rewardFFTBins[b][0] + fftData.rewardFFTBins[b][1]) / 2f;
					messages[b] = (int) (((vals[b] - minValues[b]) / rangeValues[b])*255f);
				}
				vals[fftData.bins] = nfbServer.getCurrentFeedbackSettings().getCurrentFeedback();
				messages[fftData.bins] = 127 + (int) (vals[fftData.bins] * 40f);
				serialForwarder.forwardMessage(messages);
				
				for (int b = 0; b < fftData.bins+1; b++)
					serialForwardMask.setVal(b, vals[b], messages[b]);
				
			} catch (java.lang.IllegalArgumentException e) {
				e.printStackTrace();
			}
			nextRenderTimestamp = currentTimestamp + messageInterval;
		}
		// System.out.println();
		// oscPortOut.

	}

	public NFBServer getNfbServer() {
		return nfbServer;
	}

	@Override
	public void stop() {
		this.serialForwarder.disconnect();
	}

}
