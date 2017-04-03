package org.neurovillage.gui;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.neurovillage.main.BCISettings;
import org.neurovillage.main.Launcher;
import org.neurovillage.main.MathBasics;
import org.neurovillage.main.NeuroUtils;
import org.neurovillage.model.Config;
import org.neurovillage.model.DefaultFFTData;
import org.neurovillage.model.FFTPreprocessor;
import org.neurovillage.model.NFBProcessor;
import org.neurovillage.model.RelaxFeedbackSettings;
import org.neurovillage.tools.ColorMap;
import org.neurovillage.tools.Utils;

public class LongtermAnalyzer extends JFrame
{

	// default analysis settings
	private int windowSize = 500;
	private int maxDisplayFreq = 60;
	private int stepX = 64;
	private double stepY = 4d;
	private double p2pLimit = 5d;
	private double rewardMax = Double.MIN_VALUE;
	private double rewardMin = Double.MAX_VALUE;
	
	static File fileToOpen = new File("C:/temp/eegdata/chill");
	
	
	private int numChannels = 2;
	private int bins = 4;
	DecimalFormat df = new DecimalFormat("#.###");

	public int frameWidth = 1280;
	public int frameHeight = 700;
	public static int border = 20;

	private BufferedImage trackImage;
	private Graphics2D trackGraphics;
	private int selectionIn = -1;
	private int selectionOut = -1;
	private int hoverPosX = -1;
	private int hoverPosY = -1;
	private int cursorPos = 0;
	private double zoomLevel = 1d;

	private double freqs[][][];
	private double samples[][];
	private int penalties[][];
	private long timestamps[];
	private int rewardCount = 0;
	private long startTimestamp = 0l;
	private int maxPenalty = Integer.MIN_VALUE;

	private JPanel trackPanel;
	private Graphics trackPanelGraphics;

	private FFTPreprocessor fftPreprocessor;
	private ReentrantLock lock;
	private DefaultFFTData fftData;
	private NFBProcessor nfbProcessor;
	private BCISettings bciSettings;
	private int numberOfLines = 0;

	private double maxFFT = 250;
	private double minFFT = 0;
	private double range = Math.max(minFFT, maxFFT) - Math.min(minFFT, maxFFT);
	private int trackWidth;
	private int trackHeight;
	private int binsPerPixel;

	private int heightPerChannel;
	private int startPos;
	private double totalPowerMax = Double.MIN_VALUE;
	private double totalPowerMin = Double.MAX_VALUE;
	private double totalPower[][];
	private double totalPowerDiff = 0d;
	private double[][] fftValues;
	private double[] fftValuesMax;
	protected int minimumNewSamples = 8;
	private double[] reward;

	private double rewardRange;
	private int displaySampleOffset = 0;
	private double sampleTimePerSecond = 33;
	private int barStep = 100;
	private int numBars = 14;

	private RelaxFeedbackSettings rewardSettings;
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem menuOpenItem;

	private int currentCounter = 0;

	private boolean fileOpened = false;
	
	public double[][] powerZScore;
	public double powerZMin = Double.MIN_VALUE;
	public double powerZMax = Double.MAX_VALUE;

	public static void main(String[] args)
	{
		Config config = new Config(Launcher.defaultConfig);
		LongtermAnalyzer lt = new LongtermAnalyzer(config);
		lt.openFile(fileToOpen);
//		 lt.openFile(new File("/home/horus/temp/w1_before.csv"));
		// lt.openFile(new
		// File("C:/temp/workspace2015/neurophlex/fileoutput1460657434051.csv"));
		// lt.openFile(new File("/root/offset"));
		// lt.openFile(new File("/root/e3p"));
		// lt.openFile(new File("/root/neww"));
		// lt.openFile(new File("/root/c"));

		// CMB
		// lt.openFile(new File("/root/Carlos1.csv"));
		// lt.openFile(new File("/root/c2"));
		// lt.openFile(new File("/root/cmb/iban1"));
		// lt.openFile(new File("/root/cmb/NATALIA"));
		// lt.openFile(new File("/root/cmb/Alex1"));

	}

	private long lastSecondTimestamp = 0;
	private int minSamplesPerSecond = Integer.MAX_VALUE;
	private int maxSamplesPerSecond = Integer.MIN_VALUE;
	
	private PrintWriter out;
	private File file;
	

