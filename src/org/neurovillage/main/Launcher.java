package org.neurovillage.main;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.neurovillage.main.output.audio.AudioFeedback;
import org.neurovillage.main.output.visual.LongtermFFTVisualizer;
import org.neurovillage.main.output.visual.NeuroGameVisuals;
import org.neurovillage.main.output.visual.ZenSpaceVisuals;
import org.neurovillage.main.task.OscForwardTask;
import org.neurovillage.main.task.SerialForwardTask;
import org.neurovillage.model.Config;
import org.neurovillage.model.DefaultFFTData;
import org.neurovillage.model.FFTPreprocessor;
import org.neurovillage.model.FeedbackSettings;
import org.neurovillage.model.FocusFeedbackSettings;
import org.neurovillage.model.GenericFeedbackSettings;
import org.neurovillage.model.NFBProcessor;
import org.neurovillage.model.RelaxFeedbackSettings;
import org.neurovillage.model.Task;
import org.neurovillage.tools.ResourceManager;

public class Launcher extends JFrame
{
	// JButton btLaunchCollectiveOSCReceiver = new JButton();
	
	public static String defaultConfig = "./nfx.defaultprofile.ini";
	private JCheckBox cbBit24;
	private JCheckBox cbFromFile;
	private JLabel labelAudioFeedbackMask;
	private JCheckBox cbAudioFeedbackMask;
	private JCheckBox cbAdvancedMode;
	private boolean proMode;
	private boolean simulation;
	private boolean audioFeedback;
	private boolean is24bit;
	
	private JButton btLaunchVisualization;
	private JButton btLaunchGame;
	private JButton btLaunchOSC;
	private JButton btLaunchSerial;
	
	private JLabel labelIniFile;
	private JTextField iniFileDisplay;
	
	File iniFile;
	JTextField address = new JTextField("192.168.168.33");
	JTextField port = new JTextField("7075");
	           

	private JCheckBox cbResFromDisk;

	public static void main(String[] args)
	{
		Launcher launcher = new Launcher();
	}

