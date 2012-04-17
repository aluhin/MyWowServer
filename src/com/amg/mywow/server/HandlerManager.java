package com.amg.mywow.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;

import com.amg.mywow.common.Packet;

public class HandlerManager implements Runnable {

	static Logger logger = Logger.getLogger(HandlerManager.class);
	
	private int actionPort;

	private Hashtable<Integer, MyWowHandler> handlersTable;
	private List<Packet> actionsToDistribute;
	
	HandlerManager(int actionPort) {
		this.actionPort = actionPort;
		handlersTable = new Hashtable<Integer, MyWowHandler>();
		actionsToDistribute = new ArrayList<Packet>();
	}

	public void run() {
		
		new Thread(new ActionDistributor(this)).start();
		
		try {
			ServerSocket serverSocket = new ServerSocket(actionPort);
			
			int counterClients = 0;
			
			while (true) {
				//logger.debug("Listening incomming binding requests");
				Socket clientSocket = serverSocket.accept();
				//logger.debug("Incomming accept request arrived");
				
				MyWowHandler handler = new MyWowHandler(this, clientSocket);
				new Thread(handler).start();
				counterClients++;
				System.out.println(counterClients);
			}

		} catch (IOException e) {
			e.printStackTrace();
			logger.warn("IOException in socket listen: " + e);
		}
	}

	public Hashtable<Integer, MyWowHandler> getHandlersTable() {
		return handlersTable;
	}
	
	public synchronized List<Packet> getActionsToDistribute() {
		return actionsToDistribute;
	}
	
	public synchronized void setActionsToDistribute(List<Packet> actionsToDistribute) {
		this.actionsToDistribute = actionsToDistribute;
	}
}