	private boolean openFile(File file)
	{
		resetWindow();
		if (file.exists())
		{
			this.file = file;
			numberOfLines = Utils.countLines(file);
			
			System.out.println("number of lines: \t" + numberOfLines);
			System.out.println("samples per second: \t" + windowSize);
			System.out.println("resulting record time: \t" + (numberOfLines / windowSize) + " seconds");

			if (numberOfLines > 0)
			{
				samples = new double[numberOfLines][numChannels];
				timestamps = new long[numberOfLines];
				freqs = new double[numberOfLines][numChannels][windowSize / 2];
				totalPower = new double[numberOfLines][numChannels];
				fftValues = new double[numberOfLines][numChannels];
				penalties = new int[numberOfLines][numChannels];
				
				powerZScore = new double[numberOfLines][numChannels];
				
				fftValuesMax = new double[numChannels];
				reward = new double[numberOfLines];
//				rewardMax = Double.MIN_VALUE;
//				rewardMax = Double.MAX_VALUE;
				rewardRange = 0;
				rewardCount = 0;
				sampleTimePerSecond = 1000d / (double) windowSize;

				bciSettings = new BCISettings(windowSize, numChannels, bins);
				fftData = new DefaultFFTData();
				fftPreprocessor = new FFTPreprocessor(fftData, bciSettings);
				binsPerPixel = (int) Math.ceil((double) maxDisplayFreq * numChannels / (double) trackHeight);
				// heightPerChannel = trackHeight / numChannels;
				heightPerChannel = maxDisplayFreq * (int) (stepY);
				startPos = windowSize;
				ColorMap.getJet(256);
				fftPreprocessor.getFFTData().peakToPeakLimit = this.p2pLimit;
				fftPreprocessor.enableFIRFilter(false);

				rewardSettings = new RelaxFeedbackSettings(fftData, lock, config);
				fileOpened = true;

			} else
			{
				fileOpened = false;
				return false;
			}
			try
			{
				Scanner scanner = new Scanner(file);

				double currentTime = 0l;
				double startTime = 0;
				double currentSamples[] = new double[numChannels];
				ConcurrentLinkedDeque<double[]> currentData = bciSettings.getCurrentData();

				int s = 0;
				while (scanner.hasNextLine())
				{
					String currentLine = scanner.nextLine();
					currentCounter++;
					if ((s > 1) && ((timestamps[s - 1] - lastSecondTimestamp) / 1000000 >= 1000))
					{
						// System.out.println(currentCounter + " samples");
						if (currentCounter > 100)
						{
							minSamplesPerSecond = Math.min(minSamplesPerSecond, currentCounter);
							maxSamplesPerSecond = Math.max(maxSamplesPerSecond, currentCounter);
						}
						currentCounter = 0;
						lastSecondTimestamp = timestamps[s - 1];
					}

					if (currentLine.contains("i") || currentLine.contains("s"))
					{
						currentData.addFirst(new double[] { 0, 0 });
						currentData.removeLast();
						timestamps[s] = timestamps[s - 1] + 1;
						// System.out.println("s cmd:" + s);
						s++;
						continue;
					}

					// split by old delimiter "comma"
					String[] l = currentLine.split(",");
					if (l.length < 3)
					{ // check if delimiter is "tabulator"
						l = currentLine.split(" |\t");
						System.out.println(s + ":" + l[1] + l[2] + "=" + NeuroUtils.parseUnsignedHex(l[1] + l[2]));
						if (l.length == 5)
							currentData.addFirst(new double[] { -5000d + 10000d  * NeuroUtils.parseUnsignedHex(l[1] + l[2]) / 16777216d, -5000d + 10000d  * NeuroUtils.parseUnsignedHex(l[3] + l[4]) / 16777216d });
						else // something else, or broken -> skip
							continue;
					} else
						currentData.addFirst(new double[] { Double.valueOf(l[1]), Double.valueOf(l[2]) });

					if (s == 0)
						startTime = nanoToSeconds(l[0]);
					currentTime = nanoToSeconds(l[0]) - startTime;
					timestamps[s] = Long.valueOf(l[0]);

					samples[s] = currentData.getFirst().clone();

					if (s > windowSize - 2)
					{
						if (s % minimumNewSamples == 0)
							fftPreprocessor.run();

						double a;
						if ((s > numberOfLines / 8) && (fftPreprocessor.getFFTData().getTrainingFactor() < 0.45d))
							fftPreprocessor.getFFTData().setTrainingFactor(.5d);

						for (int c = 0; c < numChannels; c++)
						{
							penalties[s][c] = new Integer(fftPreprocessor.getFFTData().packagePenalty[c]);
							maxPenalty = Math.max(penalties[s][c], maxPenalty);

							for (int f = 1; f < maxDisplayFreq; f++)
							{
								// freqs[s][c][f] = freqs[s - 1][c][f] * .999d +
								// fftPreprocessor.getFFTData().currentFFTs[c][f]
								// * .001d;
								freqs[s][c][f] = fftPreprocessor.getFFTData().currentFFTs[c][f];
								totalPower[s][c] += fftPreprocessor.getFFTData().currentFFTs[c][f];
								
								powerZScore[s][c] = MathBasics.getZScore(fftPreprocessor.getFFTData().currentFFTValue[c], fftPreprocessor.getFFTData().meanFFTValue[c], Math.sqrt(fftPreprocessor.getFFTData().varFFTValue[c]));
								
								if ((s>3000) && (s<numberOfLines-3000) &&(!Double.isNaN(powerZScore[s][c])))
								{
									powerZMin = Math.min(powerZScore[s][c], powerZMin);
									powerZMax = Math.max(powerZScore[s][c], powerZMax);
								}
								
								totalPowerMin = Math.min(totalPowerMin, totalPower[s][c]);
								totalPowerMax = Math.min(totalPowerMax, totalPower[s][c]);
							}
							fftValues[s][c] = fftPreprocessor.getFFTData().currentFFTValue[c];
						}
						if (s % minimumNewSamples == 0)
						{
							rewardSettings.updateFeedback();
//							reward[s] = MathBasics.clamp(rewardSettings.getCurrentFeedback(),-rewardMin,rewardMax);
							reward[s] = rewardSettings.getCurrentFeedback();
							if ((s>3000) && (s<numberOfLines-3000) &&(!Double.isNaN(reward[s])))
							{
								rewardMax = Math.max(rewardMax, reward[s]);
								rewardMin = Math.min(rewardMin, reward[s]);
							}
						} else
							reward[s] = reward[s - 1];
						if (reward[s] > 0d)
							rewardCount++;

						currentData.removeLast();
					}
					s++;

				}
				System.out.println("min samples per second:\t" + minSamplesPerSecond);
				System.out.println("max samples per second:\t" + maxSamplesPerSecond);
//				rewardMin = -6d;
//				rewardMax = 6d;
				rewardRange = rewardMax - rewardMin;

				System.out.println("reward min:\t" + rewardMin);
				System.out.println("reward max:\t" + rewardMax);
				System.out.println("reward range:\t" + rewardRange);
				System.out.println("reward amount over recording:" + ((double) rewardCount / (double) numberOfLines) + "%");
				System.out.println("max penalty:" + maxPenalty);
				
				System.out.println("z score power min: " + powerZMin);
				System.out.println("z score power max: " + powerZMax);

				for (int c = 0; c < numChannels; c++)
					fftValuesMax[c] = fftPreprocessor.getFFTData().meanFFTValue[c];

				totalPowerDiff = totalPowerMax - totalPowerMin;
				updateCurrentTrack();

			} catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			// System.exit(0);
		}
		return false;
	}

