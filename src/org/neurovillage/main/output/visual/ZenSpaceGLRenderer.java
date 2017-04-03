package org.neurovillage.main.output.visual;

/**
 * 
 */
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import org.neurovillage.tools.ResourceManager;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class ZenSpaceGLRenderer
{
	private float currentFeedback = 0;

	private int width = 2;
	private int height = 2;
	private Texture texYantra;
	private int shortSide;
	private int shortSideHalf;
	private int offsetX;
	private int offsetY;
	private Texture[] textures;
	private int currentForestIn = 2;
	private int currentForestOut = 1;
	
	private Texture noiseTexture = null;
	private float angle = 0f;
	
	private String[] themes = {"nforest", "universe"};
	int currentTheme = 1;


	private float oldFeedback = 0f;

	
	public ZenSpaceGLRenderer(GLProfile glprofile2)
	{
		this.glprofile = glprofile2;
	}
	
	public void nextTheme()
	{
		currentTheme++;
		if (currentTheme+1>themes.length)
			currentTheme = 0;
	}
	
	
	
	public void setCurrentFeedback(float currentFeedback)
	{
		this.currentFeedback = currentFeedback;
//		if (currentFeedback>0)
//			this.currentFeedback = oldFeedback  *.75f + currentFeedback * .25f;
//		else
//			this.currentFeedback = oldFeedback  *.95f + currentFeedback * .05f;
//		
//		oldFeedback = this.currentFeedback;
//		System.out.println("cf:" + currentFeedback);
	}

	public void setup(GL2 gl2, int width, int height)
	{
		angle = 0f;
		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glLoadIdentity();

		// coordinate system origin at lower left with width and height same as
		// the window
		GLU glu = new GLU();
		glu.gluOrtho2D(0.0f, width, 0.0f, height);

		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glLoadIdentity();

		gl2.glViewport(0, 0, width, height);

		this.width = width;
		this.height = height;

		if (texYantra == null)
		{
			try
			{
				BufferedImage img = ImageIO.read(ResourceManager.getInstance().getResource("yantra_white.png"));
				texYantra = AWTTextureIO.newTexture(glprofile, img, false);
				
				textures = new Texture[3];
				for (int i = 0; i < textures.length; i++)
				{
					textures[i] = AWTTextureIO.newTexture(glprofile, ImageIO.read(ResourceManager.getInstance().getResource(themes[currentTheme]+(i+1)+".png")), false);
				}
				
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		if (width<height)
		{
			shortSide = width;
			offsetX = 0;
			offsetY = height / 2;
//			shortSide = Math.min(width, height);
//			shortSideHalf = shortSide/2;
		}
		else
		{
			shortSide = height;
			offsetX = width / 2;
			offsetY = 0;
			
		}
		shortSideHalf = shortSide/2;
			

	}


//	private Random random = new Random();
	public void render(GL2 gl2, int width, int height)
	{
//		System.out.println("current feedback:  " + currentFeedback);
//		float currentFeedback = new Float(this.currentFeedback);
//		if (currentFeedback == Float.NaN)
//			currentFeedback = 0f;
//		angle += 1f -currentFeedback;
//		float correctedFeedback = Math.max(Math.min(.98f, (currentFeedback+.0001f)*2f),0f);
		
//		System.out.println(System.nanoTime() + ":\trendering...");
		
		if ((currentFeedback<0) || Float.isNaN(currentFeedback))
			currentFeedback = 0;
		else if (currentFeedback>1)
			currentFeedback = 1;
		
		angle +=  .25f - currentFeedback/4f;
//		angle +=  1f - correctedFeedback;
//		if (angle==Float.NaN)
//			angle = random.nextFloat()*719f;
//		System.out.println("angle:" + angle );
		
		if (angle > 720f)
		{
			currentForestOut=currentForestIn;
			currentForestIn++;
			
			if (currentForestIn > textures.length-1)
				currentForestIn = 0;
			
			angle = 0f;
		}
		
		gl2.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl2.glLoadIdentity();
		gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl2.glEnable(GL2.GL_BLEND);
//		gl2.glTranslatef(0, 0, -250.0f);
		gl2.glColor4f(1, 1, 1, 1);


		
		int i = currentForestIn;
		
		float scaleIn = angle/720f;
		float scaleInSquared = (float)Math.sqrt(scaleIn);
		textures[i].bind(gl2);
		textures[i].enable(gl2);
		gl2.glPushMatrix();
		gl2.glTranslatef((float)(width/2), (float)(height/2), 0);
		gl2.glRotatef(180, 0, 0, -1);
		gl2.glScalef(1f+scaleIn/2f, 1f+scaleIn/2f, 1);
		gl2.glColor4f(.5f, .2f, 1, scaleInSquared);
		gl2.glBegin(GL2.GL_QUADS);
		gl2.glTexCoord3f(0, 0, 0);
		gl2.glVertex3f(-width/2f, -height/2f, 0);
		gl2.glTexCoord3f(1, 0, 0);
		gl2.glVertex3f(width/2f, -height/2f, 0);
		gl2.glTexCoord3f(1, 1, 0);
		gl2.glVertex3f(width/2f, height/2f, 0);
		gl2.glTexCoord3f(0, 1, 0);
		gl2.glVertex3f(-width/2f, height/2f, 0);
		gl2.glEnd();
		gl2.glPopMatrix();
		textures[i].disable(gl2);
		
		i = currentForestOut;
		textures[i].bind(gl2);
		textures[i].enable(gl2);
		gl2.glPushMatrix();
		gl2.glTranslatef((float)(width/2), (float)(height/2), 0);
		gl2.glRotatef(180, 0, 0, -1);
		gl2.glScalef(1.5f + scaleIn, 1.5f + scaleIn, 1);
		gl2.glColor4f(.5f, .2f, 1, 1f-scaleIn);
//		gl2.glColor4f(1, 1, 1, 1f);
		gl2.glBegin(GL2.GL_QUADS);
		gl2.glTexCoord3f(0, 0, 0);
		gl2.glVertex3f(-width/2f, -height/2f, 0);
		gl2.glTexCoord3f(1, 0, 0);
		gl2.glVertex3f(width/2f, -height/2f, 0);
		gl2.glTexCoord3f(1, 1, 0);
		gl2.glVertex3f(width/2f, height/2f, 0);
		gl2.glTexCoord3f(0, 1, 0);
		gl2.glVertex3f(-width/2f, height/2f, 0);
		gl2.glEnd();
		gl2.glPopMatrix();
		textures[i].disable(gl2);
		
		
		texYantra.bind(gl2);
		texYantra.enable(gl2);
		gl2.glPushMatrix();
		gl2.glTranslatef((float)(width/2), (float)(height/2), 0);
		gl2.glRotatef(angle/2f, 0, 0, 1);
		gl2.glColor4f(1, 1, 0, 1f);
//		gl2.glColor4f(1, 1, 0, .7f + currentFeedback/2f);
		gl2.glBegin(GL2.GL_QUADS);
		gl2.glTexCoord3f(0, 0, 0);
		gl2.glVertex3f(-shortSideHalf, -shortSideHalf, 0);
		gl2.glTexCoord3f(1, 0, 0);
		gl2.glVertex3f(shortSideHalf, -shortSideHalf, 0);
		gl2.glTexCoord3f(1, 1, 0);
		gl2.glVertex3f(shortSideHalf, shortSideHalf, 0);
		gl2.glTexCoord3f(0, 1, 0);
		gl2.glVertex3f(-shortSideHalf, shortSideHalf, 0);
		gl2.glEnd();
		gl2.glPopMatrix();

		texYantra.bind(gl2);
		texYantra.enable(gl2);
		gl2.glPushMatrix();
		gl2.glTranslatef((float)(width/2), (float)(height/2), 0);
		gl2.glRotatef(180, 0, -1, 0);
		gl2.glRotatef(angle/2f, 0, 0, 1);
		gl2.glColor4f(0, 1, 1, 1f);
//		gl2.glColor4f(0, 1, 1, .5f + currentFeedback/2f);
		gl2.glBegin(GL2.GL_QUADS);
		gl2.glTexCoord3f(0, 0, 0);
		gl2.glVertex3f(-shortSideHalf, -shortSideHalf, 0);
		gl2.glTexCoord3f(1, 0, 0);
		gl2.glVertex3f(shortSideHalf, -shortSideHalf, 0);
		gl2.glTexCoord3f(1, 1, 0);
		gl2.glVertex3f(shortSideHalf, shortSideHalf, 0);
		gl2.glTexCoord3f(0, 1, 0);
		gl2.glVertex3f(-shortSideHalf, shortSideHalf, 0);
		gl2.glEnd();
		gl2.glPopMatrix();
		texYantra.disable(gl2);
		
//		if (noiseTexture==null)
//			noiseTexture = generateWhiteNoise(gl2, ZenSpaceGLRenderer.width, ZenSpaceGLRenderer.height);
//		noiseTexture.bind(gl2);
//		noiseTexture.enable(gl2);
//
//		gl2.glPushMatrix();
//		gl2.glBegin(GL2.GL_QUADS);
//		gl2.glTexCoord3f(0, 0, 0);
//		gl2.glVertex3f(0, 0, 0);
//		gl2.glTexCoord3f(1, 0, 0);
//		gl2.glVertex3f(width, 0, 0);
//		gl2.glTexCoord3f(1, 1, 0);
//		gl2.glVertex3f(width, height, 0);
//		gl2.glTexCoord3f(0, 1, 0);
//		gl2.glVertex3f(0, height, 0);
//		gl2.glEnd();
//		gl2.glPopMatrix();
//		noiseTexture.disable(gl2);
		
		
	}

	private static Random random = new Random(); // Seed to 0 for testing
	public GLProfile glprofile;

	public Texture generateWhiteNoise(GL2 gl2, int width, int height)
	{
		float[] noise = new float[width * height];
		// float[][] noise = new float[width][height];

		BufferedImage bufferedImage = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < width / 2; i++)
			for (int j = 0; j < height / 2; j++)
			{
				int a = (int) (255 * (.5f - currentFeedback / 2f));
				// int r = random.nextInt(256);
				int g = random.nextInt(256);
				int b = random.nextInt(256);
				int color = (a << 24) | (0 << 16) | (g << 8) | b;
				bufferedImage.setRGB(i, j, color);
			}
		Texture texture = AWTTextureIO.newTexture(glprofile, bufferedImage, false);
		return texture;
	}

}