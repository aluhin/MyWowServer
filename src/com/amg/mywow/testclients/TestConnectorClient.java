package com.amg.mywow.testclients;

import com.amg.mywow.common.LoginData;
import com.amg.mywow.common.MovementAction;
import com.amg.mywow.common.Packet;
import com.amg.mywow.common.Position;

public class TestConnectorClient extends ConnectorClient {
	
	// Used for testing purposed only
	private static final int NUMBER_OF_CLIENTS = 20;
	private static final int NUMBER_OF_SENTS = 10;
	private int numberOfMovementsReceived;
	private static int totalNumberOfMovementsReceived;
	private Packet[][] packetsReceived;
	
	private Packet waitingPacket; 
	private Object objectLock;
	private boolean isPacketRequested;
	private int characterId;
	
	public TestConnectorClient(String host, int port) {
		super(host, port, true);
		objectLock = new Object();
		packetsReceived = new Packet[NUMBER_OF_CLIENTS][NUMBER_OF_SENTS];
	}

	public static void main(String[] args) {
		TestConnectorClient[] clients = new TestConnectorClient[NUMBER_OF_CLIENTS];
		for (int i = 0; i < NUMBER_OF_CLIENTS; i++)
			clients[i] = new TestConnectorClient("localhost", 4444);
		for (int i = 0; i < NUMBER_OF_CLIENTS; i++)
			clients[i].connect();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void processPacket(Packet packet) {
		//System.out.println("processPacket(): " + packet);
		if (isPacketRequested) {
			isPacketRequested = false;
			synchronized (objectLock) {
				waitingPacket = packet;
				objectLock.notify();
				Thread.currentThread().setName("Character" + packet.getCharacterId());
				return;
			}
		}
		switch (packet.getAction()) {
		case Packet.ACTION_MOVEMENT :
			packetsReceived[packet.getCharacterId()][((MovementAction)packet.getData()).getPositions()[0].getX()] = packet;
			
			System.out.println(Thread.currentThread().getName() + " received: " + ++numberOfMovementsReceived + " " + packet);
			synchronized (TestConnectorClient.class) {
				System.out.println("Total received: " + ++totalNumberOfMovementsReceived);
			}
			//System.out.println("Movement arrived: " + packet);
			break;
		case Packet.ACTION_MOVEMENT_CONFIRMATION :
			//System.out.println("Movement confirmation arrived: " + packet + " --> "+ ((Byte)packet.getData() == Packet.RESPONSE_GENERIC_OK ? "VALIDO" : "NO VALIDO"));
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
		
		LoginData loginData = new LoginData("alberto", "temp123");
		sendPacket(new Packet(Packet.ACTION_MOVEMENT, loginData));
		
		Packet response;
		synchronized (objectLock) {
			try {
				isPacketRequested = true;
				objectLock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			response = waitingPacket;
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
		
		for (int i = 0; i < NUMBER_OF_SENTS; i++) {
			MovementAction movementAction = new MovementAction(new Position[] {new Position(i, i + 1), new Position (i + 10, i + 100)});
			sendPacket(new Packet(Packet.ACTION_MOVEMENT, movementAction, characterId));
//			try {
//				Thread.sleep((int)(Math.random() * 5000));
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Character " + characterId + " got:");
		for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
			System.out.println("From Char " + i + " :");
			for (int a = 0; a < NUMBER_OF_SENTS; a++) {
				System.out.println(packetsReceived[i][a]);
			}
		}

		while(true) {
			try {
				Thread.sleep(10000000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