	private double nanoToSeconds(Object nanoSeconds)
	{
		if (nanoSeconds instanceof String)
			return Double.valueOf((String) nanoSeconds) / 1000000000.0d;
		return 0d;
	}

	private void resetWindow()
	{
		selectionIn = -1;
		selectionOut = -1;
		hoverPosX = -1;
		hoverPosY = -1;
		cursorPos = 0;
		zoomLevel = 1d;
	}

	final JFileChooser fc = new JFileChooser();
	private int analysisFrameWidth = 300;
	protected boolean moving = false;
	protected int startMovingPos;
	private int softening = 1;
	protected int oldSelectionIn;
	protected int oldSelectionOut;
	protected boolean selection = false;
	private JPanel toolsPanel;
	private DefaultCategoryDataset dataset;
	private JMenuItem menuSaveItem;
	private Config config;
	
	public LongtermAnalyzer(Config config)
	{
		this.config = config;

		initMenu();

		GraphicsDevice[] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		GraphicsDevice gd = null;
		int maxRes = Integer.MIN_VALUE;
		int maxResIdx = 0;
		int maxOffsetX = 0;
		int maxOffsetY = 0;
		// Rectangle maxResRect = null;
		// gd.
		if (gds.length > 1)
		{
			for (int i = 0; i < gds.length; i++)
			{
				int curRes = gds[i].getDisplayMode().getWidth() * gds[i].getDisplayMode().getHeight();
				System.out.println(gds[i].getDefaultConfiguration().getBounds());
				// System.out.println(gds[i].getFullScreenWindow().getX() + ' '
				// + gds[i].getFullScreenWindow().getY());
				// System.out.println(gds[i].getDisplayMode().);
				if (curRes > maxRes)
				{
					maxRes = curRes;
					maxResIdx = i;
					maxOffsetX = gds[i].getDefaultConfiguration().getBounds().x;
					maxOffsetY = gds[i].getDefaultConfiguration().getBounds().y;
				}
			}
			gd = gds[maxResIdx];
			// GraphicsEnvironment.getLocalGraphicsEnvironment().
		} else
			gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		// else

		// System.out.println("max:" +
		// GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint());
		// Robot robot;
		// try {
		// robot = new Robot();
		// robot.mouseMove(GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint().x,GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint().y);
		// System.exit(0);
		// } catch (AWTException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }

		int screenWidth = gd.getDisplayMode().getWidth();
		int screenHeight = gd.getDisplayMode().getHeight();
		// gd.getDisplayMode().
		frameWidth = screenWidth - 100;
		frameHeight = screenHeight - 80;
		trackWidth = frameWidth - border * 2;
		trackHeight = frameHeight;
		lock = new ReentrantLock();

		System.out.println("bins per pixel: " + binsPerPixel);
		// System.out.println("height: " + trackHeight);

		// nfbProcessor = new NFBProcessor(null);

		this.setSize(new Dimension(frameWidth, frameHeight));
		// this.setLocationRelativeTo(null);
		this.setTitle("Neurofox Longterm Analysis");
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.trackImage = new BufferedImage(trackWidth, trackHeight, BufferedImage.TYPE_INT_ARGB);
		this.trackGraphics = (Graphics2D) this.trackImage.getGraphics();
		this.trackGraphics.setColor(Color.BLACK);
		// this.trackGraphics.fillRect(0, 0, frameWidth-border*2,
		// frameHeight/3);

		JSlider softeningSlider = new JSlider(JSlider.HORIZONTAL, 1, 1000, 1);
		JLabel softeningLabel = new JLabel("softening: " + softening + " samples");
		softeningSlider.setPreferredSize(new Dimension(200, 30));

		softeningSlider.addChangeListener(new ChangeListener()
		{

			@Override
			public void stateChanged(ChangeEvent e)
			{
				softening = softeningSlider.getValue();
				updateCurrentTrack();
				repaint();
			}
		});

		this.trackPanel = new JPanel()
		{
			@Override
			public void paint(Graphics g)
			{
				Graphics2D g2 = (Graphics2D) g;
				g2.drawImage(trackImage, border, border, null);

				if (selection)
				{
					g2.setStroke(new BasicStroke(3));
					// g2.setColor(new Color(0, 0, 120, 100));
					// g2.fillRect(border, border, selectionIn - border,
					// trackImage.getHeight());
					// g2.setColor(new Color(0, 120, 0, 100));
					// g2.fillRect(selectionOut, border, trackImage.getWidth() -
					// selectionOut + border, trackImage.getHeight());

					g2.setColor(Color.YELLOW);
					for (int c = 0; c < numChannels; c++)
						g2.drawRect(Math.min(selectionIn, selectionOut), border + heightPerChannel * c - 2, Math.abs(selectionIn - selectionOut), heightPerChannel - border);
					g2.setColor(new Color(255, 255, 0, 60));
					for (int c = 0; c < numChannels; c++)
						g2.fillRect(Math.min(selectionIn, selectionOut), border + heightPerChannel * c + 2, Math.abs(selectionIn - selectionOut), heightPerChannel - border);
					// g2.drawLine(selectionIn, border, selectionIn, border +
					// trackImage.getHeight());

					// g2.setColor(Color.YELLOW);
					// g2.drawLine(selectionOut, border, selectionOut, border +
					// trackImage.getHeight());
					// g2.setColor(new Color(0, 0, 120, 100));
					// g2.fillRect(border, border, selectionIn - border,
					// trackImage.getHeight());
					// g2.setColor(new Color(0, 120, 0, 100));
					// g2.fillRect(selectionOut, border, trackImage.getWidth() -
					// selectionOut + border, trackImage.getHeight());
					//
					// g2.setColor(Color.YELLOW);
					// g2.drawLine(selectionIn, border, selectionIn, border +
					// trackImage.getHeight());
					//
					// g2.setColor(Color.YELLOW);
					// g2.drawLine(selectionOut, border, selectionOut, border +
					// trackImage.getHeight());
					g2.setStroke(new BasicStroke(1));
				}
				
//				if (feedb)

				// draw cursor
				g2.setColor(Color.CYAN);
				g2.drawLine(hoverPosX, border, hoverPosX, border + trackImage.getHeight());
				g2.drawLine(0, hoverPosY, trackImage.getWidth(), hoverPosY);
				
				if (rewardSettings!=null)
				{
					for (int c = 0; c < numChannels; c++)
					{
						for (int b = 0; b < rewardSettings.getBinRanges().length; b+=2)
						{
//							System.out.println(rewardSettings.getBinRanges()[b]);
//							System.out.println(rewardSettings.getBinRanges()[b+1] + " -");
							int offsetY1 = (int)(rewardSettings.getBinRanges()[b]*stepY) + border + heightPerChannel*c;
							int offsetY2 = (int)((rewardSettings.getBinRanges()[b+1]+1)*stepY) + border + heightPerChannel*c;
							
							if ((hoverPosY >= offsetY1) && (hoverPosY <= offsetY2))
							{
								g2.setColor(new Color(255,127,0,120));
								g2.fillRect(border, offsetY1, trackImage.getWidth()+border, offsetY2 - offsetY1);
							}
							else
							{
								g2.setColor(new Color(255,127,0,120));
								g2.drawLine(border, offsetY1, trackImage.getWidth()-border, offsetY1);
								g2.drawLine(border, offsetY2, trackImage.getWidth()-border, offsetY2);
								
								g2.setColor(new Color(255,127,0,30));
								g2.fillRect(border, offsetY1, trackImage.getWidth()+border, offsetY2 - offsetY1);
								
							}
						}
					}
				}
//				for ()

				if (fileOpened)
				{
					int hz = (((int) ((binsPerPixel * hoverPosY - border) / stepY)) % (heightPerChannel / 4));
					// System.out.println("y:\t" + hoverPosY + "\tstepY:" +
					// stepY +"\tbpp:" + binsPerPixel);
					String time = "";
					// int ms =
					// (int)(sampleTimePerSecond*(displaySampleOffset+hoverPosX-border)*stepX);
					int sampleNumber = (int) ((displaySampleOffset + hoverPosX - border) * stepX);
					if (sampleNumber < 0)
						sampleNumber = 0;
					else if (sampleNumber > numberOfLines - 1)
						sampleNumber = numberOfLines - 1;

					if ((sampleNumber > 0) && (sampleNumber < numberOfLines))
					{
						int seconds = (int) ((timestamps[sampleNumber] - timestamps[0]) / 1000000000.0);
						// int seconds = 0;
						int minutes = 0;
						// if (ms>0)
						{
							// seconds = ms / 1000;
							minutes = seconds / 60;
						}
						time = minutes + ":" + (seconds % 60);
					}
					// String time = "";
					// if (hoverPosX*stepX<numberOfLines)
					// time = timestamps[hoverPosX*stepX];

					g2.setColor(Color.BLACK);
					drawString(g, hz + " Hz\n" + time + "\n" + sampleNumber + ":" + timestamps[sampleNumber], hoverPosX + 15, hoverPosY + 15);
					g2.setColor(Color.WHITE);
					drawString(g, hz + " Hz\n" + time + "\n" + sampleNumber + ":" + timestamps[sampleNumber], hoverPosX + 15, hoverPosY + 15);
				}

			}
		};
		this.trackPanelGraphics = this.trackPanel.getGraphics();

		this.toolsPanel = new JPanel();
		this.toolsPanel.setBackground(Color.darkGray);
		// this.toolsPanel.setLayout(new BoxLayout(this.toolsPanel,
		// BoxLayout.X_AXIS));
		this.toolsPanel.setLayout(new GridLayout(1, 3));

		this.trackPanel.setSize(frameWidth, frameHeight);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.trackPanel, toolsPanel);
		splitPane.setDividerLocation(border + trackHeight / 2);
		this.add(splitPane);
		// this.add(this.trackPanel);
		this.setJMenuBar(menuBar);

