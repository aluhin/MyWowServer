package com.amg.mywow.testclients;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import com.amg.mywow.common.LoginData;
import com.amg.mywow.common.MovementAction;
import com.amg.mywow.common.Packet;
import com.amg.mywow.common.Position;

public class TestConnectorSingleClient extends ConnectorClient {
	
	private int numberOfMovementsReceived;
	private static int totalNumberOfMovementsReceived;
	private static int numberOfFinishedThreads;
	
	private Object objectLock;
	private boolean isResponseRequested;
	private Packet requestedPacket; 
	private int characterId;
	private Position characterPosition;
	private Map<Integer, Position> enemies;
	
	public TestConnectorSingleClient(String host, int port) {
		super(host, port, true);
		objectLock = new Object();
		enemies = new Hashtable<Integer, Position>();
	}

	public static void main(String[] args) throws InterruptedException {
		TestConnectorSingleClient client = new TestConnectorSingleClient("abiamielpedro.no-ip.org", 4444);
		client.connect();
			try {
				while(true) {
					Thread.sleep(20000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
	}

	@Override
	public void processPacket(Packet packet) {
		//System.out.println("processPacket(): " + packet);
		switch (packet.getAction()) {
		case Packet.ACTION_LOGIN :
			if (isResponseRequested) {
				isResponseRequested = false;
				synchronized (objectLock) {
					requestedPacket = packet;
					objectLock.notify();
					//Thread.currentThread().setName("Character" + packet.getCharacterId());
				}
			}
			break;
		case Packet.ACTION_CHAR_LOCATION:
			if (isResponseRequested && packet.getCharacterId() == characterId) {
				isResponseRequested = false;
				characterPosition = (Position)packet.getData();
				System.out.println("Character " + characterId + " starting position is: " + characterPosition);
				synchronized (objectLock) {
					objectLock.notify();
				}
			}
			else { //TODO: Should we check the case !isResponseRequested && packet.getCharacterId() == characterId ? it should never happen
				System.out.println("New enemy found: " + packet.getCharacterId() + " starting position is: " + (Position)packet.getData());
				enemies.put(packet.getCharacterId(), (Position)packet.getData());
			}
			break;
		case Packet.ACTION_MOVEMENT :
			System.out.println("Character " + characterId + " received movement: " + numberOfMovementsReceived + " " + packet);
			//System.out.println("Movement arrived: " + packet);
			break;
		case Packet.ACTION_MOVEMENT_CONFIRMATION :
			//System.out.println("Character " + characterId +" movement confirmation: " + packet + " --> "+ ((Byte)packet.getData() == Packet.RESPONSE_GENERIC_OK ? "VALID" : "NOT VALID"));
			break;
		case Packet.ACTION_ATTACK :
			//System.out.println("Attack arrived: " + packet);
			break;
		default :
			//System.out.println("Unrecognized action: " + packet);
			break;
		}
	}

	@Override
	public void processConnection(boolean isConnected) {
		if (!isConnected) {
			System.out.println("Client could not connect");
			return;
		}
		System.out.println("Client connected correctly");
		
		LoginData loginData = new LoginData("username4", "userpassword4");
		
		Packet response;
		synchronized (objectLock) {
			try {
				isResponseRequested = true;
				sendPacket(new Packet(Packet.ACTION_LOGIN, loginData));
				objectLock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			response = requestedPacket;
		}
		
		System.out.println("Login packet was: " + response);
		characterId = response.getCharacterId();
		
		if (response.getAction() != Packet.ACTION_LOGIN) {
			System.out.println("Not expected response");
			return;
		}
		if ((Byte)response.getData() == Packet.RESPONSE_LOGIN_INCORRECT) {
			System.out.println("User or password doesn't match");
			return;
		}
		if ((Byte)response.getData() == Packet.RESPONSE_LOGIN_ALREADY) {
			System.out.println("User is already logged in");
			return;
		}
		System.out.println("User correctly logged in con id " + characterId);
		
		synchronized (objectLock) {
			try {
				isResponseRequested = true;
				sendPacket(new Packet(Packet.ACTION_CHAR_LOCATION, null, characterId));
				objectLock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {}
		
	}
}
