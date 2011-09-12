package fi.iki.bzar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.IOException;
import java.util.ArrayList;

import fi.iki.bzar.gamenode.*;
import javax.swing.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

public class GamenodeClientUI extends JFrame implements ConnectHandler, DisconnectHandler, MessageHandler, ErrorHandler, WindowListener {
	public class Skeleton {
		GamenodeClientUI ui;
		
		public Skeleton(GamenodeClientUI myUi) {
			ui = myUi;
		}
		
		public Object echo(Object... params) {
			return params[0];
		}
		
		public Object rmiTest(Object... params) {
			ui.addMessage("rmiTest successful!");
			return params[0];
		}
	}
	
	final private GamenodeClient client;
	final private JTextArea outputArea;
	final private JScrollPane scrollArea;
	final private JTextField inputArea;
	
	public GamenodeClientUI() {
		setSize(300, 400);
		addWindowListener(this);
		setTitle("GamenodeClient tester");
		
		JPanel w = new JPanel();
		w.setLayout(new BoxLayout(w, BoxLayout.Y_AXIS));
		add(w);
		
		client = new GamenodeClientImpl(new GamenodeSkeletonImpl(new Skeleton(this)));
		outputArea = new JTextArea();
		outputArea.setEnabled(false);
		outputArea.setDisabledTextColor(Color.black);
		scrollArea = new JScrollPane();
		scrollArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		inputArea = new JTextField();
		inputArea.setMaximumSize(new Dimension(5000, 24));
		
		scrollArea.getViewport().add(outputArea);
		w.add(scrollArea);
		w.add(inputArea);		
		
		client.onConnect(this);
		client.onMessage(this);
		client.onError(this);

		try {
			client.connect("ws://localhost:8888");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		inputArea.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				String[] parts = cmd.split(" ", 2);
				String command = parts[0];
				if(command.equals("call") && parts.length == 2) {
					String params = parts[1];
					String[] callParts = params.split(" ", 2);
					String method = callParts[0];
					String methodParamString = "null";
					if(callParts.length == 2) {
						methodParamString = callParts[1];
					}
					
					try {
						ArrayList<Object> paramArray = new ArrayList<Object>();
						JSONTokener tokener = new JSONTokener(methodParamString);
						while(tokener.more()) {
							paramArray.add(tokener.nextValue());
							tokener.next();
						}
						
						client.getStub().call(method, paramArray.toArray()).onReturn(new Callback() {
							@Override
							public void exec(Object params) {
								addMessage("RESPONSE: " + params.toString());
							}
						});
						inputArea.selectAll();
					} catch (NoSuchMethodException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
				} else if(command.equals("msg") && parts.length == 2) {
					String messageString = parts[1];
					try {
						ArrayList<Object> paramArray = new ArrayList<Object>();
						JSONTokener tokener = new JSONTokener(messageString);
						while(tokener.more()) {
							paramArray.add(tokener.nextValue());
							tokener.next();
						}
						
						if(paramArray.size() == 1) {
							client.sendMessage(paramArray.get(0));
						} else {
							client.sendMessage(paramArray);
						}
						inputArea.selectAll();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
				} else if(command.equals("methods")) {
					addMessage(client.getStub().methodList().toString());
					inputArea.selectAll();
				}
			}
		});
	}
	
	public void addMessage(String message) {
		outputArea.append(message + "\n");
		scrollArea.getVerticalScrollBar().setValue(scrollArea.getVerticalScrollBar().getMaximum());
	}
	
	public static void main(String[] args) {
		GamenodeClientUI ui = new GamenodeClientUI();
		ui.setVisible(true);
	}

	@Override
	public void onError(Object error) {
		addMessage("ERROR: " + error.toString());
	}

	@Override
	public void onMessage(Object message) {
		addMessage("MESSAGE: " + message.toString());
	}

	@Override
	public void onDisconnect() {
		addMessage("Disconnected");
	}

	@Override
	public void onConnect() {
		addMessage("Connected\n");
		addMessage("Commands:");
		addMessage("call <method> <parameters> - call a method");
		addMessage("msg <message> - send a message");
		addMessage("methods - server method list\n");
	}

	@Override
	public void windowClosing(WindowEvent e) {
		System.out.println("Shutting down");
		client.disconnect();
		System.exit(0);
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}
}
