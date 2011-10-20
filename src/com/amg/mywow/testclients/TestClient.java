package com.amg.mywow.testclients;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class TestClient implements Runnable{
	
	private static final int DEFAULT_ACTION_PORT = 4444;
	
	// MacOS limit number of threads ~2540
	private static final int NUMBER_OF_CLIENTS = 10;
	private static final int NUMBER_OF_SENTS = 10000000;
	
	// Generic response code
	public static final byte RESPONSE_0 = 0;
	public static final byte RESPONSE_1 = 1;
	public static final byte RESPONSE_2 = 2;
	
	public static final short ACTION_MOVE = 0;
	public static final short ACTION_ATACK = 1;
	
	public static void main(String[] args) throws InterruptedException {
		TestClient[] clients = new TestClient[NUMBER_OF_CLIENTS];
		for (int i = 0; i < clients.length; i++)
			clients[i] = new TestClient();
		Thread[] threads = new Thread[clients.length];
		for (int i = 0; i < clients.length; i++)
			threads[i] = new Thread(clients[i]);	
		for (int i = 0; i < clients.length; i++)
			threads[i].start();
		for (int i = 0; i < clients.length; i++)
			threads[i].join();
	}
	
	public void run() {
		try {
			System.setProperty("javax.net.ssl.trustStore", "mySrvKeystore");
			System.setProperty("javax.net.ssl.trustStorePassword","micuenta");

			SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			
			SSLSocket sslSocket = null;
			boolean isConnected = false;
			int tries = 0;
			while (!isConnected) {
				try {
					sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", DEFAULT_ACTION_PORT);
					sslSocket.setEnabledCipherSuites(new String[]{"SSL_RSA_WITH_RC4_128_MD5"});
					sslSocket.startHandshake();
					isConnected = true;
				} catch (SocketException e) {
					tries++;
					System.out.println("ERROR WHEN CONNECTING: " + e + " TRY: " + tries);
				}
			}
			
			//System.out.println("Just connected to " + sslSocket.getRemoteSocketAddress());
			
			ObjectOutputStream oos = new ObjectOutputStream(sslSocket.getOutputStream());
			oos.flush();
			ObjectInputStream ois = new ObjectInputStream(sslSocket.getInputStream());
			
			// Perform login
			oos.writeUTF("username");
			oos.flush();
			oos.writeUTF("password");
			oos.flush();
			
			byte loginResponse = ois.readByte();
			//System.out.println("LOGIN RESPONSE: " + responseByte);
			
			if (loginResponse != RESPONSE_0) {
				System.out.println("Could not login correctly");
				sslSocket.close();
				return;
			}
			
			//System.out.println("Correctly logged in");
			
			for (int i = 0; i < NUMBER_OF_SENTS; i++) {
				oos.writeShort(ACTION_MOVE);
				oos.flush();
				oos.reset();
				String response = ois.readUTF();
				System.out.println(i + " ACTION RESPONSE in client " + Thread.currentThread().getId() + ": " + response);
			}
			sslSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
