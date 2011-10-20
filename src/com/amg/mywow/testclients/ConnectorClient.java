package com.amg.mywow.testclients;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.amg.mywow.common.Packet;

public abstract class ConnectorClient {
	
	private static final int CONECTION_RETRIES = 10;
	
	private SSLSocket sslSocket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private String host;
	private int port;
	private boolean asynchronousMode;

	public ConnectorClient(String host, int port) {
		this(host,port,false);
	}
	
	public ConnectorClient(String host, int port, boolean asynchronousMode) {
		this.host = host;
		this.port = port;
		this.asynchronousMode = asynchronousMode;
		System.setProperty("javax.net.ssl.trustStore", "mySrvKeystore");
		System.setProperty("javax.net.ssl.trustStorePassword","micuenta");
	}

	// TODO: in asynchronous mode there are conflicts between synchronous and asynchronous answers
	// Possible solution could be implementing a timeout for synchronous operations and sharing 
	// the response. Mind synchronization.
	// Simplest solution: different ports
	public void connect() {
		new Thread(new Runnable() {
			public void run() {
				try {
					SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					int retries = CONECTION_RETRIES;
					boolean isConnected = false;
					sslSocket = null;
					while (!isConnected && retries > 0) {
						try {
							sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);
							sslSocket.setEnabledCipherSuites(new String[]{"SSL_RSA_WITH_RC4_128_MD5"});
							sslSocket.startHandshake();
							isConnected = true;
						} catch (IOException e) {
							try {
								retries--;
								Thread.sleep(100);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
					}
					
					if (!isConnected)
					{
						processConnection(false);
						return;
					}
					oos = new ObjectOutputStream(sslSocket.getOutputStream());
					oos.flush();
					ois = new ObjectInputStream(sslSocket.getInputStream());

					if (asynchronousMode) {
						new Thread(new Runnable() {
							public void run() {
								while (true) {
									//try {
										if (sslSocket.isClosed())
											return;
										//processPacket((Packet) ois.readObject());
										Packet packet = receivePacket();
										processPacket(packet);
//									} catch (IOException e) {
//										// Nothing happen, socket disconnected maybe so we check
//									} catch (ClassNotFoundException e) {
//										e.printStackTrace();
//									}
								}
							}
						}).start();
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
					processConnection(false);
					return;
				} catch (IOException e) {
					e.printStackTrace();
					processConnection(false);
					return;
				}
				processConnection(true);
			}
		}).start();
	}
	
	public void disconnect() {
		try {
			sslSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPacket(Packet packet) {
		try {
			oos.writeObject(packet);
			oos.flush();
			oos.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Packet sendAndReceivePacket(Packet packet) {
		sendPacket(packet);
		return receivePacket();
	}
	
	public Packet receivePacket() {
		Packet returnPacket = null;

		try {
			returnPacket = (Packet) ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return returnPacket;
	}

	public abstract void processPacket(Packet packet);
	
	public abstract void processConnection(boolean connected);
}
