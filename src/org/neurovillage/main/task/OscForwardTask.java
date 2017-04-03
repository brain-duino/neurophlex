package org.neurovillage.main.task;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JSlider;
import javax.swing.JTextField;

import org.neurovillage.main.NFBServer;
import org.neurovillage.main.network.OSCForwarder;
import org.neurovillage.model.Config;
import org.neurovillage.model.DefaultFFTData;
import org.neurovillage.model.FFTData;
import org.neurovillage.model.OSCForwardMask;
import org.neurovillage.model.Task;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

public class OscForwardTask implements Task {

	private float defaultMin = -2f;
	private float defaultMax = 3f;

	private DatagramSocket socket = null;
	private String address;
	private String port;
	private String oscAddress;
	private NFBServer nfbServer;
	private String[] outputs;

	private float minValues[];
	private float maxValues[];
	private float rangeValues[];

	private DefaultFFTData fftData;

	private int mode = 0;

	public synchronized void setMinVal(int index, float value) {
		minValues[index] = value;
		rangeValues[index] = maxValues[index] - minValues[index];
	}

	public synchronized void setMaxVal(int index, float value) {
		maxValues[index] = value;
		rangeValues[index] = maxValues[index] - minValues[index];
	}

	public synchronized void setOutputString(int index, String string) {
		outputs[index] = string;
	}

	// 0 = per channel per bin
	// 1 = per channel per bin
	private int forwardMode = 0;

	private OSCForwarder oscForwarder;
	private OSCForwardMask oscForwardMask;
	private boolean connected = false;
	private Config config;

	public OscForwardTask(NFBServer nfbServer) {
		this.nfbServer = nfbServer;
		this.config = nfbServer.getConfig();
		this.oscForwarder = new OSCForwarder();
		this.oscForwardMask = new OSCForwardMask(this, nfbServer.getFftData(), nfbServer.getConfig());
	}

	public boolean connect(String address, String port) {
		this.address = address;
		this.port = port;
		return connected = oscForwarder.connect(address, port);
	}

	@Override
	public void init() {
		System.err.println("init osc forward");
	}

	public boolean disconnect() {
		return oscForwarder.disconnect();
	}

	@Override
	public void run() {

		if (!oscForwardMask.isVisible()) // INIT ////////////////////////////////////// INIT //////////////
		{
			this.fftData = nfbServer.getFftData();
			this.oscForwardMask.init(nfbServer.getFftData());
			int size = fftData.numChannels * fftData.bins;
			System.out.println("size:" + size);
			minValues = new float[size];
			maxValues = new float[size];
			rangeValues = new float[size];
			outputs = new String[size];

			
			
			for (int b = 0; b < fftData.bins; b++) {
				for (int c = 0; c < fftData.numChannels; c++) {
					int cb = c * fftData.bins + b;
					float minVal =  Float.valueOf(config.getPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.address) + cb + "min" ));
					float maxVal =  Float.valueOf(config.getPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.address) + cb + "max" ));		
					
					minValues[cb] = minVal;
					maxValues[cb] = maxVal;
					rangeValues[cb] = maxVal - minVal;
					outputs[cb] = oscForwardMask.getOutputs()[cb].getText();
				}
			}
		}
		
		if (connected)
		{

			OSCBundle bundle = new OSCBundle();
			try {
				for (int b = 0; b < fftData.bins; b++) {
					for (int c = 0; c < fftData.numChannels; c++) {
						int cb = c * fftData.bins + b;
						// System.out.println(fftData.rewardFFTBins.length + "" +
						// fftData.rewardFFTBins[c].length);
						OSCMessage msg = new OSCMessage(outputs[cb]);
						Object argument = new Object();
						float val = ((float) fftData.rewardFFTBins[b][c] - minValues[cb]) / rangeValues[cb];
						argument = val;
						
						oscForwardMask.setVal(cb, val);
						
						// System.out.println("value:" + argument + " to " +
						// outputs[cb]);
						msg.addArgument(argument);
						bundle.addPacket(msg);
						// oscForwarder.forwardMessage(msg);
						// bundle.addPacket(packet);
					}
				}
				oscForwarder.forwardBundle(bundle);
			} catch (java.lang.IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		// System.out.println();
		// oscPortOut.

	}

	public NFBServer getNfbServer() {
		return nfbServer;
	}

	@Override
	public void stop() {
		this.oscForwarder.disconnect();
	}

}
