package ds.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import util.MessagePasser;
import util.MulticastDelivery;
import util.Node;
import ds.model.Constants.Kind;
import ds.model.*;

public class MulticastService {
	MessagePasser msgPasser = null;
	HashMap<String, ArrayList<HoldBackMessage>> holdbackMap = null;
	HashMap<String, ArrayList<HoldBackMessage>> deliverMap = null;
	public static ReentrantLock hbMapLock = null;

	public MulticastService() {
		try {
			msgPasser = MessagePasser.getInstance();
			holdbackMap = new HashMap<String, ArrayList<HoldBackMessage>>();
			deliverMap = new HashMap<String, ArrayList<HoldBackMessage>>();
			hbMapLock = new ReentrantLock();

			for (String grpName : msgPasser.groups.keySet()) {				
				ArrayList<HoldBackMessage> grpHoldbackQueue = new ArrayList<HoldBackMessage>();
				ArrayList<HoldBackMessage> deliverQueue = new ArrayList<HoldBackMessage>();
				holdbackMap.put(grpName, grpHoldbackQueue);
				deliverMap.put(grpName, deliverQueue);
			}

			for (String groupName : msgPasser.groups.keySet()) {
				Group grp = msgPasser.groups.get(groupName);

				if (grp.isMember(msgPasser.localName))
				{
					MulticastDelivery deliver = new MulticastDelivery(groupName);
					deliver.start();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void multicast(TimeStampedMessage mmsg) {
		Group currGrp = msgPasser.groups.get(mmsg.getGroupName());

		if (mmsg.getOrigSrc().equals(mmsg.getSrc())) {
			ArrayList<HoldBackMessage> hbQueue = holdbackMap.get(mmsg.getGroupName());
			HoldBackMessage hbMsb = new HoldBackMessage(mmsg);
			hbMsb.addAck(msgPasser.localName);
			hbMsb.getMessage().setTimeStamp(FactoryService.getClockService().getCurrentTime());
			hbQueue.add(hbMsb);
		}

		for (Node node : currGrp.getMemberArray()) {
			//TODO - Fix this issue
			if (node.getName().equals(msgPasser.localName)) {

				continue;
			}
			TimeStampedMessage temp = new TimeStampedMessage(mmsg);
			temp.setDest(node.getName());
			msgPasser.send(temp);
		}
	}

	public boolean checkDupDeliveredMessages(TimeStampedMessage msg, String groupName) {
		ArrayList<HoldBackMessage> deliverList = deliverMap.get(groupName);

		for (HoldBackMessage hmsg: deliverList)
		{
			boolean ret = false;
			ret = this.compareTS(msg.getGroupTimeStamp().getVector(), hmsg.getMessage().getGroupTimeStamp().getVector());
			if (msg.getOrigSrc().equals(hmsg.getMessage().getOrigSrc()) && ret &&	msg.getGroupName().equals(hmsg.getMessage().getGroupName()))
			{
				return true;
			}
		}

		/* Not found */
		return false;		
	}

	public void updateHoldBackQueue(TimeStampedMessage mmsg) {
		ArrayList<HoldBackMessage> hbQueue = holdbackMap.get(mmsg.getGroupName());
		Group selectedGroup = msgPasser.groups.get(mmsg.getGroupName());
		HoldBackMessage hmsg = null;

		/* Check if the message has already been delivered */
		if (!checkDupDeliveredMessages(mmsg, mmsg.getGroupName())) {
			/* If message is not already there, do this */
			hmsg = this.checkExists(mmsg);

			if (hmsg == null)
			{
				hmsg = new HoldBackMessage(mmsg);
				hmsg.addAck(mmsg.getSrc());
				hmsg.addAck(msgPasser.localName);

				/* Take the Lock and re-order Arraylist after adding into HBQ */
				hbQueue.add(hmsg);
				Collections.sort(hbQueue);
			}
			else
			{
				/* Else just decrement the counter for the message received */
				hmsg.addAck(mmsg.getSrc());		
			}	

			/* Deliver to Receive Buffer if counter is zero */
			if (hmsg.isReadyToBeDelivered() == true)
			{
				while (this.causalOrder(selectedGroup.getName()));
			}	
		}
	}

	public void receiveMulticast(TimeStampedMessage mmsg)  {

		updateHoldBackQueue(mmsg);

		/* Re-multicast if this is the original sent message */
		if (mmsg.getSrc().equals(mmsg.getOrigSrc()))
		{
			mmsg.setSrc(msgPasser.localName);
			this.multicast(mmsg);
		}

	}

	/* Check if a msg exists in the Arraylist */
	public HoldBackMessage checkExists(TimeStampedMessage msg)
	{
		ArrayList<HoldBackMessage> hbQueue = holdbackMap.get(msg.getGroupName());

		for (HoldBackMessage hmsg: hbQueue)
		{
			boolean ret = false;
			ret = this.compareTS(msg.getGroupTimeStamp().getVector(), hmsg.getMessage().getGroupTimeStamp().getVector());
			if (msg.getOrigSrc().equals(hmsg.getMessage().getOrigSrc()) && ret &&	msg.getGroupName().equals(hmsg.getMessage().getGroupName()))
			{
				return hmsg;
			}
		}
		/* Not found */
		return null;		
	}

	public boolean compareTS(int [] a1, int [] a2)
	{
		if (a1.length != a2.length)
		{
			return false;
		}

		for (int i=0; i<a1.length ; i++)
		{
			if (a1[i] != a2[i])
			{
				return false;
			}
		}
		return true;
	}

	public boolean checkIfOkToDeliver(HoldBackMessage hbMsg, ArrayList<HoldBackMessage> hbQueue) {
		for (HoldBackMessage holdBackMessage : hbQueue) {
			if (hbMsg.compareTo(holdBackMessage) == 1)
				return false;
		}

		return true;
	}

	/* Function to causal order to check whether
	 * any message from the Hold-Back queue can be inserted into
	 * the receive buffer.
	 */
	public boolean causalOrder(String groupName)
	{
		boolean delivered = false;
		ArrayList<HoldBackMessage> hbqueue = holdbackMap.get(groupName);
		Group selectedGroup = msgPasser.groups.get(groupName);
		VectorTimeStamp cmpTS;

		for (Iterator<HoldBackMessage> it = hbqueue.iterator(); it.hasNext(); ) 
		{
			HoldBackMessage hbm = it.next();
			int result = 0;
			cmpTS = (VectorTimeStamp)selectedGroup.getCurrentGroupTimeStamp();
			result = this.getTSDiff(cmpTS, hbm.getMessage().getGroupTimeStamp());

			//if (cmpTS.compareTo(hbm.getMessage().getGroupTimeStamp()) != 1)
			if (checkIfOkToDeliver(hbm, hbqueue))
			{			
				/* Add to receive buffer on satisfaction of these 2 conditions */
				if (hbm.isReadyToBeDelivered() && ((result <= 1) || cmpTS.compareTo(hbm.getMessage().getGroupTimeStamp()) == 0))
				{
					delivered = true;
					int index = hbqueue.indexOf(hbm);
					HoldBackMessage reqMsg = hbqueue.get(index);
					it.remove();
					TimeStampedMessage reqTs = reqMsg.getMessage();

					HoldBackMessage deliveredMsg = new HoldBackMessage(reqTs);
					ArrayList<HoldBackMessage> deliverList = deliverMap.get(groupName);
					deliverList.add(deliveredMsg);

					hbMapLock.unlock();

					/* Add to recv buffer only if no other messages before this */
					if (reqTs.getData().equals(Kind.REQUEST.toString()))
						FactoryService.getMutexService().receiveMutexRequest(reqTs);
					else if (reqTs.getData().equals(Kind.RELEASE.toString()))
						FactoryService.getMutexService().receiveMutexRelease(reqTs);
					else
						msgPasser.addToRecvBuf(reqTs);

					/* Remove the messages in the delay buffer */
					msgPasser.clearRecvDelayBuf();

					/* Update the TimeStamp after putting in recv buffer */ 
					selectedGroup.updateGroupTSOnRecv(hbm.getMessage().getGroupTimeStamp(), msgPasser.localName);

					hbMapLock.lock();				
				}
			}
		}

		return delivered;
	}

	/* Get TimeStamp difference between 2 Vector TimeStamps */
	public int getTSDiff(VectorTimeStamp t1, VectorTimeStamp t2)
	{
		int result = 0;

		for (int i=0; i< t1.getVectorLength(); i++)
		{
			result = result + Math.abs(t1.getVector()[i] - t2.getVector()[i]);
		}

		return result;		
	}

	public void sendUnicast(String destination, TimeStampedMessage msg)
	{
		TimeStampedMessage uniMsg = new TimeStampedMessage(msg);

		uniMsg.setSrc(msgPasser.localName);
		uniMsg.setDest(destination);
		uniMsg.setKind(Kind.UNICAST.toString());
		msgPasser.send(uniMsg);
	}

	public void receiveUnicast(TimeStampedMessage msg) {
		updateHoldBackQueue(msg);
		TimeStampedMessage newMsg = new TimeStampedMessage(msg);
		String destination = msg.getSrc();

		newMsg.setSrc(msgPasser.localName);
		newMsg.setKind(Kind.ACK.toString());
		newMsg.setDest(destination);

		msgPasser.send(newMsg);
	}

	public void receiveAck(TimeStampedMessage msg) {
		updateHoldBackQueue(msg);
	}

	public HashMap<String, ArrayList<HoldBackMessage>> getHoldbackMap() {
		return holdbackMap;
	}

	public boolean handleMulticastService(TimeStampedMessage msg) {
		if (msg.getKind().equals(Kind.MULTICAST.toString())) {
			hbMapLock.lock();
			receiveMulticast(msg);
			hbMapLock.unlock();
			return true;
		}
		else if (msg.getKind().equals(Kind.UNICAST.toString())) {
			hbMapLock.lock();
			receiveUnicast(msg);
			hbMapLock.unlock();
			return true;
		}
		else if (msg.getKind().equals(Kind.ACK.toString())) {
			hbMapLock.lock();
			receiveAck(msg);
			hbMapLock.unlock();
			return true;
		}
		else if (msg.getData().equals(Kind.VOTE.toString())) {
			FactoryService.getMutexService().receiveVote(msg);
			return true;
		}
		return false;
	}
}
