package org.neurovillage.main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.neurovillage.main.network.OSCForwarder;
import org.neurovillage.main.output.feedback.Feedback;
import org.neurovillage.main.output.visual.FocusOMeter;
import org.neurovillage.model.Config;
import org.neurovillage.model.DefaultFFTData;
import org.neurovillage.model.FFTPreprocessor;
import org.neurovillage.model.FeedbackSettings;
import org.neurovillage.model.FileOutputTask;
import org.neurovillage.model.Task;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;

public class NFBServer extends BCISettings implements DataReceiver {

	private static boolean sham = false;

	////////////////////////////////////////////////////////////////////////////

	public static int samplesPerSecond = 256;
	public static int numChannels = 2;
	private float minVal = -100f;
	private float maxVal = 100f;
	private float minValAbs = Math.abs(minVal);
	private float valRange = 200f;
	private String unit = "uV";
	public static boolean drawBins = true;


	// private double fftMin = -9000d;
	private double fftMinAbs = 9000d;
	// private double fftMax = -3000d;
	private double fftRange = 6000d;

	private boolean baseline = false;
	private double zoom = 2d;

	DecimalFormat df = new DecimalFormat("#.####");

	// private int bins = 5;
	// private String[] binLabels = {"theta", "low alpha", "high alpha", "beta",
	// "gamma"};
	// private int[] binRanges = {4,7, 8,10, 11,13, 14,30, 31,40};
	// private int[] binRangesAmount = {4, 3, 3, 17, 10};

	public static int bins = 4;
	public static String[] binLabels = { "theta", "lowalpha", "highalpha", "beta" };
	public static int[] binRanges = { 4, 7, 8, 10, 11, 13, 17, 30 };
	public static int[] binRangesAmount = { 4, 3, 3, 14 };

	public static float factor = 1f;

	private long penaltyTimestamp = 0l;
	
	public static int border = 40;

	private int spectralOffsetX = samplesPerSecond + border;
	private int binOffsetX = spectralOffsetX + border;

	private int curOffset = 0;
	private int windowOffsetY = 50;

	public static int width = 1320;
	public static int height = 500;

	private int visWidth = 512;
	private int visHeight = 400;
	private int displayRange = visHeight / numChannels;
	private int binDisplayWidth = displayRange / bins;

	boolean[] notBrainwaves = { false, false, false, false };

	// public static String playbackFile = "ABSPATH:/root/1111noisekirimbi";
	public static String playbackFile = "k24bit2.csv";// fileoutput1457537281935.csv

	// private FakeLock lock = new FakeLock();
	private static ReentrantLock lock = new ReentrantLock();

	private BufferedImage visImage;
	private Graphics2D visGraphics;
	private JFrame frame;
	private Graphics frameGraphics;

	private double[][] currentFFTs = new double[numChannels][samplesPerSecond];
	private double[][] windows = new double[numChannels][samplesPerSecond];

	private double[][] filteredTimeseries = new double[numChannels][samplesPerSecond];

	private Filter[] highPassFilters = new Filter[numChannels];
	private Filter[] lowPassFilters = new Filter[numChannels];

	private int[] highPass = { 8, 18 };
	private int[] lowPass = { 12, 30 };

	private LinkedList<Task> tasks;
	private LinkedList<JFrame> frames;

	public LinkedList<Task> getTasks() {
		return tasks;
	}

	public LinkedList<JFrame> getFrames() {
		return frames;
	}

	public void addFrame(JFrame frame) {
		frames.add(frame);
	}

	public void setTasks(LinkedList<Task> tasks) {
		this.tasks = tasks;

		for (Task task : tasks) {
			if (task instanceof FFTPreprocessor)
				this.fftData = ((FFTPreprocessor) task).getFFTData();
			if (task instanceof FileOutputTask)
				this.fileOutputTask = (FileOutputTask) task;
		}

	}

	InputInterface serialPortInterface = null;
	private FocusOMeter focusOMeter;

