package com.amg.mywow.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.amg.mywow.server.entities.Customer;

public class MyWowHandler implements Runnable {

	// Generic response code
	public static final byte RESPONSE_0 = 0;
	public static final byte RESPONSE_1 = 1;
	public static final byte RESPONSE_2 = 2;

	public static final short ACTION_DISCONNECT = -1;
	public static final short ACTION_MOVE = 0;
	public static final short ACTION_ATACK = 1;

	static Logger logger = Logger.getLogger(MyWowHandler.class);

	private HandlerManager handlerManager;
	private SSLSocket sslSocket;

	private ObjectOutputStream oos;
	private ObjectInputStream ois;

	private boolean isLogged;
	private int customerId;

	MyWowHandler(HandlerManager handlerManager, SSLSocket sslSocket) {
		this.handlerManager = handlerManager;
		this.sslSocket = sslSocket;
		isLogged = false;
		customerId = -1;
	}

	public void run2() {
		try {
			oos = new ObjectOutputStream(sslSocket.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(sslSocket.getInputStream());
			
			authenticateUser();
			
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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

			//logger.debug("Customer correctly logged in with id: " + customerId);

			while (true) {
				short action = -1;
				action = ois.readShort();
				if (action == ACTION_DISCONNECT) {
					handlerManager.getHandlersTable().remove(customerId);
					//sslSocket.close();
					//logger.debug("Client disconnected");
					return;
				}
				//logger.debug("Handler " + Thread.currentThread().getName() + " received request with action: " + action);

				distributeAction(action);
			}
		} catch (IOException e) {
			if (isLogged) {
				handlerManager.getHandlersTable().remove(customerId);
				//logger.debug("Client disconnected");
			}
			else
				e.printStackTrace();
		}
	}

	private void authenticateUser2() throws IOException {
		String customerName = ois.readUTF();
		String customerPassword = ois.readUTF();

		isLogged = true;
		
		oos.writeByte(0);
		oos.flush();
		oos.reset();
	}
	private void authenticateUser() throws IOException {

		String customerName = ois.readUTF();
		String customerPassword = ois.readUTF();
		//System.out.println("Received loggin parameters, user: " + customerName + " password: " + customerPassword);

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
						.setString(0, customerName).setString(1, customerPassword)
						.uniqueResult();
				
				session.getTransaction().commit();
				isConnectionPerformed = true;
			} catch (HibernateException e) {
				System.out.println("Retrying creating connection: " + e + " TRY: " + tries);
			}
		} while (!isConnectionPerformed);
		
		boolean isAlreadyLogged = false;
		if (customer != null) {
			customerId = customer.getId();

			// We synchronize here in case two same users try to enter at the same time
			synchronized (handlerManager) {
				if (handlerManager.getHandlersTable().containsKey(customerId)) {
					isAlreadyLogged = true;				
					//logger.debug("User tried to log in when already logged in");
				}
				else {
					handlerManager.getHandlersTable().put(customerId, this);
					isLogged = true;
				}
			}
		}

		// TODO: these 2 lines must go away!!!
		isLogged = true;
		isAlreadyLogged = false;

		byte response = isLogged ? RESPONSE_0 : RESPONSE_1;
		if (isAlreadyLogged) {
			// TODO: The user is already logged. Should we kick out the previous one ?
			response = RESPONSE_2;
		}

		oos.writeByte(response);
		oos.flush();
		oos.reset();
		
	}

	private void distributeAction(short action) throws IOException {
		switch (action) {
		case ACTION_MOVE: {
			oos.writeUTF("ACTION MOVE");
			oos.flush();
			oos.reset();
			break;
		}
		case ACTION_ATACK: {
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
}
