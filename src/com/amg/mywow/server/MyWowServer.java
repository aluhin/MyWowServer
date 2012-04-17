package com.amg.mywow.server;

public class MyWowServer {
	
	private static final int DEFAULT_ACTION_PORT = 4444;

	private int actionPort;
	private HandlerManager handlerManager;
	private Thread handlerManagerThread;
	
	public MyWowServer(int actionPort) {
		this.actionPort = actionPort;
	}
	
	public void startServer() {
		handlerManager = new HandlerManager(actionPort);
		handlerManagerThread = new Thread(handlerManager);
		handlerManagerThread.start();
	}
	
	public void WaitUntilEnd() {
		try {
			handlerManagerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		int actionPort = DEFAULT_ACTION_PORT;
		
		if (args.length > 0) {
			actionPort = Integer.parseInt(args[0]);
		}
		
		MyWowServer myWowServer = new MyWowServer(actionPort);
		myWowServer.startServer();
		myWowServer.WaitUntilEnd();
	}
}