	public Launcher()
	{
		// load config
		Config config = new Config(defaultConfig);
		ResourceManager.loadFromDisk = ( Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.load_from_disk) ))>0?true:false);
		System.out.println("load from disk: " + ResourceManager.loadFromDisk);
		proMode = ( Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.pro_mode) ))>0?true:false);
		audioFeedback = ( Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.audiofeedback) ))>0?true:false);
		simulation = ( Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.simulation) ))>0?true:false);
		is24bit = ( Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.bit24) ))>0?true:false);
		
		btLaunchGame = new JButton("focus");
		btLaunchVisualization = new JButton("relax"); 
		btLaunchOSC = new JButton("vj"); 
		btLaunchSerial = new JButton("serial"); 
		
		cbAdvancedMode = new JCheckBox("advanced mode");	
		cbAdvancedMode.setSelected(proMode);
		cbAdvancedMode.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (proMode!=cbAdvancedMode.isSelected())
				{
					proMode = cbAdvancedMode.isSelected();
					config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.pro_mode), proMode?"1":"0");					
					config.store();
				}
			}
		});
		
		cbResFromDisk = new JCheckBox("load resources from disk");
		cbResFromDisk.setSelected(ResourceManager.loadFromDisk );
		cbResFromDisk.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (ResourceManager.loadFromDisk !=cbResFromDisk.isSelected())
				{
					ResourceManager.loadFromDisk  = cbResFromDisk.isSelected();
					config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.load_from_disk), ResourceManager.loadFromDisk?"1":"0");					
					config.store();
				}
			}
		});		
		
		cbFromFile = new JCheckBox("simulation");
		cbFromFile.setSelected(simulation);
		cbFromFile.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (simulation!=cbFromFile.isSelected())
				{
					simulation = cbFromFile.isSelected();
					config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.simulation), simulation?"1":"0");					
					config.store();
				}
			}
		});		
		
		
		
		cbAudioFeedbackMask = new JCheckBox("audio feedback");
		cbAudioFeedbackMask.setSelected(audioFeedback);
		cbAudioFeedbackMask.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (audioFeedback!=cbAudioFeedbackMask.isSelected())
				{
					audioFeedback = cbAudioFeedbackMask.isSelected();
					config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.audiofeedback), audioFeedback?"1":"0");					
					config.store();
				}
			}
		});		
		
		
		cbBit24 = new JCheckBox("24 bit");
		cbBit24.setSelected(is24bit);
		cbBit24.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (is24bit!=cbBit24.isSelected())
				{
					is24bit = cbBit24.isSelected();
					config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.bit24), is24bit?"1":"0");					
					config.store();
				}
			}
		});			
		labelIniFile = new JLabel("config: ");
		iniFileDisplay = new JTextField();
		 
		btLaunchGame.addActionListener(new ActionListener() ///////////////////////////////// GAME ///////////////////////////////////////
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Launcher.this.btLaunchGame.setEnabled(false);
				Thread t = new Thread()
				{
					@Override
					public void run()
					{
						try 
						{
							NFBServer rn = new NFBServer(config, cbFromFile.isSelected(), false, cbBit24.isSelected(), proMode);

							// fft data and preprocessor task
							DefaultFFTData fftData = new DefaultFFTData(NFBServer.samplesPerSecond, NFBServer.bins, NFBServer.numChannels);
							FFTPreprocessor fftPreprocessor = new FFTPreprocessor(fftData, rn);

							// feedback settings and processor task
							FeedbackSettings currentFeedbackSettings = new FocusFeedbackSettings(fftData, rn.getLock(), config);
//							FeedbackSettings currentFeedbackSettings = new RelaxFeedbackSettings(fftData, rn.getLock());
							NFBProcessor nfbProcessor = new NFBProcessor(currentFeedbackSettings);

							NFBGraph nfbGraph = new NFBGraph(rn, currentFeedbackSettings);
							
							// file output task
//							FileOutputTask fileOutput = new FileOutputTask(rn);
//							fileOutput.setFileName("fileoutput"+System.currentTimeMillis()+".csv");
//							fileOutput.setWrite(true);
							
							
							if (cbAudioFeedbackMask.isSelected())
							{
								AudioFeedback audioFeedback = new AudioFeedback(currentFeedbackSettings, config);
								audioFeedback.displayGui(true);
								currentFeedbackSettings.addFeedback(audioFeedback);
							}
							rn.setCurrentFeedbackSettings(currentFeedbackSettings);
							
							// longterm fft visualization
							LongtermFFTVisualizer longtermFFTVisualizer = new LongtermFFTVisualizer(fftData,rn);
							
							LinkedList<Task> tasks = rn.getTasks();
							tasks.add(fftPreprocessor);
							tasks.add(nfbProcessor);
							tasks.add(longtermFFTVisualizer);
							tasks.add(nfbGraph);
//							tasks.add(fileOutput);
							rn.setTasks(tasks);

							NeuroGameVisuals ngvis = null;
							currentFeedbackSettings.addFeedback(ngvis  = new NeuroGameVisuals(rn.getCurrentFeedbackSettings()));
//							currentFeedbackSettings.addFeedback(new ZenSpaceVisuals(rn.getCurrentFeedbackSettings()));
										
							if (ngvis!=null)
								ngvis.getGameFrame().setVisible(true);
							nfbGraph.init();
							btLaunchVisualization.setEnabled(false);
							Launcher.this.setVisible(false);


						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						// new RunNFB(false, false);
					}
				};
//				Launcher.this.setVisible(false);
				t.start();
			}
		});


		////////////////////////////////////////////////////////////////////////// RELAX /////////////////////////
		btLaunchVisualization.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Launcher.this.btLaunchVisualization.setEnabled(false);
				Thread t = new Thread()
				{
					@Override
					public void run()
					{
						NFBServer rn = new NFBServer(config, cbFromFile.isSelected(), false, cbBit24.isSelected(), proMode);
						rn.setConfig(config);


						// fft data and preprocessor task
						DefaultFFTData fftData = new DefaultFFTData(NFBServer.samplesPerSecond, NFBServer.bins, NFBServer.numChannels);
						FFTPreprocessor fftPreprocessor = new FFTPreprocessor(fftData, rn);

						// feedback settings and processor task
						FeedbackSettings currentFeedbackSettings = new RelaxFeedbackSettings(fftData, rn.getLock(), config);
						NFBProcessor nfbProcessor = new NFBProcessor(currentFeedbackSettings);
						
						NFBGraph nfbGraph = new NFBGraph(rn, currentFeedbackSettings);

						// file output task
//						FileOutputTask fileOutput = new FileOutputTask(rn);
//						fileOutput.setFileName("defaultfile.csv");
//						fileOutput.setWrite(true);

						// longterm fft visualization
						LongtermFFTVisualizer longtermFFTVisualizer = new LongtermFFTVisualizer(fftData,rn);
						
						LinkedList<Task> tasks = rn.getTasks();
//						tasks.add(fileOutput);
						tasks.add(fftPreprocessor);
						tasks.add(nfbProcessor);
						tasks.add(nfbGraph);
						tasks.add(longtermFFTVisualizer);
						rn.setTasks(tasks);

//						currentFeedbackSettings.addFeedback(new AudioFeedback(rn.getCurrentFeedbackSettings()));
						
						if (cbAudioFeedbackMask.isSelected())
						{
							AudioFeedback audioFeedback = new AudioFeedback(currentFeedbackSettings, config);
							audioFeedback.displayGui(true);
							currentFeedbackSettings.addFeedback(audioFeedback);
						}
						rn.setCurrentFeedbackSettings(currentFeedbackSettings);
						currentFeedbackSettings.addFeedback(new ZenSpaceVisuals(currentFeedbackSettings));
						nfbGraph.init();
						
						btLaunchVisualization.setEnabled(false);
						Launcher.this.setVisible(false);


					}
				};
				Launcher.this.setVisible(false);
				t.start();
			}
		});
		
		btLaunchOSC.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Thread t = new Thread()
				{
					@Override
					public void run()
					{
						
						try ///////////////////////////////// VJ ///////////////////////////////////////
						{
							
							NFBServer rn = new NFBServer(config, cbFromFile.isSelected(), false, cbBit24.isSelected(), proMode);
							rn.setConfig(config);

//							rn.setMinimumNewSamples(32);

							// fft data and preprocessor task
							DefaultFFTData fftData = new DefaultFFTData(NFBServer.samplesPerSecond, NFBServer.bins, NFBServer.numChannels);
							FFTPreprocessor fftPreprocessor = new FFTPreprocessor(fftData, rn);

							// feedback settings and processor task
//							FeedbackSettings currentFeedbackSettings = new GenericAmplitudeFeedbackSettings(rn, fftData,rn.getLock(), config);
							FeedbackSettings currentFeedbackSettings = new GenericFeedbackSettings(fftData,rn.getLock(), config);
							NFBProcessor nfbProcessor = new NFBProcessor(currentFeedbackSettings);

//							 file output task
//							FileOutputTask fileOutput = new FileOutputTask(rn);
//							fileOutput.setFileName("fileoutput.csv");
//							fileOutput.setWrite(true);
							
//							// osc forward task + mask
							OscForwardTask oscForwardTask = new OscForwardTask(rn);
//							if (oscForwardTask.connect(address.getText(), port.getText()))
//							{
//								System.out.println("connection successful");
//							}
							
							
							// longterm fft visualization
							LongtermFFTVisualizer longtermFFTVisualizer = new LongtermFFTVisualizer(fftData,rn);

							LinkedList<Task> tasks = rn.getTasks();
							tasks.add(fftPreprocessor);
//							tasks.add(fileOutput);
							tasks.add(nfbProcessor);
							tasks.add(longtermFFTVisualizer);
							tasks.add(oscForwardTask);
							rn.setTasks(tasks);

							rn.setCurrentFeedbackSettings(currentFeedbackSettings);
							
							if (cbAudioFeedbackMask.isSelected())
							{
								AudioFeedback audioFeedback = new AudioFeedback(rn.getCurrentFeedbackSettings(), config);
								audioFeedback.displayGui(true);
								currentFeedbackSettings.addFeedback(audioFeedback);
							}
								
							btLaunchVisualization.setEnabled(false);
							Launcher.this.setVisible(false);



						} catch (Exception e)
						{
							e.printStackTrace();
						}
						// new RunNFB(false, false);
					}
				};
