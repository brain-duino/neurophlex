package org.neurovillage.main.output.visual;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import org.neurovillage.main.output.feedback.Feedback;
import org.neurovillage.model.FeedbackSettings;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

public class ZenSpaceVisuals extends Feedback
{

	private GLCanvas glcanvas;
	private double currentFeedback = 0;
	private ZenSpaceGLRenderer zenSpaceRenderer;

	public void setCurrentFeedback(float currentFeedback)
	{
		zenSpaceRenderer.setCurrentFeedback(currentFeedback);
		glcanvas.repaint();
	}

	public ZenSpaceVisuals(FeedbackSettings feedbackSettings)
	{
		super(feedbackSettings);

		GLProfile glprofile = GLProfile.getDefault();
		
		zenSpaceRenderer = new ZenSpaceGLRenderer(glprofile);
		GLCapabilities glcapabilities = new GLCapabilities(glprofile);
		glcanvas = new GLCanvas(glcapabilities);
		
		FPSAnimator fpsAnimator = new FPSAnimator(glcanvas, 60, true);
		
		glcanvas.addGLEventListener(new GLEventListener()
		{

			@Override
			public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height)
			{
				zenSpaceRenderer.setup(glautodrawable.getGL().getGL2(), width, height);
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
				zenSpaceRenderer.render(glautodrawable.getGL().getGL2(), glautodrawable.getSurfaceWidth(), glautodrawable.getSurfaceHeight());
			}
		});

		final JFrame jframe = new JFrame("Neurofox Zen Space");
		jframe.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent windowevent)
			{
//				jframe.dispose();
//				System.exit(0);
//				jframe.
			}
		});

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();

		jframe.getContentPane().add(glcanvas, BorderLayout.CENTER);
		jframe.setSize(width - (width/5), height - (height/5));
		jframe.setLocation((width/10), (height/10));
		jframe.setAlwaysOnTop(true);
//		jframe.setUndecorated(true);
		jframe.setVisible(true);
		
		fpsAnimator.start();
		
		jframe.addWindowListener(new WindowListener() {
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
		

	}
	
	@Override
	public void updateCurrentFeedback(double currentFeedback)
	{
		super.updateCurrentFeedback(currentFeedback);
		setCurrentFeedback((float)currentFeedback);
	}
}