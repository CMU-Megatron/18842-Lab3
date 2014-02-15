package ds.model;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import util.*;



public class Group {
	private String name;
	private ArrayList<Node> members;
	private VectorTimeStamp groupTS;
	private Lock groupLock;
	
	public Group(String name)
	{
		this.name = name;
		this.members = new ArrayList<Node>();
		this.groupLock = new ReentrantLock();
	}
	
	public void createGroupTimeStamp(int num)
	{
		this.groupTS = new VectorTimeStamp(num);
	}
	
	public TimeStamp getCurrentGroupTimeStamp()
	{
		return new VectorTimeStamp(this.groupTS);
	}
	
	public void setCurrentGroupTimeStamp(VectorTimeStamp ts)
	{
		this.groupTS = ts;
	}
	
	public TimeStamp updateGroupTSOnSend(String name)
	{
		int index = this.getIndexOf(name);
		if (index == -1)
		{
			System.out.println("Arraylist doesn't contain element");
			return null;
		}
		this.groupLock.lock();
		this.groupTS.setVector(index, (this.groupTS.getVector()[index] + 1));
		TimeStamp ts = new VectorTimeStamp(this.groupTS);
		this.groupLock.unlock();
		return ts;
	}
	
	public void updateGroupTSOnRecv(TimeStamp recvTs, String name)
	{
		VectorTimeStamp recvTS = (VectorTimeStamp)recvTs;
		
		int index = this.getIndexOf(name);
		if (index == -1)
		{
			System.out.println("Arraylist doesn't contain element");
		}
		
		this.groupLock.lock();
		for (int i=0; i < this.groupTS.getVectorLength() ; i++)
		{
			if (this.groupTS.getVector()[i] < recvTS.getVector()[i])
			{
				this.groupTS.setVector(i, recvTS.getVector()[i]);
			}
		}
		this.groupLock.unlock();
	}	
	
	public void addToGroup(Node n)
	{
		this.members.add(n);
	}
	
	public int getIndexOf(String nodeName)
	{
		int i = 0;
		
		for (Node node : this.members) {
			if (node.getName().equals(nodeName))
				return i;
			i++;
		}
		return -1;
	}
	
	public ArrayList<Node> getMemberArray()
	{
		return this.members;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public void printGroupInfo()
	{
		System.out.println("Info for "+this.name);
		System.out.println("Members: ");
		for (Node a : this.members)
		{
			System.out.println(""+a.getName());
		}
	}
	
	public boolean isMember(String nodeName)
	{
		for (Node node : this.members) {
			if (node.getName().equals(nodeName))
				return true;
		}
		
		return false;
	}
	
	public int numOfMembers()
	{
		return this.members.size();
	}
}