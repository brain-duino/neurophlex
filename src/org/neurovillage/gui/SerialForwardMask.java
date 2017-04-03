package org.neurovillage.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.neurovillage.main.network.SerialForwarder;
import org.neurovillage.main.task.OscForwardTask;
import org.neurovillage.main.task.SerialForwardTask;
import org.neurovillage.model.Config;
import org.neurovillage.model.DefaultFFTData;

public class SerialForwardMask extends JFrame
{
	private static final long serialVersionUID = 1L;
	private JSlider[] minSliders;
	private JSlider[] maxSliders;
	
	private ThresholdRenderer[] thresholdRenderers;
	
	private SerialForwarder serialForwarder;
	private DefaultFFTData fftData;
	DecimalFormat df = new DecimalFormat("#.##");

	private SerialForwardTask fwdTask;
	private Config config;
	private JTextField addressText;
	private JTextField baudText;
	
	public static int serialWidth = 580;
	public static int serialHeight = 700;
	private JLabel[] currentValLabels;
	


	public SerialForwardMask(SerialForwardTask fwdTask, DefaultFFTData fftData, Config config)
	{
		this.fwdTask = fwdTask;
		this.config = config;
		df.setRoundingMode(RoundingMode.CEILING);

		this.fftData = fftData;
		serialForwarder = new SerialForwarder();
		

	}
	public boolean connect()
	{
		return fwdTask.connect(addressText.getText(), baudText.getText());
	}
	
	public void setVal(int index, float val, int msgVal)
	{
		currentValLabels[index].setText(df.format(val) + " [" + msgVal +"]");
		thresholdRenderers[index].setCur(val);
//		currentMsgLabels[index].setText(msgVal+"");
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
				if (serialForwarder.isConnected())
				{
					serialForwarder.disconnect();
					try {
						Thread.sleep(300);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				boolean connectionSuccessful = connect();
//				System.out.println();
				JOptionPane.showMessageDialog(SerialForwardMask.this, "forwarding serial: " + (connectionSuccessful?"successful":"failed"), "Serial forwarder", JOptionPane.INFORMATION_MESSAGE);
				
				
			}
		});
		
		addressText = new JTextField(config.getPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.address) ));
		addressText.getDocument().addDocumentListener(new DocumentListener() {
			public void save() {
				if (config != null) {
					config.setPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.address), addressText.getText());
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
		baudText = new JTextField(config.getPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.baudrate) ));
		baudText.getDocument().addDocumentListener(new DocumentListener() {
			public void save() {
				if (config != null) {
					config.setPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.baudrate), baudText.getText());
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
		addressText.setPreferredSize(new Dimension((int)(serialWidth/2.3) , 14));
		
		JPanel ipPanel = new JPanel(new FlowLayout());
		ipPanel.add(new JLabel("address:"));
		ipPanel.add(addressText);
		
		JPanel portPanel = new JPanel(new FlowLayout());
		portPanel.add(new JLabel("br:"));
		portPanel.add(baudText);
		
		connectionPanel.add(ipPanel);
		connectionPanel.add(portPanel);
		connectionPanel.add(buttonConnect);
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		
		
		this.add(connectionPanel, BorderLayout.NORTH);
		
//		ipPanel.add(new JLabel("ip:"));

		minSliders = new JSlider[fftData.bins +1 ];
		maxSliders = new JSlider[fftData.bins +1 ];
		thresholdRenderers = new ThresholdRenderer[fftData.bins +1 ];
		currentValLabels = new JLabel[fftData.bins +1 ];
		
		for (int b = 0; b < fftData.bins+1; b++)
		{
//			for (int c = 0; c < fftData.numChannels; c++)
			{
				int bi = b;
				String msgAsString = config.getPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.message) + bi );
//				"/layer"+(ci+1)+"/clip1/video/opacity/values";
				
				float minVal =  Float.valueOf(config.getPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.message) + bi + "min" ));
				float maxVal =  Float.valueOf(config.getPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.message) + bi + "max" ));
				
				minSliders[bi] = new JSlider(SwingConstants.HORIZONTAL, 1, 500, Math.min(500,Math.max(1,(int)((minVal+2d)*100d))));
				maxSliders[bi] = new JSlider(SwingConstants.HORIZONTAL, 1, 500, Math.min(500,Math.max(1,(int)((maxVal+2d)*100d))));
				thresholdRenderers[bi] = new ThresholdRenderer(new Dimension(serialWidth,40),minVal, maxVal, 0f);
				
				currentValLabels[bi] = new JLabel("0");

				JPanel sliderPanel = new JPanel();
//				minSliderPanel.setBorder(BorderFactory.createTitledBorder("ch:" + c + " b:" + b));
				String label = "";
				if (b<fftData.bins)
					label = fwdTask.getNfbServer().getCurrentFeedbackSettings().binLabels[bi];
				else
					label = "feedback";
						
				sliderPanel.setBorder(BorderFactory.createTitledBorder(label));
				sliderPanel.setLayout(new BorderLayout());
				
				JPanel sliders = new JPanel();
				sliders.setLayout(new BoxLayout(sliders, BoxLayout.Y_AXIS));
				
				sliders.add(thresholdRenderers[bi]);
				sliders.add(minSliders[bi]);
				sliders.add(maxSliders[bi]);
				
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
				labels.add(currentValLabels[bi]);
				labels.add(maxLabel);
				
				
				
				minSliders[b].addChangeListener(new ChangeListener()
				{
					@Override
					public void stateChanged(ChangeEvent e)
					{
						float val = (((JSlider)e.getSource()).getValue()-200f) / 100f;
						minLabel.setText(df.format(val));
						fwdTask.setMinVal(bi, val);
						config.setPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.message) + bi + "min" , ""+val);
						config.store();
						thresholdRenderers[bi].setMin(val);
						
					}
				});
				
				maxSliders[b].addChangeListener(new ChangeListener()
				{
					@Override
					public void stateChanged(ChangeEvent e)
					{
						float val = (((JSlider)e.getSource()).getValue()-200f) / 100f;
						maxLabel.setText(df.format(val));
						fwdTask.setMaxVal(bi, val);
						config.setPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.message) + bi + "max" , ""+val);
						config.store();
						thresholdRenderers[bi].setMax(val);
					}
				});
						
				sliderPanel.add(labels, BorderLayout.NORTH);
				sliderPanel.add(sliders, BorderLayout.CENTER);
//				sliderPanel.add(outputs[ci], BorderLayout.SOUTH);
//				maxSliderPanel.add(maxLabel, BorderLayout.NORTH);
				
//				outputs[ci].getDocument().addDocumentListener(new DocumentListener() {
//					public void save() {
//						String string = outputs[ci].getText();
//						if (config != null) {
//							config.setPref(Config.serial_settings, String.valueOf(Config.serial_settings_params.message) + ci, string);
//							config.store();
//						}
//						if (string.length()>0)
//						{
//							fwdTask.setOutputString(ci, string);
//						}
//					}
//					@Override
//					public void changedUpdate(DocumentEvent arg0) {save();}
//					@Override
//					public void insertUpdate(DocumentEvent arg0) {save();}
//					@Override
//					public void removeUpdate(DocumentEvent arg0) {save();}
//				});
				settingsPanel.add(sliderPanel);
			}
		}
		this.add(settingsPanel, BorderLayout.CENTER);
		this.setSize(serialWidth, serialHeight);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Serial Forward Settings");
	}

}
