package org.neurovillage.main.output.visual;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.SocketException;

import javax.swing.JFrame;

import org.neurovillage.main.output.feedback.Feedback;
import org.neurovillage.model.FeedbackSettings;

import com.illposed.osc.OSCPortIn;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

/**
 * 
 *
 * @author fractalfox
 */
public class FocusOMeter extends Feedback
{

	private static OSCPortIn receiver;
	private GLCanvas glcanvas;

	public void setCurrentFeedback(float currentFeedback)
	{
		FocusOMeterGLRenderer.setCurrentFeedback(currentFeedback);
		glcanvas.repaint();
	}

	double currentFeedback = 0;
	
	public void setOSCInput(int port)
	{
		try
		{
			receiver = new OSCPortIn(port);
			
		} catch (SocketException e)
		{
			e.printStackTrace();
		}
	}

	public FocusOMeter(FeedbackSettings feedbackSettings)
	{
		super(feedbackSettings);

		GLProfile glprofile = GLProfile.getDefault();
		GLCapabilities glcapabilities = new GLCapabilities(glprofile);
		glcanvas = new GLCanvas(glcapabilities);
		glcanvas.addGLEventListener(new GLEventListener()
		{

			@Override
			public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height)
			{
				FocusOMeterGLRenderer.setup(glautodrawable.getGL().getGL2(), width, height);
			}

			@Override
			public void init(GLAutoDrawable glautodrawable)
			{
			}

			@Override
			public void dispose(GLAutoDrawable glautodrawable)
			{
			}

			@Override
			public void display(GLAutoDrawable glautodrawable)
			{
				FocusOMeterGLRenderer.render(glautodrawable.getGL().getGL2(), glautodrawable.getSurfaceWidth(), glautodrawable.getSurfaceHeight());
			}
		});

		final JFrame jframe = new JFrame("Focus-O-Meter");
		jframe.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent windowevent)
			{
				jframe.dispose();
				System.exit(0);
			}
		});

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();

		jframe.getContentPane().add(glcanvas, BorderLayout.CENTER);
		jframe.setSize(100, height);
		jframe.setLocation(width - 100, 0);
		jframe.setAlwaysOnTop(true);
		jframe.setUndecorated(true);
		jframe.setVisible(true);

	}
	
	@Override
	public void updateCurrentFeedback(double currentFeedback)
	{
		super.updateCurrentFeedback(currentFeedback);
		setCurrentFeedback((float)currentFeedback);
	}
}