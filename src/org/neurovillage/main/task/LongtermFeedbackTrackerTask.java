package org.neurovillage.main.task;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.neurovillage.main.LongtermGraph;
import org.neurovillage.model.DefaultFFTData;
import org.neurovillage.model.FeedbackSettings;
import org.neurovillage.model.Task;

public class LongtermFeedbackTrackerTask implements Task {

	private DefaultFFTData defaultFFData;
	private FeedbackSettings feedbackSettings;
	private LongtermGraph longtermGraph;

	public LongtermFeedbackTrackerTask(DefaultFFTData defaultFFTData, FeedbackSettings feedbackSettings, LongtermGraph longtermGraph) {
		this.defaultFFData = defaultFFTData;
		this.feedbackSettings = feedbackSettings;
		this.longtermGraph = longtermGraph;
	}
	
	@Override
	public void init() {
		
	}

	@Override
	public void run() {
		
	}

	@Override
	public void stop() {
		
	}

}
