package org.neurovillage.main.output.visual;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.SocketException;

import javax.swing.JFrame;

import org.neurovillage.main.output.audio.AudioFeedback;
import org.neurovillage.main.output.feedback.Feedback;
import org.neurovillage.model.FeedbackSettings;

import com.illposed.osc.OSCPortIn;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

public class NeuroGameVisuals extends Feedback {

	private GLCanvas glcanvas;
	private double currentFeedback = 0;
	private NeuroGameRenderer neurofeedbackRenderer;
	private static boolean debug = false;

	public static void main(String[] args) {

		debug = true;
		audioFeedback = new AudioFeedback(null);
		audioFeedback.displayGui(true);
		Thread thread = new Thread(audioFeedback);
		thread.run();

		NeuroGameVisuals n = new NeuroGameVisuals(null);

	}
	
	public JFrame getGameFrame() {
		return gameFrame;
	}

	public void setCurrentFeedback(float currentFeedback) {
		neurofeedbackRenderer.setCurrentFeedback(currentFeedback);
		glcanvas.repaint();
	}

	public NeuroGameVisuals(FeedbackSettings feedbackSettings) {
		super(feedbackSettings);
		if (feedbackSettings == null)
			currentFeedback = 0.01f;

		GLProfile glprofile = GLProfile.getDefault();

		neurofeedbackRenderer = new NeuroGameRenderer(glprofile);
		GLCapabilities glcapabilities = new GLCapabilities(glprofile);
		glcanvas = new GLCanvas(glcapabilities);

		FPSAnimator fpsAnimator = new FPSAnimator(glcanvas, 60, true);

		glcanvas.addGLEventListener(new GLEventListener() {

			@Override
			public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
				neurofeedbackRenderer.setup(glautodrawable.getGL().getGL2(), width, height);
			}

			@Override
			public void init(GLAutoDrawable glautodrawable) {
			}

			@Override
			public void dispose(GLAutoDrawable glautodrawable) {
			}

			@Override
			public void display(GLAutoDrawable glautodrawable) {
				neurofeedbackRenderer.render(glautodrawable.getGL().getGL2(), glautodrawable.getSurfaceWidth(),
						glautodrawable.getSurfaceHeight());
			}
		});

		gameFrame = new JFrame("Neurofeedback Game");
		gameFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent windowevent) {
				// jframe.dispose();
				// System.exit(0);
				// jframe.
			}
		});

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//		int width = gd.getDisplayMode().getWidth();
//		int height = gd.getDisplayMode().getHeight();
		int width = 1720;
		int height = 1080;

		gameFrame.getContentPane().add(glcanvas, BorderLayout.CENTER);
		gameFrame.setSize(width - (width / 5), height - (height / 5));
		gameFrame.setLocation((width / 10), (height / 10));
		gameFrame.setAlwaysOnTop(true);
		// jframe.setUndecorated(true);
//		gameFrame.setVisible(true);
		// jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		fpsAnimator.start();

		gameFrame.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
				fpsAnimator.stop();
			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowActivated(WindowEvent e) {

			}
		});
		if (debug) {
			glcanvas.addMouseListener(new MouseListener() {

				@Override
				public void mouseReleased(MouseEvent e) {

				}

				@Override
				public void mousePressed(MouseEvent e) {
					xStart = e.getX();
					yStart = e.getY();

				}

				@Override
				public void mouseExited(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseClicked(MouseEvent e) {
					// TODO Auto-generated method stub

				}
			});
			glcanvas.addMouseMotionListener(new MouseMotionListener() {

				@Override
				public void mouseMoved(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseDragged(MouseEvent e) {

					if ((yStart - e.getY() > 0) && (currentFeedback < .94f))
						currentFeedback += 0.05f;
					else if ((yStart - e.getY() < 0) && (currentFeedback > 0.06f))
						currentFeedback -= 0.05f;
					System.out.println("              cf:  " + currentFeedback);
					updateCurrentFeedback(currentFeedback);
					audioFeedback.updateCurrentFeedback(currentFeedback);

				}
			});
		}

	}

	private static int xStart = 0;
	private static int yStart = 0;
	private static AudioFeedback audioFeedback;
	private JFrame gameFrame;

	@Override
	public void updateCurrentFeedback(double currentFeedback) {
		super.updateCurrentFeedback(currentFeedback);
		setCurrentFeedback((float) currentFeedback);
	}
}