	private Timer timer;
	protected boolean collected;
	private boolean running = false;
	private JTextField textOSCPort;
	private JTextField textIP;
	private JTextField textSerialPort;
	private JTextField textSamplesPerSecond;
	private OSCForwarder oscForwarder;
	private JButton buttonBase;
	private JButton buttonMode;
	
	public InputInterface getSerialPortInterface() {
		return serialPortInterface;
	}

	// private ArrayList<DataReceiver> dataReceivers;

	private FeedbackSettings currentFeedbackSettings = null;
	protected ConcurrentLinkedDeque<double[]> currentData = new ConcurrentLinkedDeque<double[]>();
	// protected double[] currentFFTValue = new double[numChannels];

	protected long currentTimestamp = System.currentTimeMillis();
	protected int currentSamples = 0;
	protected int newSamples = 0;
	public int minimumNewSamples = 16;

	boolean recording = false;
	private ArrayList<FeedbackSettings> feedbackSettings;
	private int numSamples;
	private List<double[]> inputData;
	private DefaultFFTData fftData;
	private JButton buttonRecord;
	protected FileOutputTask fileOutputTask = null;
	private boolean b24 = false;
	protected boolean training;
	private Config config;

	private boolean drawRawTimeSeries = true;

	public void setMinimumNewSamples(int minimumNewSamples) {
		this.minimumNewSamples = minimumNewSamples;
	}

	public ConcurrentLinkedDeque<double[]> getCurrentData() {
		return currentData;
	}

	public int getBins() {
		return bins;
	}

	public int getSamplesPerSecond() {
		return samplesPerSecond;
	}

	public void setCurrentFeedbackSettings(FeedbackSettings newFeedbackSettings) {
		boolean firstTime = false;
		if (this.currentFeedbackSettings == null)
			firstTime = true;

		if (!firstTime) {
			ArrayList<Feedback> feedbacks = this.currentFeedbackSettings.getFeedbacks();
			for (Feedback feedback : feedbacks)
				newFeedbackSettings.addFeedback(feedback);
		}
		this.currentFeedbackSettings = newFeedbackSettings;

		this.bins = newFeedbackSettings.getBins();
		this.binLabels = newFeedbackSettings.getBinLabels();
		this.binRanges = newFeedbackSettings.getBinRanges();

		// if (firstTime)
		// new NFBSettingsWindow(this);
	}

	public FeedbackSettings getCurrentFeedbackSettings() {
		return currentFeedbackSettings;
	}


	private JButton buttonReset;
	private JButton buttonTrain;
	private boolean proMode;
	public JFrame buttonFrame;
	private JButton buttonFir;
	private long lastTimestamp;
	private boolean fir;
	private int pp;
	private int avg;
	


	public int getNumChannels() {
		return numChannels;
	}

	public NFBServer(boolean sham, boolean record) {
		this(sham, record, false);
	}

	public NFBServer(boolean sham, boolean record, boolean b24) {
		this(null, sham, record, b24, false);
	}