		this.trackPanel.addMouseListener(new MouseListener()
		{

			@Override
			public void mouseReleased(MouseEvent e)
			{
				moving = false;
				selectionOut = e.getX();
				if (selectionIn==selectionOut)
					selection = false;
				else
					updateDataset();
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if ((e.getButton() == 2) || (e.isShiftDown()))
				{
					if (!moving)
					{
						moving = true;
						startMovingPos = displaySampleOffset + e.getX();
						oldSelectionIn = selectionIn;
						oldSelectionOut = selectionOut;
						// System.out.println("starting to move");
					}
					// else
					// {
					// displaySampleOffset=displaySampleOffset+(e.getX()-startMovingPos);
					// updateCurrentTrack();
					// System.out.println("moving " + displaySampleOffset);
					// }
				} else
				{
					if (e.getButton() != 2)
					{
						selectionIn = e.getX();
						selection = true;
					}
					hoverPosX = e.getX();
					hoverPosY = e.getY();
				}
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
			}
		});

		this.trackPanel.addMouseWheelListener(new MouseWheelListener()
		{

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if ((e.getWheelRotation() > 0) && (stepX > 1))
				{
					stepX /= 2;
					if (selection)
					{
						selectionIn *= 2;
						selectionOut *= 2;
					}
				} else
				{
					stepX *= 2;
					if (selection)
					{
						selectionIn /= 2;
						selectionOut /= 2;
					}
				}
				updateCurrentTrack();

			}
		});

		this.trackPanel.addMouseMotionListener(new MouseMotionListener()
		{

			@Override
			public void mouseMoved(MouseEvent e)
			{
				hoverPosX = e.getX();
				hoverPosY = e.getY();
				repaint();

			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (moving)
				{
					displaySampleOffset = startMovingPos - e.getX();
					if (displaySampleOffset < 0)
						displaySampleOffset = 0;
					else
					{
						selectionIn = oldSelectionIn - displaySampleOffset;
						selectionOut = oldSelectionOut - displaySampleOffset;
						updateCurrentTrack();
					}
				} else
					// if (e.getX()<selectionIn)
					// {
					//// selectionOut = selectionIn;
					// selectionIn = e.getX();
					// }
					// else
					// selectionIn = startMovingPos - e.getX();
					selectionOut = e.getX();
				repaint();
			}
		});
		
		JPanel feedbackSettingsPanel = new JPanel();
		feedbackSettingsPanel.setBorder(BorderFactory.createTitledBorder("feedback: " ));

		JPanel displaySettingsPanel = new JPanel();
		displaySettingsPanel.setSize(80, frameHeight);
		displaySettingsPanel.setBorder(BorderFactory.createTitledBorder("display settings"));
		displaySettingsPanel.setLayout(new BorderLayout());
		JSlider sliderMin = new JSlider(JSlider.VERTICAL, 1, 128000, (int) minFFT + 1);
		sliderMin.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				double val = (double) sliderMin.getValue();
				if (val != minFFT)
				{
					updateFFTMinMax(val, maxFFT);
					updateCurrentTrack();
				}
			}
		});
		JSlider sliderMax = new JSlider(JSlider.VERTICAL, 1, 1280, (int) maxFFT + 1);
		sliderMax.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				double val = (double) sliderMax.getValue();
				if (val != maxFFT)
				{
					updateFFTMinMax(minFFT, val);
					updateCurrentTrack();
				}
			}
		});
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.X_AXIS));
		sliderPanel.add(sliderMin);
		sliderPanel.add(sliderMax);
		sliderPanel.setPreferredSize(new Dimension(60, 600));
		sliderMin.setPreferredSize(new Dimension(30, 600));
		sliderMax.setPreferredSize(new Dimension(30, 600));

		displaySettingsPanel.add(sliderPanel, BorderLayout.CENTER);
		JPanel softeningPanel = new JPanel();
		softeningPanel.setLayout(new BoxLayout(softeningPanel, BoxLayout.Y_AXIS));
		softeningPanel.add(softeningSlider);
		softeningPanel.add(softeningLabel);

		displaySettingsPanel.add(softeningPanel, BorderLayout.SOUTH);

		// colorSettingsFrame.setLocationRelativeTo(this);
		// displaySettingsPanel.revalidate();
		// this.move(maxOffsetX, maxOffsetY + 50);
		// displaySettingsPanel.move(maxOffsetX + frameWidth + 2, maxOffsetY +
		// 50);

		JPanel analysisPanel = new JPanel();
		analysisPanel.setBackground(new Color(50, 100, 100));
		analysisPanel.setLayout(new BorderLayout());

		JFreeChart lineChart = ChartFactory.createLineChart("power spectral density", "Frequency (Hz)", "Power (uV^2/Hz)", updateDataset(), PlotOrientation.VERTICAL, true, true, false);
		lineChart.getTitle().setFont(new Font("Ubuntu", Font.PLAIN, 20));
		lineChart.getPlot().setOutlineStroke(new BasicStroke(3));
		lineChart.getCategoryPlot().getRenderer().setSeriesStroke(0, new BasicStroke(3));
		lineChart.getCategoryPlot().getRenderer().setSeriesStroke(1, new BasicStroke(3));