//				Launcher.this.setVisible(false);
				t.start();
			}
		});

		btLaunchSerial.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Thread t = new Thread()
				{
					@Override
					public void run()
					{
						
						try ///////////////////////////////// SERIAL ///////////////////////////////////////
						{
							
							NFBServer rn = new NFBServer(config, cbFromFile.isSelected(), false, cbBit24.isSelected(), proMode);

//							rn.setMinimumNewSamples(32);

							// fft data and preprocessor task
							DefaultFFTData fftData = new DefaultFFTData(NFBServer.samplesPerSecond, NFBServer.bins, NFBServer.numChannels);
							FFTPreprocessor fftPreprocessor = new FFTPreprocessor(fftData, rn);

							// feedback settings and processor task
							FeedbackSettings currentFeedbackSettings = new GenericFeedbackSettings(fftData, rn.getLock(), config);
							NFBProcessor nfbProcessor = new NFBProcessor(currentFeedbackSettings);

//							 file output task
//							FileOutputTask fileOutput = new FileOutputTask(rn);
//							fileOutput.setFileName("fileoutput.csv");
//							fileOutput.setWrite(true);
							
//							// osc forward task + mask
							SerialForwardTask serialForwardTask = new SerialForwardTask(rn);
//							OscForwardTask oscForwardTask = new OscForwardTask(rn);
//							if (oscForwardTask.connect(address.getText(), port.getText()))
//							{
//								System.out.println("connection successful");
//							}
							
							
							// longterm fft visualization
							LongtermFFTVisualizer longtermFFTVisualizer = new LongtermFFTVisualizer(fftData,rn);

							LinkedList<Task> tasks = rn.getTasks();
							tasks.add(fftPreprocessor);
//							tasks.add(fileOutput);
							tasks.add(nfbProcessor);
							tasks.add(longtermFFTVisualizer);
							tasks.add(serialForwardTask);
							rn.setTasks(tasks);

							rn.setCurrentFeedbackSettings(currentFeedbackSettings);
							
							if (cbAudioFeedbackMask.isSelected())
							{
								AudioFeedback audioFeedback = new AudioFeedback(rn.getCurrentFeedbackSettings(), config);
								audioFeedback.displayGui(true);
								currentFeedbackSettings.addFeedback(audioFeedback);
							}
								
							btLaunchVisualization.setEnabled(false);
							Launcher.this.setVisible(false);


						} catch (Exception e)
						{
							e.printStackTrace();
						}
						// new RunNFB(false, false);
					}
				};