	public NFBServer(Config config, boolean sham, boolean record, boolean b24, boolean proMode) {
		this.config = config;
		this.sham = sham;
		this.b24 = b24;
		this.proMode = proMode;

		df.setRoundingMode(RoundingMode.CEILING);

		tasks = new LinkedList<>();
		frames = new LinkedList<>();
		feedbackSettings = new ArrayList<>();

		for (int c = 0; c < numChannels; c++) {
			lowPassFilters[c] = new Filter(lowPass[c], samplesPerSecond * 4, true, 50);
			highPassFilters[c] = new Filter(highPass[c], samplesPerSecond * 4, false, 200);
		}

		visImage = new BufferedImage(width, visHeight + 40, BufferedImage.TYPE_INT_ARGB);
		visGraphics = (Graphics2D) visImage.getGraphics();

		visGraphics.setColor(Color.GRAY);
		visGraphics.fillRect(20, 80, width - 80, height - 150);

		int[] x0 = new int[numChannels];
		int[] y0 = new int[numChannels];
		int[] x1 = new int[numChannels];
		int[] y1 = new int[numChannels];

		frame = new JFrame() {


			@Override
			public void paint(Graphics g) {

				if (!NFBServer.this.proMode)
					return;

				// super.paint(g);
				visGraphics.setColor(Color.WHITE);
				visGraphics.fillRect(0, 0, width, visHeight + 40);
				visGraphics.setColor(Color.BLACK);

				// Stream<double[]> currentSampleStream = currentData.stream();
				lock.lock();

				for (int c = 0; c < numChannels; c++) {
					int halfMin = (int) minValAbs / 2;
					curOffset = displayRange * c + halfMin;

					visGraphics.setColor(new Color(230, 255, 255));
					visGraphics.fillRect(0, curOffset, samplesPerSecond, displayRange);
					visGraphics.setColor(Color.BLACK);
					visGraphics.drawRect(0, curOffset, samplesPerSecond, displayRange);

					visGraphics.setColor(Color.GRAY);

					visGraphics.drawLine(0, curOffset + displayRange / 2, samplesPerSecond, curOffset + displayRange / 2);
					// visGraphics.drawLine(0, curOffset+halfMin/2,
					// samplesPerSecond, curOffset+halfMin/2);
					// visGraphics.drawLine(0, curOffset+halfMin,
					// samplesPerSecond, curOffset+halfMin);
					// visGraphics.drawLine(0, curOffset+halfMin*2,
					// samplesPerSecond, curOffset+halfMin*2);
				}

				// Iterator<double[]> currentDataIterator =
				// displayData.iterator();

				/*
				 * DRAW RAW TIME SERIES
				 */
				// for (int c = 0; c < numChannels; c++)

				if (drawRawTimeSeries) {
					visGraphics.setColor(Color.BLACK);
					visGraphics.setStroke(new BasicStroke(2f));

					Iterator<double[]> currentDataIterator = currentData.iterator();
					double meanValues[] = new double[numChannels];
					while (currentDataIterator.hasNext()) {
						double[] currentSamples = currentDataIterator.next();

						for (int c = 0; c < numChannels; c++)
							meanValues[c] += currentSamples[c];
					}
					for (int c = 0; c < numChannels; c++) {
						meanValues[c] /= currentData.size();
						// System.out.println(meanValues[c]);
					}

					currentDataIterator = currentData.iterator();

					int i = 0;
					while (currentDataIterator.hasNext()) {
						double[] currentSamples = currentDataIterator.next();
						for (int c = 0; c < numChannels; c++) {
							curOffset = displayRange * c;
							x1[c] = i;
							// y1[c] = (int) (displayRange * ((currentSamples[c]
							// - meanValues[c]) / valRange));
							y1[c] = (int) (((currentSamples[c] - meanValues[c])) * 4) + displayRange / 2;

							if (i == 0) {
								x0[c] = x1[c];
								y0[c] = y1[c];
							}

							visGraphics.drawLine(x0[c], windowOffsetY + curOffset + y0[c], x1[c], windowOffsetY + curOffset + y1[c]);
							x0[c] = x1[c];
							y0[c] = y1[c];
						}
						i++;
					}
				}

				/*
				 * DRAW SPECTRUM
				 */

				// TODO to put somewhere else

				if (fftData != null)
					for (int c = 0; c < numChannels; c++) {
						// visGraphics.drawString(""+currentFFTValue[c], 128,
						// windowOffsetY*2 + curOffset);

						visGraphics.setColor(Color.BLACK);
						curOffset = displayRange * c;

						// FREQUENCY VISUALIZATION
						double fftDisplayData[][];

						for (int d = 0; d < 2; d++) {
							if (d == 0) {
								fftDisplayData = fftData.meanFFTs;
								visGraphics.setColor(Color.BLUE);
							} else {
								fftDisplayData = fftData.currentFFTs;
								visGraphics.setColor(Color.BLACK);
							}

							for (int f = 0; f < fftDisplayData[c].length / 8; f++) {
								double yVal = 0d;
								if (f < 6)
									yVal = ((fftDisplayData[c][f] * .25d * zoom + fftMinAbs) / fftRange * 2d);
								else
									yVal = ((fftDisplayData[c][f] * 1.5d * zoom + fftMinAbs) / fftRange * 2d);

								x1[c] = spectralOffsetX + f * 8;
								y1[c] = (int) (displayRange * yVal);
								if (f == 0) {
									x0[c] = x1[c];
									y0[c] = y1[c];
								}
								visGraphics.drawLine(x0[c], -50 + windowOffsetY * 2 + curOffset + (int) (displayRange * 3.5) - y0[c], x1[c], -50 + windowOffsetY * 2 + curOffset + (int) (displayRange * 3.5) - y1[c]);
								x0[c] = x1[c];
								y0[c] = y1[c];
							}
						}


						visGraphics.setColor(Color.GRAY);
						int interleavingOffset = 0;
						for (int f = 0; f < fftData.currentFFTs[c].length / 4.5; f += 5) {
							visGraphics.drawLine(spectralOffsetX + f * 4, (int) (windowOffsetY * 1.5) + curOffset, spectralOffsetX + f * 4, (int) (windowOffsetY * 3.5) + curOffset + 10);
							if (f % 10 == 0)
								interleavingOffset = 5;
							else
								interleavingOffset = -05;
							visGraphics.drawString("" + f, spectralOffsetX + f * 4 - 8, windowOffsetY * 4 + curOffset - interleavingOffset);
						}

						int afterSpecX = spectralOffsetX + samplesPerSecond;
						int m = (int) ((fftData.meanFFTValue[c] / fftData.maxFFTValue[c]) * (double) displayRange);

						/*
						 * SHOW FFT POWER
						 */
						// visGraphics.setColor(Color.blue);
						// visGraphics.fillRect(afterSpecX, windowOffsetY +
						// curOffset, 100,
						// (int)((fftData.currentFFTValue[c]/fftData.maxFFTValue[c])*(double)displayRange));
						// visGraphics.setColor(Color.black);
						// visGraphics.drawLine(afterSpecX, windowOffsetY +
						// curOffset + m, afterSpecX + 100, windowOffsetY +
						// curOffset + m);
						// visGraphics.drawString("" +
						// (int)fftData.currentFFTValue[c], afterSpecX,
						// windowOffsetY + curOffset);
						// visGraphics.drawString("" +
						// (int)fftData.maxFFTValue[c], afterSpecX + 60,
						// windowOffsetY + curOffset);
						// visGraphics.drawString("" +
						// (int)fftData.meanFFTValue[c], afterSpecX + 30,
						// windowOffsetY + curOffset +20);

						/*
						 * DRAW FFT BINS
						 */
						// visGraphics.drawString("z-score", binOffsetX + 256,
						// curOffset + 45 );

						if ((currentFeedbackSettings == null) || (!running))
							continue;

						if (drawBins)
						{
							for (int b = 0; b < fftData.bins; b++) {
								if (fftData.packagePenalty[c] <= 0)
									visGraphics.setColor(new Color(0, 255 - b * 40, b * 40));
								else
									visGraphics.setColor(new Color(200, 40, 40));
	
								if (collected)
									visGraphics.setColor(visGraphics.getColor().brighter().brighter());
	
								int val = (int) (32d * ((fftData.meanFFTBins[b][c] + 1d) / (1d + fftData.baselineFFTValues[b][c]) * 2d));
								int curVal = (int) (32d * ((fftData.currentFFTBins[b][c] + 1d) / (1d + fftData.baselineFFTValues[b][c]) * 2d));
								int avgVal = (int) (32d * ((fftData.shortMeanFFTBins[b][c] + 1d) / (1d + fftData.baselineFFTValues[b][c]) * 2d));
	
								visGraphics.fillRect(binOffsetX + 216, curOffset + 40 + binDisplayWidth * b, val, binDisplayWidth / 2);
								visGraphics.setColor(new Color(0, 0, 0, 50));
								visGraphics.fillRect(binOffsetX + 216 + curVal, curOffset + 40 + binDisplayWidth * b, 2, binDisplayWidth / 2);
								visGraphics.setColor(new Color(0, 0, 0));
								visGraphics.fillRect(binOffsetX + 216 + avgVal, curOffset + 40 + binDisplayWidth * b, 2, binDisplayWidth / 2);
	
								visGraphics.drawString(currentFeedbackSettings.getBinLabels()[b] + ":", binOffsetX + 216, curOffset + 35 + binDisplayWidth * b);
								// visGraphics.drawString("" +
								// df.format(currentFeedbackSettings.getRewardFFTBins()[b][c]),
								// binOffsetX + 285, curOffset + 39 +
								// binDisplayWidth * b);
	
							}
							
						}
					}
				// super.paint(g);
				if (drawRawTimeSeries)
					g.drawImage(visImage, 0, 0, null);
				else
					g.drawImage(visImage, -samplesPerSecond, 0, null);

				lock.unlock();

			}
		};

		frame.setLayout(new BorderLayout());
		frameGraphics = frame.getGraphics();

		textIP = new JTextField("127.0.0.1");
		// textIP = new JTextField("192.168.100.155");
		textIP.setBackground(new Color(100, 255, 170));

		textOSCPort = new JTextField("7009");
		textOSCPort.setBackground(new Color(100, 255, 170));

		// textSerialPort = new JTextField("COM4");
		textSerialPort = new JTextField("/dev/rfcomm0");
		textSerialPort.setBackground(new Color(100, 255, 170));

		textSerialPort.getDocument().addDocumentListener(new DocumentListener() {
			public void save() {
				if (config != null) {
					config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.serial_address), textSerialPort.getText());
					config.store();
				}
			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				save();
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				save();
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				save();
			}
		});

		// textSamplesPerSecond = new JTextField("256");
		// textSamplesPerSecond.setEnabled(false);
		// textSamplesPerSecond.addKeyListener(new KeyListener() {
		//
		// @Override
		// public void keyTyped(KeyEvent e) {
		//
		// }
		//
		// @Override
		// public void keyReleased(KeyEvent e) {
		// NFBServer.this.samplesPerSecond =
		// Integer.valueOf(textSamplesPerSecond.getText());
		// }
		//
		// @Override
		// public void keyPressed(KeyEvent e) {
		//
		// }
		// });

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		JButton buttonCollect = new JButton("collect");
		buttonCollect.setPreferredSize(new Dimension(100, 30));
		buttonCollect.setEnabled(false);

		buttonBase = new JButton("base");
		buttonBase.setPreferredSize(new Dimension(100, 30));
		buttonBase.setEnabled(true);
		buttonBase.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentFeedbackSettings != null) {
					baseline = true;
					currentFeedbackSettings.base();

				}
			}
		});

		buttonTrain = new JButton("train");
		buttonTrain.setPreferredSize(new Dimension(100, 30));
		buttonTrain.setEnabled(false);
		buttonTrain.setBackground(Color.cyan.darker());

		// buttonPeakCheck = new JButton("peak check");
		// buttonPeakCheck.setPreferredSize(new Dimension(100, 30));
		// buttonPeakCheck.setEnabled(false);
		// buttonPeakCheck.setBackground(Color.cyan.darker());

		buttonReset = new JButton("reset");
		buttonReset.setPreferredSize(new Dimension(100, 30));
		buttonReset.setEnabled(false);

		buttonMode = new JButton("mode=I");
		buttonMode.setPreferredSize(new Dimension(100, 30));
		buttonMode.setEnabled(false);

		buttonRecord = new JButton("record");
		buttonRecord.setPreferredSize(new Dimension(100, 30));
		buttonRecord.setEnabled(true);
		buttonRecord.setBackground(new Color(60, 255, 120));

		fir = ( Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.fir) ))>0?true:false);
		buttonFir = new JButton("fir=" + fir);
		buttonFir.setPreferredSize(new Dimension(100, 30));
		buttonFir.setEnabled(true);
		buttonFir.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (buttonFir.getText().contains("true"))
				{
					buttonFir.setText("fir=false");
					fir = false;
				}
				else
				{
					buttonFir.setText("fir=true");
					fir = true;
				}
				
				for (Task task : tasks)
				{
					if (task instanceof FFTPreprocessor)
						((FFTPreprocessor)task).enableFIRFilter(fir);
				}
				config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.fir), fir?"1":"0");
				config.store();
			}
		});

		
		JButton buttonConnect = new JButton("connect");
		buttonConnect.setPreferredSize(new Dimension(100, 30));

		JButton buttonForward = new JButton("forward");
		buttonForward.setPreferredSize(new Dimension(100, 30));
		// buttonPanel.add(new JLabel(" serial port: "));

		buttonPanel.setBackground(new Color(0, 200, 126));
		buttonPanel.add(new JLabel(" serial port: "));
		buttonPanel.add(textSerialPort);
		buttonPanel.add(buttonConnect);
		if (proMode) {
			buttonPanel.add(buttonBase);
			buttonPanel.add(buttonReset);
			if(b24)
				buttonPanel.add(buttonMode);
			buttonPanel.add(buttonRecord);
			buttonPanel.add(buttonFir);
			
		}
		// buttonPanel.add(textSamplesPerSecond);

		JPanel parameterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		parameterPanel.setBackground(Color.GREEN);

		JSlider freqZoomSlider = new JSlider(JSlider.HORIZONTAL, 1, 800, 200);
		JLabel freqZoomLabel = new JLabel("freq zoom: 0.5x");
		freqZoomSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				zoom = (double) freqZoomSlider.getValue() / 100d;
				freqZoomLabel.setText("freq zoom: " + df.format(zoom) + "x");
			}
		});
		parameterPanel.add(freqZoomSlider);
		parameterPanel.add(freqZoomLabel);

		JSlider avgSlider = new JSlider(JSlider.HORIZONTAL, 100, 100000, 100);
		avg = Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.avg) ));
		JLabel avgLabel = new JLabel("avg: " + avg);
		avgSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				avg = avgSlider.getValue();
				fftData.setFbCount(avg);
				avgLabel.setText("avg: " + fftData.maxSampleCount);
				config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.avg), avg+"");
				config.store();
			}
		});
		 parameterPanel.add(avgSlider);
		 parameterPanel.add(avgLabel);

		pp = Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.pp)));