//		lineChart.getPlot().get
//		lineChart.getPlot().set
		
		// LineCha chart = new LineChart_AWT("School Vs Years" , "Numer of
		// Schools vs years");
		// Plot plot = new Plo
		// JFreeChart chart = new JFreeChart(plot);
		ChartPanel chartPanel = new ChartPanel(lineChart);
		analysisPanel.add(chartPanel, BorderLayout.CENTER);

		this.toolsPanel.add(displaySettingsPanel);
		this.toolsPanel.add(analysisPanel);
		// analysisFrame.setSize(analysisFrameWidth, frameHeight);
		// analysisFrame.setVisible(true);
		// analysisFrame.move(displaySettingsPanel.getWidth() + maxOffsetX +
		// frameWidth + 2, maxOffsetY + 50);

		this.revalidate();
	}

	private DefaultCategoryDataset updateDataset()
	{
		if (dataset==null)
			dataset = new DefaultCategoryDataset();
		else
			dataset.clear();
		if ((selection) && (selectionIn > 0) && (Math.abs(selectionIn - selectionOut) > 0))
		{
			double meanFreqs[][] = new double[numChannels][maxDisplayFreq];
			int i = Math.max(0,(Math.min(selectionIn, selectionOut) + displaySampleOffset) * stepX);
			int o = Math.min((Math.max(selectionIn, selectionOut) + displaySampleOffset) * stepX,numberOfLines);
			int n = o-i;
			int p = 0;
//			System.out.println("in=" + i);
//			System.out.println("out=" + o);
			
			for (int f = 4; f < 40; f++)
			{
				for (int c = 0; c < numChannels; c++)
				{
//					penalties[c][c]
					
					for (int s = i; s < o; s++)
					{
//						if (penalties[s][c]>0)
//							p++;
//						else
							meanFreqs[c][f] += freqs[s][c][f];
					}
					meanFreqs[c][f] /= (double) Math.max(1,(n-p));
					dataset.addValue(meanFreqs[c][f], "channel " + (c+1), f + "");
				}
				
			}
		}
		return dataset;
	}

	private void initMenu()
	{
		menuBar = new JMenuBar();

		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().setAccessibleDescription("File menu, for opening and saving files.");

		menuOpenItem = new JMenuItem("Open...", KeyEvent.VK_O);
		menuOpenItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
		menuOpenItem.getAccessibleContext().setAccessibleDescription("Open EEG files");
		menuOpenItem.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				int returnVal = fc.showOpenDialog(LongtermAnalyzer.this);

				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					File file = fc.getSelectedFile();
					// This is where a real application would open the file.
					LongtermAnalyzer.this.openFile(file);
					menuSaveItem.setEnabled(true);
				} else
				{
				}

			}
		});
		fileMenu.add(menuOpenItem);
		
		menuSaveItem = new JMenuItem("Save as CSV...", KeyEvent.VK_O);
		menuSaveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
		menuSaveItem.getAccessibleContext().setAccessibleDescription("Save CSV file...");
		menuSaveItem.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				int returnVal = fc.showSaveDialog(LongtermAnalyzer.this);
				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					File file = fc.getSelectedFile();
					// This is where a real application would open the file.
					LongtermAnalyzer.this.saveCSVFile(file);
				} else
				{
				}

			}
		});
		menuSaveItem.setEnabled(false);
		fileMenu.add(menuSaveItem);

		menuBar.add(fileMenu);

	}

	protected void saveCSVFile(File file)
	{
		
		try {
			out = new PrintWriter(file, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		try
		{
			Scanner scanner = new Scanner(this.file);
			String outputLine = "";

			int s = 0;
			out.println("timestamp,ch1,ch2");

			while (scanner.hasNextLine())
			{
				String currentLine = scanner.nextLine();
				outputLine = "";

				if (currentLine.contains("i") || currentLine.contains("s"))
				{
//					currentData.addFirst(new double[] { 0, 0 });
//					currentData.removeLast();
//					timestamps[s] = timestamps[s - 1] + 1;
//					// System.out.println("s cmd:" + s);
					s++;
					continue;
				}

				// split by old delimiter "comma"
				String[] l = currentLine.split(",");
				if (l.length < 3)
				{ // check if delimiter is "tabulator"
					l = currentLine.split(" |\t");
					if (l.length == 5)
						outputLine = (-5000d + 10000d * NeuroUtils.parseUnsignedHex(l[1] + l[2]) / 16777216d) + "," +  (-5000d + 10000d  * NeuroUtils.parseUnsignedHex(l[3] + l[4]) / 16777216d) ;
					else // something else, or broken -> skip
						continue;
				} else
					outputLine = Double.valueOf(l[1]) + "," +  Double.valueOf(l[2]) ;

				outputLine = Long.valueOf(l[0]) + "," + outputLine;
				out.println(outputLine);

				s++;
			}
			out.close();
			System.out.println("saved CSV file to " + file);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
//		File
	}

	public void updateFFTMinMax(double min, double max)
	{
		minFFT = (double) min / 1000d;
		maxFFT = (double) max ;
		range = Math.max(minFFT, maxFFT) - Math.min(minFFT, maxFFT);
	}

	void drawString(Graphics g, String text, int x, int y)
	{
		for (String line : text.split("\n"))
			g.drawString(line, x, y += g.getFontMetrics().getHeight());
	}

	public void updateCurrentTrack()
	{
		// for (int c=0; c < numChannels ; c++)
		// channelGraphics[c].copyArea(0, 0, windowWidth -
		// sideBorder-numSamples, displayHeight, numSamples, 0);
		trackGraphics.setColor(Color.BLACK);
		trackGraphics.fillRect(0, 0, trackImage.getWidth(), trackImage.getHeight());

		double[][] currentFreqs = new double[numChannels][maxDisplayFreq];
		for (int s = 0; s < trackImage.getWidth(); s++)
		{
			int i = (s + displaySampleOffset) * stepX;
			if (i > freqs.length)
				break;
			for (int c = 0; c < numChannels; c++)
			{
				for (int f = 0; f < maxDisplayFreq; f++)
				{
					// int i = s*stepX;

					currentFreqs[c][f] = currentFreqs[c][f] * ((double) softening / 1000d) + freqs[i][c][f] * ((double) (1000 - softening) / 1000d);
					// currentFreqs[c][f] = freqs[i][c][f] ;

					// if ((f > 0) && ((f + 1) % binsPerPixel == 0))
					{

						// currentFreqs[c][f] /= (double) binsPerPixel;
						int colorVal = (int) (((currentFreqs[c][f] - minFFT) / range) * 255d);

						if (colorVal > 255)
							colorVal = 255;
						else if (colorVal < 0)
							colorVal = 0;

						trackGraphics.setColor(new Color(ColorMap.getColor(colorVal)));
						trackGraphics.fillRect(s, (int) ((f) * stepY) + heightPerChannel * c, 1, (int) stepY);
						// System.out.println(f);

					}

					// if ((f>0)&&((f+1)%binsPerPixel==0))
					// {
					// currentFreqs[c][f]/=(double)binsPerPixel;
					// int colorVal =
					// (int)(((currentFreqs[c][f]-minFFT)/range)*255d);
					// if (colorVal>255)
					// colorVal = 255;
					// else if (colorVal<0)
					// colorVal = 0;
					// trackGraphics.setColor(new
					// Color(ColorMap.getColor(colorVal)));
					// trackGraphics.fillRect(s,
					// (f/binsPerPixel)+heightPerChannel*c, 1, 1);
					// currentFreqs[c][f]=0d;
					// }
				}
				int colorVal = (int) (((3d+ MathBasics.clamp(powerZScore[i][c],-3d,6d)) / 9d) * 255d);
//				System.out.println("co:"+colorVal);
				trackGraphics.setColor(new Color(colorVal,0,255-colorVal));
				trackGraphics.fillRect(s, -23 + maxDisplayFreq * (int) stepY + heightPerChannel * c, 1, 7);

				
				
				colorVal = (int) (((reward[i] - rewardMin) / rewardRange) * 255d);
				if (colorVal > 255)
				colorVal = 255;
					else if (colorVal < 0)
				colorVal = 0;
//				trackGraphics.setColor(new Color(ColorMap.getColor(colorVal)));
				if (colorVal>127)
					trackGraphics.setColor(new Color(0, (colorVal-128)*2,166));
				else
					trackGraphics.setColor(new Color((127-colorVal)*2, 0, 0));
				trackGraphics.fillRect(s, -30-15 + maxDisplayFreq * (int) stepY + heightPerChannel * c, 1, 25);

				
				
				
				
				colorVal = (int) ((1d + (double) penalties[i][c]) / (1d + (double) maxPenalty) * 255d);
				if (colorVal > 255)
					colorVal = 255;
				else if (colorVal < 0)
					colorVal = 0;
				trackGraphics.setColor(new Color(ColorMap.getColor(colorVal)));
				trackGraphics.fillRect(s, -8 + maxDisplayFreq * (int) stepY + heightPerChannel * c, 1, 7);

			}

		}
		trackPanel.repaint();

	}

}