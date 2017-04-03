package org.neurovillage.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.neurovillage.main.NFBServer;
import org.neurovillage.main.output.feedback.Feedback;
import org.neurovillage.model.FeedbackSettings;

public class NFBSettingsWindow extends JFrame
{
	private NFBServer nfbServer;
	private FeedbackSettings feedbackSettings;
	
	private JLabel feedbackTitle;
	private JLabel difficultyFactor;
	private JLabel feedbackOutput;
	
	ArrayList<Feedback> feedbacks;
	private JList feedbackSettingsList;
	private DefaultListModel listModel;
	
	private int windowWidth = 300;
	private int windowHeight = 300;
	
	public NFBSettingsWindow(NFBServer nfbServer)
	{
		
		this.nfbServer = nfbServer;
		this.feedbackSettings = nfbServer.getCurrentFeedbackSettings();
		this.setSize(new Dimension(windowWidth, windowHeight));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		this.setVisible(true);
//		this.setLocationRelativeTo(nfbServer);
		
		feedbackTitle = new JLabel(" feedback type: " + feedbackSettings.getFeedbackSettingsName() + " ");
		this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		
		this.add(feedbackTitle);
		
		listModel = new DefaultListModel();
//		ArrayList<Integer> feedbackSettingsIndex = new ArrayList<>();
		
		for (FeedbackSettings feedbackSettings :  nfbServer.getAllSettings() )
			listModel.addElement(feedbackSettings.getFeedbackSettingsName());
		
		
		feedbackSettingsList = new JList(listModel); //data has type Object[]
		feedbackSettingsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		feedbackSettingsList.setLayoutOrientation(JList.VERTICAL);
		feedbackSettingsList.setVisibleRowCount(-1);
		feedbackSettingsList.setSelectedIndex(0);

		JScrollPane feedbackSettingsScroller = new JScrollPane(feedbackSettingsList);
		feedbackSettingsScroller.setMaximumSize(new Dimension(windowWidth, 180));
		
		JButton button = new JButton("select");
//		JButton button2 = new JButton("ok");
		button.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int i = feedbackSettingsList.getSelectedIndex();
//				nfbServer.getCurrentFeedbackSettings().get(i)
				feedbackSettings = nfbServer.getAllSettings().get(i);
				nfbServer.setCurrentFeedbackSettings(feedbackSettings);
				feedbackTitle = new JLabel(" feedback type: " + feedbackSettings.getFeedbackSettingsName() + " ");
				
			}
		});
		button.setHorizontalAlignment(JButton.LEFT);
		
		JPanel feedbackSettingsPanel = new JPanel();
		feedbackSettingsPanel.setLayout(new BoxLayout(feedbackSettingsPanel, BoxLayout.X_AXIS));
		
		feedbackSettingsPanel.add(button);
//		feedbackSettingsPanel.add(button2);
		
		this.add(feedbackSettingsScroller);
		this.add(feedbackSettingsPanel);
		this.revalidate();
		this.setVisible(true);
	}

}
