package org.neurovillage.main.network;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.neurovillage.main.Launcher;
import org.neurovillage.model.Config;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPort;
import com.illposed.osc.OSCPortIn;

public class OSCReceiver
{
	protected OSCPortIn receiver;
	protected ArrayList<String> senderList;
	protected boolean exit = false;
	protected boolean listening = false;
	protected JTextArea listenerTextList;
	protected JTextArea messageOutputTextList;
	protected JFrame frame;
	protected JLabel statusLabel;
	protected JTextField ipField;
	protected JTextField portField;
	
	protected boolean forwarding = false;
	protected JButton forwardButton;

	public static void main(String[] args) throws SocketException
	{
//		Config config = new Config(Launcher.defaultConfig);
		ArrayList<String> senderList = new ArrayList<>();
		senderList.add("/dev/rfcomm0/");
		senderList.add("/dev/rfcomm1/");
		OSCReceiver or = new OSCReceiver(7009, senderList);
		
		OSCListener listener = new OSCListener()
		{
			public void acceptMessage(java.util.Date time, OSCMessage message)
			{
				
				System.out.println("received " + message.getAddress() + ":");
				for (Object argument : message.getArguments())
					System.out.println(argument.toString());

			}
		};
		or.addOSCListener("/sayhello1", listener);
		or.addOSCListener("/sayhello2", listener);
		or.startListening();
		
	}
	
	public void addOSCListener(String address, OSCListener oscListener)
	{
		receiver.addListener(address, oscListener);
		listenerTextList.append(address+"\n");
	}

	public void startListening()
	{
		receiver.startListening();
		listening = true;
		statusLabel.setForeground(new Color(0,155,0));
		statusLabel.setText("listening");
	}

	public void stopListening()
	{
		receiver.stopListening();
		listening = false;
		statusLabel.setForeground(new Color(255,0,0));
		statusLabel.setText("mute");
	}

	public OSCReceiver(int port, ArrayList<String> senderList) throws SocketException
	{

		// receiver = new OSCPortIn(OSCPort.defaultSCOSCPort());
		receiver = new OSCPortIn(port);
		
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		
		frame = new JFrame();
		frame.setSize(400, 600);
		frame.setLocationRelativeTo(null);
		
		
		listenerTextList = new JTextArea();
		listenerTextList.setEditable(false);
		JScrollPane lPane = new JScrollPane(listenerTextList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		lPane.setPreferredSize(new Dimension(400, 200));
//		lPane.setSize(new Dimension(400, 360));
//		lPane.setMaximumSize(new Dimension(200, 360));

		messageOutputTextList = new JTextArea();
		messageOutputTextList.setPreferredSize(new Dimension(500, 360));
		messageOutputTextList.setEditable(false);
		JScrollPane mPane = new JScrollPane(messageOutputTextList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		ipField = new JTextField("127.0.0.1");
		portField = new JTextField("7077");
		
		forwardButton = new JButton();
		forwardButton.setText("forward");
		forwardButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				forward();
			}
		});
		
		
		JButton stopButton = new JButton();
		stopButton.setText("exit");
		stopButton.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				OSCReceiver.this.stopListening();
            	receiver.close();
				System.exit(0);
			}
		});
		
		frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (listening)
                {
                	OSCReceiver.this.stopListening();
                	receiver.close();
                }
                e.getWindow().dispose();
            }
        });
		
		statusLabel = new JLabel("mute");
		statusLabel.setBackground(new Color(255,0,0));
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.add(statusLabel);
		buttons.add(stopButton);
		buttons.add(ipField);
		buttons.add(portField);
		buttons.add(forwardButton);
		
		content.add(lPane);
		content.add(mPane);
		content.add(buttons);

		frame.add(content);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	public void forward()
	{
		
	}

	public OSCPortIn getReceiver()
	{
		return receiver;
	}

	public void setReceiver(OSCPortIn receiver)
	{
		this.receiver = receiver;
	}

	public ArrayList<String> getSenderList()
	{
		return senderList;
	}

	public void setSenderList(ArrayList<String> senderList)
	{
		this.senderList = senderList;
	}
	


}
