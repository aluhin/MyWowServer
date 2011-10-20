package com.amg.mywow.server;

import java.util.ArrayList;
import java.util.List;

import com.amg.mywow.common.Packet;

// TODO: We may want to change this so we can notify this thread when there is a new object to distribute
public class ActionDistributor implements Runnable{

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
						myWowHandler.sendPacket(packet);
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