//		fftData.peakToPeakLimit = (double)pp/10d;
		
		JSlider peakSlider = new JSlider(JSlider.HORIZONTAL, 1, 5000, 200);
		JLabel peakLabel = new JLabel("p2p: "+pp+" uV");
		peakSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				fftData.peakToPeakLimit = (double)peakSlider.getValue()/10d;
				pp = peakSlider.getValue()/10;
				peakLabel.setText("p2p: " + peakSlider.getValue()/10 + " uV");
				config.setPref(Config.server_settings, String.valueOf(Config.server_settings_params.pp), pp+"");
				config.store();
			}
		});

		parameterPanel.add(peakSlider);
		parameterPanel.add(peakLabel);

		int tf = Integer.valueOf(config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.tf)));
		JSlider trainSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 10);
		JLabel trainLabel = new JLabel("training factor: "+tf+"%");
		trainSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				fftData.trainingFactor = (double) trainSlider.getValue() / 100d;
				trainLabel.setText("training factor: " + (int) (fftData.trainingFactor * 100d) + "%");
			}
		});

		parameterPanel.add(trainSlider);
		parameterPanel.add(trainLabel);

		// parameterPanel.add(comp)

		// buttonPanel.add(new JLabel(" OSC forward: "));
		// buttonPanel.add(textIP);
		// buttonPanel.add(textOSCPort);
		// buttonPanel.add(buttonForward);

		buttonMode.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (running) {
					String sendCmd = "I";
					if (buttonMode.getText().contains("I"))
						sendCmd = "S";
					boolean response = serialPortInterface.sendCommand(sendCmd);
					System.out.println("sent command: " + response);
					buttonMode.setText("mode=" + sendCmd);
				}
			}
		});

		buttonTrain.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// training = !training;
				fftData.setTrainingFactor(1d - fftData.getTrainingFactor());

				if (fftData.getTrainingFactor() > 0.5) {
					buttonTrain.setBackground(Color.CYAN);
					buttonTrain.setText("training");
					fftData.peakToPeakCheck = true;
				} else {
					buttonTrain.setBackground(Color.cyan.darker());
					buttonTrain.setText("train");
					fftData.peakToPeakCheck = false;
				}
				// trainLabel.setText("training factor:
				// "+(int)(fftData.trainingFactor*100d)+"%");
				trainSlider.setValue((int) (fftData.trainingFactor * 100d));

			}
		});

		buttonConnect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				textSerialPort.setText(textSerialPort.getText().trim());
				if (connect(textSerialPort.getText())) {
					buttonConnect.setEnabled(false);
					textSerialPort.setEnabled(false);
				} else {
					JOptionPane.showMessageDialog(frame, "Could not connect to '" + textSerialPort.getText() + "'.\nPlease check if the serial port at the specified address is available.", "connection error", JOptionPane.ERROR_MESSAGE);
					buttonConnect.setEnabled(true);
					textSerialPort.setEnabled(true);
				}
			}
		});


		buttonReset.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				lock.lock();
				fftData.reset();
				lock.unlock();
			}
		});

		buttonRecord.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				if (buttonRecord.getText().equals("record")) {
					JFileChooser chooser = new JFileChooser();
					int retrieval = chooser.showSaveDialog(null);
					if ((retrieval == JFileChooser.APPROVE_OPTION) && (chooser.getSelectedFile() != null)) {
						System.out.println("saving raw data stream to " + chooser.getSelectedFile().toString());

						serialPortInterface.record(chooser.getSelectedFile().toString());

						// fileOutputTask.setFileName(chooser.getSelectedFile().toString());
						// fileOutputTask.setWrite(true);
						buttonRecord.setText("stop");
						buttonRecord.setBackground(new Color(180, 60, 60));
					}
				} else {
					// fileOutputTask.setWrite(false);
					// fileOutputTask.stop();
					serialPortInterface.stopRecording();
					buttonRecord.setText("record");
					buttonRecord.setBackground(new Color(60, 255, 120));

				}

			}
		});

		if (this.proMode) {

			if (drawRawTimeSeries)
				frame.setSize(width-(drawBins?0:600), height);
			else
				frame.setSize(width - samplesPerSecond-(drawBins?0:600), height);

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			System.out.println("promode:" + this.proMode);
		}

		buttonFrame = new JFrame();
		buttonFrame.setLayout(new BorderLayout());
		buttonFrame.setSize(width, 100);
		buttonFrame.add(buttonPanel, BorderLayout.CENTER);
		if (proMode)
			buttonFrame.add(parameterPanel, BorderLayout.SOUTH);
		buttonFrame.setLocationRelativeTo(frame);
		buttonFrame.setVisible(true);
		buttonFrame.setResizable(false);
		buttonFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		buttonFrame.move(buttonFrame.getX(), buttonFrame.getY() + height / 2);
		
