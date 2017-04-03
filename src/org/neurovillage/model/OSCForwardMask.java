package org.neurovillage.model;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.neurovillage.gui.ThresholdRenderer;
import org.neurovillage.main.network.OSCForwarder;
import org.neurovillage.main.task.OscForwardTask;

public class OSCForwardMask extends JFrame
{
	private static final long serialVersionUID = 1L;
	private JSlider[] minSliders;
	private JSlider[] maxSliders;
	
	private JTextField[] outputs;
	
	private OSCForwarder oscForwarder;
	private DefaultFFTData fftData;
	DecimalFormat df = new DecimalFormat("#.##");

	private OscForwardTask fwdTask;
	private Config config;
	private JTextField ipText;
	private JTextField portText;
	
	public static int oscWidth = 500;
	public static int oscHeight = 640;
	private JLabel[] currentValLabels;

	private ThresholdRenderer[] thresholdRenderers;


	public synchronized JTextField[] getOutputs()
	{
		return outputs;
	}


	public OSCForwardMask(OscForwardTask fwdTask, DefaultFFTData fftData, Config config)
	{
		this.fwdTask = fwdTask;
		this.config = config;
		df.setRoundingMode(RoundingMode.CEILING);

		this.fftData = fftData;
		oscForwarder = new OSCForwarder();
		

	}
	public boolean connect()
	{
		return fwdTask.connect(ipText.getText(), portText.getText());
	}
	
	public void setVal(int index, float val)
	{
		currentValLabels[index].setText(df.format(val));
		thresholdRenderers[index].setCur(val);
	}
	

	public void init(DefaultFFTData fftData)
	{
		this.fftData = fftData;
		this.setLayout(new BorderLayout());
		
		JPanel connectionPanel = new JPanel(new FlowLayout());
		
		JButton buttonConnect = new JButton("connect");
		buttonConnect.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (oscForwarder.isConnected())
					oscForwarder.disconnect();
				boolean connectionSuccessful = connect();
//				System.out.println();
				JOptionPane.showMessageDialog(OSCForwardMask.this, "forwarding OSC: " + (connectionSuccessful?"successful":"failed"), "OSC forwarder", JOptionPane.INFORMATION_MESSAGE);
				
				
			}
		});
		
		ipText = new JTextField(config.getPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.ip) ));
		ipText.getDocument().addDocumentListener(new DocumentListener() {
			public void save() {
				if (config != null) {
					config.setPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.ip), ipText.getText());
					config.store();
				}
			}
			@Override
			public void changedUpdate(DocumentEvent arg0) {save();}
			@Override
			public void insertUpdate(DocumentEvent arg0) {save();}
			@Override
			public void removeUpdate(DocumentEvent arg0) {save();}
		});
		portText = new JTextField(config.getPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.port) ));
		portText.getDocument().addDocumentListener(new DocumentListener() {
			public void save() {
				if (config != null) {
					config.setPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.port), portText.getText());
					config.store();
				}
			}
			@Override
			public void changedUpdate(DocumentEvent arg0) {save();}
			@Override
			public void insertUpdate(DocumentEvent arg0) {save();}
			@Override
			public void removeUpdate(DocumentEvent arg0) {save();}
		});
		ipText.setPreferredSize(new Dimension((int)(oscWidth/2.3) , 14));
		
		JPanel ipPanel = new JPanel(new FlowLayout());
		ipPanel.add(new JLabel("ip:"));
		ipPanel.add(ipText);
		
		JPanel portPanel = new JPanel(new FlowLayout());
		portPanel.add(new JLabel("port:"));
		portPanel.add(portText);
		
		connectionPanel.add(ipPanel);
		connectionPanel.add(portPanel);
		connectionPanel.add(buttonConnect);
		
		JPanel settingsPanel = new JPanel(new GridLayout(fftData.bins, fftData.numChannels));
		
		
		this.add(connectionPanel, BorderLayout.NORTH);
		
//		ipPanel.add(new JLabel("ip:"));

		minSliders = new JSlider[fftData.bins * fftData.numChannels];
		maxSliders = new JSlider[fftData.bins * fftData.numChannels];
		
		outputs = new JTextField[fftData.bins * fftData.numChannels];
		currentValLabels = new JLabel[fftData.bins * fftData.numChannels];
		thresholdRenderers = new ThresholdRenderer[fftData.bins * fftData.numChannels];
		
		for (int b = 0; b < fftData.bins; b++)
		{
			for (int c = 0; c < fftData.numChannels; c++)
			{
				int ci = c * fftData.bins + b;
				String msgAsString = config.getPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.address) + ci );
