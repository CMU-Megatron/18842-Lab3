package ds.model;

import java.util.ArrayList;
import java.util.HashMap;

import util.MessagePasser;
import util.Node;

public class HoldBackMessage implements Comparable<HoldBackMessage> {


	TimeStampedMessage ts;
	HashMap <String, Boolean> acknowledgement;

	public HoldBackMessage()
	{}

	public HoldBackMessage(TimeStampedMessage ts) {
		this.ts = new TimeStampedMessage(ts);
		this.acknowledgement = new HashMap<String, Boolean>();
	}

	public TimeStampedMessage getMessage()
	{
		return this.ts;
	}

	public void addAck(String nodeName)
	{
		if (acknowledgement.containsKey(nodeName)) {
			if (acknowledgement.get(nodeName) == true)
				return;
		}
		
		acknowledgement.put(nodeName, true);
	}

	public boolean isReadyToBeDelivered()
	{
		Group grp;

		try {
			MessagePasser.getInstance();
			grp = MessagePasser.groups.get(ts.getGroupName());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		for (Node node : grp.getMemberArray()) {
			if (!acknowledgement.containsKey(node.getName())) {
				return false;
			}
			
			if (acknowledgement.get(node.getName()) != true) {
				return false;
			}
		}

		return true;
	}

	public ArrayList<String> getRemainingAckList()
	{
		ArrayList<String> unicastList = new ArrayList<String>();
		Group grp;

		try {
			MessagePasser.getInstance();
			grp = MessagePasser.groups.get(ts.getGroupName());
		} catch (Exception e) {
			e.printStackTrace();
			return unicastList;
		}

		for (Node node : grp.getMemberArray()) {
			if (!acknowledgement.containsKey(node.getName())) {
				unicastList.add(node.getName());
			}
			else if (acknowledgement.get(node.getName()) != true) {
				unicastList.add(node.getName());
			}
		}

		return unicastList;
	}
	/*
	public int compareTo(HoldBackMessage hbMsg)
	{
		int returnVal = 0;
		VectorTimeStamp vts = hbMsg.getMessage().getGroupTimeStamp();
		for(int i = 0; i < this.ts.getGroupTimeStamp().getVectorLength(); i++)
		{
			if(this.ts.getGroupTimeStamp().getVector()[i] > vts.getVector()[i] )
			{
				if(returnVal==0)
				{
					returnVal = 1;
				}
				else if (returnVal<0)
				{
					return 0;
				}
			}
			else if(this.ts.getGroupTimeStamp().getVector()[i] < vts.getVector()[i] )
			{
				if(returnVal == 0)
				{
					returnVal = -1;
				}
				else if(returnVal>0)
				{
					return 0;
				}
			}

		}
		return returnVal;
	}
	 */
	@Override
	public int compareTo(HoldBackMessage hbm)
	{
		int returnVal = 0;
		VectorTimeStamp vts = (hbm.getMessage().getGroupTimeStamp());
		VectorTimeStamp current = (this.getMessage().getGroupTimeStamp());
		for(int i=0; i<vts.getVectorLength(); i++)
		{
			if(current.getVector()[i] > vts.getVector()[i] )
			{
				if(returnVal==0)
				{
					returnVal = 1;
				}
				else if (returnVal<0)
				{
					return 0;
				}
			}
			else if(current.getVector()[i] < vts.getVector()[i] )
			{
				if(returnVal == 0)
				{
					returnVal = -1;
				}
				else if(returnVal>0)
				{
					return 0;
				}
			}

		}
		return returnVal;
	}	
}
