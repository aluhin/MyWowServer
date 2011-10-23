package com.amg.mywow.server;

import java.util.ArrayList;
import java.util.List;

import com.amg.mywow.common.MovementAction;
import com.amg.mywow.common.Packet;

// TODO: We may want to change this so we can notify this thread when there is a new object to distribute
public class ActionDistributor implements Runnable{

	private static final int MOVEMENT_RADIUS = 10;
	
	private HandlerManager handlerManager;
	
	ActionDistributor(HandlerManager handlerManager) {
		this.handlerManager = handlerManager;
	}
	
	@Override
	public void run() {
		List<Packet> actionsToDistribute;
		while(true) {
			// here we synchronize using double buffer
			synchronized (handlerManager.getActionsToDistribute()) {
				actionsToDistribute = handlerManager.getActionsToDistribute();
				handlerManager.setActionsToDistribute(new ArrayList<Packet>());
			}
			// here we block the whole handler table. Just blocking new handler additions or removals
			synchronized (handlerManager.getHandlersTable()) {
				for (MyWowHandler myWowHandler : handlerManager.getHandlersTable().values()) {
					for (Packet packet : actionsToDistribute)
					{
						switch (packet.getAction()) {
						case Packet.ACTION_MOVEMENT :
							if (myWowHandler.getCharacterId() != -1 
									&& myWowHandler.getCharacterId() != packet.getCharacterId() 
									&& myWowHandler.getCharacterPosition() != null) {
							
								final int rightX = myWowHandler.getCharacterPosition().getX() + MOVEMENT_RADIUS;
								final int leftX = myWowHandler.getCharacterPosition().getX() - MOVEMENT_RADIUS;
								final int topY = myWowHandler.getCharacterPosition().getY() + MOVEMENT_RADIUS;
								final int bottomY = myWowHandler.getCharacterPosition().getY() - MOVEMENT_RADIUS;
								
								final int movementX = ((MovementAction)packet.getData()).getPositions()[0].getX();
								final int movementY = ((MovementAction)packet.getData()).getPositions()[0].getY();
								
								if (movementX < rightX && movementX > leftX && movementY < topY && movementY > bottomY)
									myWowHandler.sendPacket(packet);
							}
							break;
							default :
								myWowHandler.sendPacket(packet);
						}
					}
				}
			}
			
			// Sleeping some time so we don't create almost any permanent overhead
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