//				Launcher.this.setVisible(false);
				t.start();
			}
		});		
		

		this.setLayout(new FlowLayout());
		
		JPanel programPanel = new JPanel(new FlowLayout());
		JPanel flagPanel = new JPanel();
		flagPanel.setLayout(new BoxLayout(flagPanel, BoxLayout.PAGE_AXIS));
//		flagPanel.setLayout(new GridLayout(5, 1));
		
		programPanel.add(btLaunchGame);
		programPanel.add(btLaunchVisualization);
		programPanel.add(btLaunchOSC);
		programPanel.add(btLaunchSerial);
		flagPanel.add(cbFromFile);
		flagPanel.add(cbResFromDisk);
//		this.add(labelAudioFeedbackMask = new JLabel("audio feedback mask:"));
		flagPanel.add(cbAudioFeedbackMask);
		flagPanel.add(cbBit24);
		flagPanel.add(cbAdvancedMode);
		flagPanel.setBorder(BorderFactory.createTitledBorder("settings"));
		this.add(flagPanel);
		programPanel.setBorder(BorderFactory.createTitledBorder("program"));
		this.add(programPanel);
		
		// btLaunchCollectiveOSCReceiver = new JButton("Launch Collective OSC
		// Receiver");
		this.setSize(400, 240);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		this.revalidate();
		this.setTitle("prototype");
		
	}

}