//				"/layer"+(ci+1)+"/clip1/video/opacity/values";
				
				double minVal =  Double.valueOf(config.getPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.address) + ci + "min" ));
				double maxVal =  Double.valueOf(config.getPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.address) + ci + "max" ));
				
				minSliders[ci] = new JSlider(SwingConstants.HORIZONTAL, 1, 500, (int)((minVal+2d)*100d));
				maxSliders[ci] = new JSlider(SwingConstants.HORIZONTAL, 1, 500, (int)((maxVal+2d)*100d));
				outputs[ci] = new JTextField(msgAsString);
				thresholdRenderers[ci] = new ThresholdRenderer(new Dimension(oscWidth,40),(float)minVal, (float)maxVal, 0f);
				
				currentValLabels[ci] = new JLabel("0");

				JPanel sliderPanel = new JPanel();
//				minSliderPanel.setBorder(BorderFactory.createTitledBorder("ch:" + c + " b:" + b));
				sliderPanel.setBorder(BorderFactory.createTitledBorder((c==0?"left":"right") + " " + fwdTask.getNfbServer().getCurrentFeedbackSettings().binLabels[b]));
				sliderPanel.setLayout(new BorderLayout());
				
				JPanel sliders = new JPanel();
				sliders.setLayout(new BoxLayout(sliders, BoxLayout.Y_AXIS));
				
				sliders.add(outputs[ci]);
				sliders.add(thresholdRenderers[ci]);
				sliders.add(minSliders[ci]);
				sliders.add(maxSliders[ci]);
				
//				JPanel maxSliderPanel = new JPanel();
//				maxSliderPanel.setBorder(BorderFactory.createEmptyBorder());
//				maxSliderPanel.setLayout(new BorderLayout());
//				maxSliderPanel.add(maxSliders[ci], BorderLayout.CENTER);				
				
				JLabel minLabel = new JLabel(df.format(minVal)); minLabel.setForeground(Color.RED);
				JLabel maxLabel = new JLabel(df.format(maxVal)); maxLabel.setForeground(Color.BLUE);
				JPanel labels = new JPanel();
				labels.setLayout(new GridLayout(2, 3));
				labels.add(new JLabel("min"));
				labels.add(new JLabel("val"));
				labels.add(new JLabel("max"));
				labels.add(minLabel);
				labels.add(currentValLabels[ci]);
				labels.add(maxLabel);
				
				
				
				minSliders[ci].addChangeListener(new ChangeListener()
				{
					@Override
					public void stateChanged(ChangeEvent e)
					{
						float val = (((JSlider)e.getSource()).getValue()-200f) / 100f;
						minLabel.setText(df.format(val));
						fwdTask.setMinVal(ci, val);
						config.setPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.address) + ci + "min" , ""+val);
						config.store();
						thresholdRenderers[ci].setMin(val);
						
					}
				});
				
				maxSliders[ci].addChangeListener(new ChangeListener()
				{
					@Override
					public void stateChanged(ChangeEvent e)
					{
						float val = (((JSlider)e.getSource()).getValue()-200f) / 100f;
						maxLabel.setText(df.format(val));
						fwdTask.setMaxVal(ci, val);
						config.setPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.address) + ci + "max" , ""+val);
						config.store();
						thresholdRenderers[ci].setMax(val);
					}
				});
						
				sliderPanel.add(labels, BorderLayout.NORTH);
				sliderPanel.add(sliders, BorderLayout.CENTER);
//				sliderPanel.add(outputs[ci], BorderLayout.SOUTH);
//				maxSliderPanel.add(maxLabel, BorderLayout.NORTH);
				
				outputs[ci].getDocument().addDocumentListener(new DocumentListener() {
					public void save() {
						String string = outputs[ci].getText();
						if (config != null) {
							config.setPref(Config.osc_settings, String.valueOf(Config.osc_settings_params.address) + ci, string);
							config.store();
						}
						if (string.length()>0)
						{
							fwdTask.setOutputString(ci, string);
						}
					}
					@Override
					public void changedUpdate(DocumentEvent arg0) {save();}
					@Override
					public void insertUpdate(DocumentEvent arg0) {save();}
					@Override
					public void removeUpdate(DocumentEvent arg0) {save();}
				});
				settingsPanel.add(sliderPanel);
			}
		}
		this.add(settingsPanel, BorderLayout.CENTER);
		this.setSize(oscWidth, oscHeight);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