//		buttonFrame.addWindowFocusListener(new WindowFocusListener() {
//			@Override
//			public void windowLostFocus(WindowEvent e) {
//			}
//
//			@Override
//			public void windowGainedFocus(WindowEvent e) {
//				for (JFrame frame : frames) {
//					frame.toFront();
//					frame.repaint();
//				}
//			}
//		});

		// buttonFrame.setLocation(frame.getX()+frame.getWidth(), frame.getY());

		for (int i = 0; i < this.samplesPerSecond; i++) {
			double[] data = { .0, .0, .0, .0 };
			currentData.add(data);
		}
		frame.addWindowListener(new NFBWindowAdapter());
		buttonFrame.addWindowListener(new NFBWindowAdapter());

		// focusOMeter = new FocusOMeter();
		timer = new Timer();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		buttonPanel.repaint();

	}


	class NFBWindowAdapter extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			if (running) {
				System.out.println("Closing");
				int returnVal = serialPortInterface.shutDown();
				if (returnVal > 0)
					System.out.println("data stream shut down successful");
				System.out.println("Closed!");
			}
			e.getWindow().dispose();
		}
	}

	public static ReentrantLock getLock() {
		return lock;
	}

	protected boolean connect(String serialPortAddress) {

		Thread t = null;
		if (sham)
			t = new Thread(serialPortInterface = new PlaybackStream(this, numChannels, playbackFile, true));
		else
			t = new Thread(serialPortInterface = new SerialPortInterface(this, numChannels, textSerialPort.getText(), 230400, this.b24), "serialport");
		// t = new Thread(serialPortInterface = new SerialPortInterface(this,
		// numChannels, "/dev/rfcomm0", 230400), "serialport");

		buttonTrain.setEnabled(true);
		System.out.println("sham:" + sham);
		if (serialPortInterface.isConnectionSuccessful()) {
			t.start();
			running = true;
			buttonReset.setEnabled(true);
			buttonMode.setEnabled(true);
			// boolean sendCommand = serialPortInterface.sendCommand("I");
			//// sendCommand = serialPortInterface.sendCommand("S");
			// System.out.println("sent command: " + sendCommand);

		} else
			running = false;

		// for (Task task : tasks)
		// task.init();

		timer.schedule(new DrawTask(frame), 1500, 50);

		return running;

	}

	public boolean isRunning() {
		return running;
	}

	public int getNumSamples() {
		return numSamples;
	}

	public List<double[]> getInputData() {
		return inputData;
	}

	public long getCurrentTimestamp() {
		return currentTimestamp;
	}

	public synchronized void appendData(List<double[]> data) {
		lock.lock();
		try {
			currentTimestamp = System.nanoTime();
			inputData = data;
			numSamples = data.size();
			currentSamples += numSamples;

			for (int i = 0; i < numSamples; i++) {
				currentData.removeLast();
				currentData.addFirst(data.get(i));
			}
			newSamples += numSamples;
//			if ((currentTimestamp-lastTimestamp<60000000) || (newSamples < minimumNewSamples)) {
			if (newSamples < minimumNewSamples) {
				lock.unlock();
				return;
			} else
				newSamples = 0;

			for (Task task : tasks)
				task.run();
			
			lastTimestamp = currentTimestamp;

		} catch (Exception e) {
			e.printStackTrace();
			lock.unlock();
		}
		lock.unlock();

	}

	public DefaultFFTData getFftData() {
		return fftData;
	}

	class DrawTask extends TimerTask {

		private JFrame frame;

		DrawTask(JFrame frame) {
			this.frame = frame;
		}

		public void run() {
			if (currentFeedbackSettings != null) {
				currentFeedbackSettings.updateFeedback();
				if (oscForwarder != null) {
					OSCBundle bundle = new OSCBundle();
					for (int c = 0; c < numChannels; c++) {
						for (int b = 0; b < bins; b++) {
							OSCMessage msg = new OSCMessage(textSerialPort.getText() + "/" + binLabels[b] + "/" + c);
							Object argument = new Object();

							argument = new Double(currentFeedbackSettings.getRewardFFTBins()[b][c]);
							// argument=new Float(meanFFTBins[b][c]);
							msg.addArgument(argument);
							bundle.addPacket(msg);
							oscForwarder.forwardMessage(msg);
						}
					}
				}
			}
			this.frame.repaint();
			// oscForwarder.forwardBundle(bundle);
			// focusOMeter.setCurrentFeedback(currentFeedback);
		}
	}

	public ArrayList<FeedbackSettings> getAllSettings() {
		return feedbackSettings;
	}

	public void setConfig(Config config) {
		this.config = config;
		this.textSerialPort.setText(this.config.getPref(Config.server_settings, String.valueOf(Config.server_settings_params.serial_address)));
	}

	public Config getConfig() {
		return config;
	}

}
