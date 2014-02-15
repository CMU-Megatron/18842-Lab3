package util;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import ds.model.HoldBackMessage;
import ds.model.TimeStampedMessage;
import ds.service.FactoryService;
import ds.service.MulticastService;

public class MulticastDelivery extends Thread {
	private String groupName;
	
	public MulticastDelivery(String groupName){
		super();
		this.groupName = groupName;
	}
	
	public void run(){
		while(true) {
			MulticastService.hbMapLock.lock();
			HashMap<String, ArrayList<HoldBackMessage>> holdbackMap = FactoryService.getMultiCastService().getHoldbackMap();
			ArrayList<HoldBackMessage> hbQueue = holdbackMap.get(groupName);
			ArrayList<String> unicastList = new ArrayList<String>();
			
			//Collections.sort(hbQueue);
			
			for (HoldBackMessage holdBackMessage : hbQueue) {
				/*
				if (holdBackMessage.isReadyToBeDelivered())
				{
					if (checkIfOkToDeliver(holdBackMessage, hbQueue))
					{
						hbQueue.remove(holdBackMessage);
						try {
							MessagePasser.getInstance().addToRecvBuf(holdBackMessage.getMessage());
							continue;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				*/
				
				unicastList = holdBackMessage.getRemainingAckList();
				
				for (String nodeName : unicastList) {
					System.out.println("Retrying, sending to "+nodeName);
					FactoryService.mcService.sendUnicast(nodeName, holdBackMessage.getMessage());
				}
			}

			MulticastService.hbMapLock.unlock();
			try {
				sleep(Application.intervalTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
