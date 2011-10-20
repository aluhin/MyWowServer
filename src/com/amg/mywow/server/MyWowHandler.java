package com.amg.mywow.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.amg.mywow.common.LoginData;
import com.amg.mywow.common.Packet;
import com.amg.mywow.server.entities.Customer;

public class MyWowHandler implements Runnable {

	static Logger logger = Logger.getLogger(MyWowHandler.class);
	
	static int idCount = 0;
	
	private HandlerManager handlerManager;
	private SSLSocket sslSocket;

	private ObjectOutputStream oos;
	private ObjectInputStream ois;

	private boolean isLogged;
	private int characterId;

	MyWowHandler(HandlerManager handlerManager, SSLSocket sslSocket) {
		this.handlerManager = handlerManager;
		this.sslSocket = sslSocket;
		isLogged = false;
		characterId = -1;
	}

	public void run() {
		try {
			oos = new ObjectOutputStream(sslSocket.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(sslSocket.getInputStream());

			authenticateUser();

			if (!isLogged) {
				logger.debug("Client was rejected when trying to log in");
				//sslSocket.close();
				return;
			}

			logger.debug("Customer correctly logged in with id: " + characterId);

			while (true) {
				Packet packet = receivePacket();
				// TODO: think if we want to handle specific disconnection or just catch SocketException (check if packet.customerId is the correct one!!!)
				if (packet == null || packet.getAction() == Packet.ACTION_DISCONNECT) {
					//logger.debug("Client disconnected");
					continue;
				}
				logger.debug("Handler " + Thread.currentThread().getName() + " received packet: " + packet);

				distributeAction(packet);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (isLogged) {
				synchronized (handlerManager.getHandlersTable()) {
					handlerManager.getHandlersTable().remove(characterId);
				}
				logger.debug("Client disconnected");
			}
		}
	}

	private void authenticateUser() throws IOException {

		Packet packet = receivePacket(); // TODO: check correct action
		LoginData loginData = (LoginData) packet.getData();
		System.out.println("Received loggin parameters, user: " + loginData.getUserName() + " password: " + loginData.getPassword());

		Session session = HibernateUtil.getSessionFactory().getCurrentSession();

		Customer customer = null;
		boolean isConnectionPerformed = false;
		int tries = 0;
		
		do {
			try {
				tries++;
				session.beginTransaction();
		
				customer = (Customer) session
						.createQuery("from Customer where name = ? and password = ?")
						.setString(0, loginData.getUserName()).setString(1, loginData.getPassword())
						.uniqueResult();
				
				session.getTransaction().commit();
				isConnectionPerformed = true;
			} catch (HibernateException e) {
				// TODO: this is wrong: createQuery(), uniqueResult() and commit() may also throw HibernateException and we cannot retry --> create specific retry catch for .beginTransaction()
				System.out.println("Retrying creating connection: " + e + " TRY: " + tries);
			}
		} while (!isConnectionPerformed);
		
		boolean isAlreadyLogged = false;
		if (customer != null) {
			characterId = customer.getId();

			// We synchronize here in case two handlers try to add themselves at the same time
			synchronized (handlerManager.getHandlersTable()) {
				// TODO: Remove this line! they emulate always login situation for performance test purposes
				characterId += idCount++ - 1;
				
				if (handlerManager.getHandlersTable().containsKey(characterId)) {
					isAlreadyLogged = true;				
					//logger.debug("User tried to log in when already logged in");
				}
				else {
					handlerManager.getHandlersTable().put(characterId, this);
					isLogged = true;
				}
			}
		}

		// TODO: Remove these two lines! they emulate always login situation for performance test purposes
		isLogged = true;
		isAlreadyLogged = false;

		byte response = isLogged ? Packet.RESPONSE_LOGIN_OK : Packet.RESPONSE_LOGIN_INCORRECT;
		if (isAlreadyLogged) {
			// TODO: The user is already logged. Should we instead kick out the previous one? I don't think so, just inform the user 
			response = Packet.RESPONSE_LOGIN_ALREADY;
		}

		sendPacket(new Packet(Packet.ACTION_LOGIN, response, characterId));
	}

	private void distributeAction(Packet packet) throws IOException {
		if (packet.getCharacterId() != characterId) {
			System.out.println("Packet discarded due to different characters Id. Should be " + characterId + " Packet: " + packet);
		}
			
		switch (packet.getAction()) {
		case Packet.ACTION_MOVEMENT: {
			
			// Check validity of the movement
			sendPacket(new Packet(Packet.ACTION_MOVEMENT_CONFIRMATION, Packet.RESPONSE_GENERIC_OK, characterId));
			synchronized (handlerManager.getActionsToDistribute()) {
				handlerManager.getActionsToDistribute().add(packet);
			}
			break;
		}
		case Packet.ACTION_ATTACK: {
			oos.writeUTF("ACTION ATACK");
			oos.flush();
			oos.reset();
			break;
		}
		default: {
			oos.writeUTF("ACTION UNKOUWN");
			oos.flush();
			oos.reset();
			break;
		}
		}
	}
	
	public synchronized void sendPacket(Packet packet) {
		try {
			oos.writeObject(packet);
			oos.flush();
			oos.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Packet sendAndReceivePacket(Packet packet) {
		sendPacket(packet);
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
	
	private Packet receivePacket() {
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
}
