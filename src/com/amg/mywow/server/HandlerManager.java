package com.amg.mywow.server;

import java.io.IOException;
import java.util.Hashtable;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;

public class HandlerManager implements Runnable {

	static Logger logger = Logger.getLogger(HandlerManager.class);
	
	private int actionPort;

	private Hashtable<Integer, MyWowHandler> handlersTable;
	
	HandlerManager(int actionPort) {
		this.actionPort = actionPort;
		handlersTable = new Hashtable<Integer, MyWowHandler>();
	}

	public void run() {
		
		try {
			SSLServerSocketFactory sslSocketFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			SSLServerSocket sslServerSocket = (SSLServerSocket)sslSocketFactory.createServerSocket(actionPort);
			sslServerSocket.setEnabledCipherSuites(new String[]{"SSL_RSA_WITH_RC4_128_MD5"});
			
			int counterClients = 0;
			
			while (true) {
				//logger.debug("Listening incomming binding requests");
				SSLSocket sslClientSocket = (SSLSocket) sslServerSocket.accept();
				//logger.debug("Incomming accept request arrived");
				
				System.out.println("SERVER Local port number: " + sslClientSocket.getLocalPort());
				System.out.println("SERVER Remote port number: " + sslClientSocket.getPort());
				
				MyWowHandler handler = new MyWowHandler(this, sslClientSocket);
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

}
