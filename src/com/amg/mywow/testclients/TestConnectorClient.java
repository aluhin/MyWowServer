package com.amg.mywow.testclients;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import com.amg.mywow.common.LoginData;
import com.amg.mywow.common.MovementAction;
import com.amg.mywow.common.Packet;
import com.amg.mywow.common.Position;

public class TestConnectorClient extends ConnectorClient {
	
	// Used for testing purposed only
	private static final int NUMBER_OF_CLIENTS = 100;
	private static final int NUMBER_OF_SENTS = 100;
	private static final int MAX_SLEEP_BETWEEN_ACTIONS = 500;
	//private static final int MAX_SLEEP_BETWEEN_ACTIONS = 10;
	private int numberOfMovementsReceived;
	private static int totalNumberOfMovementsReceived;
	private static int numberOfFinishedThreads;
	private Packet[][] packetsReceived;
	
	private Object objectLock;
	private boolean isResponseRequested;
	private Packet requestedPacket; 
	private int characterId;
	private Position characterPosition;
	private Map<Integer, Position> enemies;
	
	public TestConnectorClient(String host, int port) {
		super(host, port, true);
		objectLock = new Object();
		packetsReceived = new Packet[NUMBER_OF_CLIENTS][NUMBER_OF_SENTS];
		enemies = new Hashtable<Integer, Position>();
	}

	public static void main(String[] args) throws InterruptedException {
		TestConnectorClient[] clients = new TestConnectorClient[NUMBER_OF_CLIENTS];
		for (int i = 0; i < NUMBER_OF_CLIENTS; i++)
			clients[i] = new TestConnectorClient("localhost", 4444);
		for (int i = 0; i < NUMBER_OF_CLIENTS; i++)
			clients[i].connect();
//			try {
//				Thread.sleep(2000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		
		long beforeTime = new Date().getTime();
		
		boolean shouldContinueWaiting = true; 
		do {
			Thread.sleep(200);
			synchronized (TestConnectorClient.class) {
				shouldContinueWaiting = numberOfFinishedThreads != NUMBER_OF_CLIENTS;
			}
		} while (shouldContinueWaiting);
		
		long afterTime = new Date().getTime();
		long estimatedTime = afterTime - beforeTime;
		System.out.println("Estimated Time: " + estimatedTime);
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
				//System.out.println("Character " + characterId + " starting position is: " + characterPosition);
				synchronized (objectLock) {
					objectLock.notify();
				}
			}
			else { //TODO: Should we check the case !isResponseRequested && packet.getCharacterId() != characterId ? it should never happen
				enemies.put(packet.getCharacterId(), (Position)packet.getData());
			}
			break;
		case Packet.ACTION_MOVEMENT :
			packetsReceived[packet.getCharacterId()-1][((MovementAction)packet.getData()).getPositions()[1].getX() - 1] = packet;
			
			synchronized (objectLock) {
				numberOfMovementsReceived++;
			}
			
			//System.out.println("Character " + characterId + " received movement: " + numberOfMovementsReceived + " " + packet);
			synchronized (TestConnectorClient.class) {
				totalNumberOfMovementsReceived++;
				//System.out.println("Total received: " + totalNumberOfMovementsReceived);
			}
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
		
		LoginData loginData = new LoginData("username", "userpassword");
		
		Packet response;
		synchronized (objectLock) {
			try {
				isResponseRequested = true;
				sendPacket(new Packet(Packet.ACTION_MOVEMENT, loginData));
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
		
		for (int i = 0; i < NUMBER_OF_SENTS; i++) {
			MovementAction movementAction = new MovementAction(new Position[] {new Position(characterPosition.getX(), characterPosition.getY()), new Position (i + 1, i + 1)});
			sendPacket(new Packet(Packet.ACTION_MOVEMENT, movementAction, characterId));
			try {
				Thread.sleep((int)(Math.random() * MAX_SLEEP_BETWEEN_ACTIONS));
				//Thread.sleep(MAX_SLEEP_BETWEEN_ACTIONS/2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		boolean shouldContinueWaiting = true; 
		int lastNumberReceived;
		do {
			synchronized (objectLock) {
				lastNumberReceived = numberOfMovementsReceived;
			}
			try {
				Thread.sleep(MAX_SLEEP_BETWEEN_ACTIONS * 50);
				//Thread.sleep(25000);
			} catch (InterruptedException e) {}
			synchronized (objectLock) {
				System.out.println("Thread " + characterId + " waiting " + lastNumberReceived + "!=" + numberOfMovementsReceived + " total " + totalNumberOfMovementsReceived);
				shouldContinueWaiting = lastNumberReceived == 0 || lastNumberReceived != numberOfMovementsReceived;
			}
		}
		while (shouldContinueWaiting);

//		try {
//			Thread.sleep(20000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
			
		synchronized (TestConnectorClient.class) {
			System.out.println("-------------------------------");
			
			System.out.println("Character " + characterId + " got the following movements:");
//			for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
//				System.out.println("From Char " + i + " :");
//				for (int a = 0; a < NUMBER_OF_SENTS; a++) {
//					System.out.println(packetsReceived[i][a]);
//				}
//			}
			
			System.out.println("Character " + characterId + " detected the following enemies: " + enemies.size());
//			for (Entry<Integer, Position> enemy : enemies.entrySet()) {
//				System.out.println("EnemyId: " + enemy.getKey() + " position: " + enemy.getValue());
//			}
			
			numberOfFinishedThreads++;
			System.out.println("numberOfFinishedThreads: " + numberOfFinishedThreads);
		}
	}
}
