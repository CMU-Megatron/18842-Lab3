package util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.w3c.dom.ls.LSLoadEvent;

import ds.model.Constants;
import ds.model.LogicalTimeStamp;
import ds.model.Message;
import ds.model.TimeStamp;
import ds.model.TimeStampedMessage;
import ds.model.VectorTimeStamp;


class Logger 
{
	private static ArrayList<TimeStampedMessage> msgArray = new ArrayList<TimeStampedMessage>();
	private static HashMap<String, ArrayList<TimeStampedMessage>> logicalArray = new HashMap<String, ArrayList<TimeStampedMessage>>();
	public static Lock logLock = new ReentrantLock();
	
	public static ArrayList<TimeStampedMessage> getArray() {
		return msgArray;
	}
	
	public 	static HashMap<String, ArrayList<TimeStampedMessage>> getHashMap()
	{
		return logicalArray;
	}
	public static  void addToHashMap(String key, TimeStampedMessage tmsg)
	{
		logicalArray.get(key).add(tmsg);
	}
	
	public static  void putInHashMap(String key, TimeStampedMessage tmsg)
	{
		ArrayList<TimeStampedMessage> arList = new ArrayList<TimeStampedMessage>();
		logicalArray.put(key, arList);
		logicalArray.get(key).add(tmsg);
	}
	
	public static void addToArray(TimeStampedMessage tmsg) {
		
		msgArray.add(tmsg);
	}
	
	public static void main(String args[]) throws Exception
	{
		if(args.length != 2)
		{
			System.out.println("Usage : Please pass the Config File Name and Local Name as arguments");
			System.exit(-1);
		}
		else
		{
			MessagePasser.createInstance(args[0], args[1]);
		}
				
		MessagePasser msgPasser = MessagePasser.getInstance();
		
		List<Node> nodes = msgPasser.getNodeList();
		Reader r = new Reader();
		r.start();
		
		System.out.println("Welcome to the LOGGER!");
		System.out.println("---------------------");
		Scanner reader = new Scanner(System.in);
		while(true)
		{
			if (MessagePasser.tsType == Constants.TimeStampType.VECTOR)
			{
				System.out.println();
				System.out.println();
				System.out.println("Please select one of the following options");
				System.out.println("1> Display all events");
				System.out.println("2> Info about single event");
				System.out.println("3> Info about all events");
				System.out.println("4> Discard Logs");
				System.out.println("5> Exit");
			
				int option = 0;
				while( option!=1 && option!=2 && option!=3 && option!=4)
				{
					System.out.println();
					System.out.println();
					System.out.print("Select Option(1 or 2 or 3 or 4) : ");
					option = reader.nextInt();
				}
				reader.nextLine();
				System.out.println();
			
				switch(option)
				{
					case 1:
					{
						if (msgArray.size() == 0)
						{
							System.out.println();
							System.out.println();
							System.out.println("No events to display");
						}
						else
						{
							System.out.println();
							System.out.println();
							System.out.println("List of events");
							int i=0;
							for(TimeStampedMessage msg: msgArray)
							{
								i++;
								VectorTimeStamp vin = (VectorTimeStamp)msg.getTimeStamp();
								System.out.println(i+"> Src: "+msg.getSrc()+", Dst: "+msg.getDest()+", Seq No:"+msg.getSeqNum()+", Kind: "+msg.getKind()+", Duplicate:"+msg.isDupe()+", Message: "+msg.getData().toString()+", TimeStamp: "+ Arrays.toString(vin.getVector()));
							}
						}
					}
					break;
					case 2:
					{
						if (msgArray.size() == 0)
						{
							System.out.println();
							System.out.println();
							System.out.println("No events to display");
						}
						else
						{
							System.out.println();
							System.out.println();
							System.out.println("List of events");
							int i=0;
							for(TimeStampedMessage msg: msgArray)
							{
								i++;
								VectorTimeStamp vin = (VectorTimeStamp)msg.getTimeStamp();
								System.out.println(i+"> Src: "+msg.getSrc()+", Dst: "+msg.getDest()+", Seq No:"+msg.getSeqNum()+", Kind: "+msg.getKind()+", Duplicate:"+msg.isDupe()+", Message: "+msg.getData().toString()+", TimeStamp: "+ Arrays.toString(vin.getVector()));
							}
							option = 0;
							while(option<1 || option>i)
							{
								System.out.print("Select an Event: ");
								option = reader.nextInt();
							}
							option = option - 1;
							reader.nextLine();
							System.out.println();
						
							printAllRelations(option);
							System.out.println();
						}
					}
					break;
					case 3:
					{
						for (int j=0; j<msgArray.size(); j++)
						{
							System.out.println("Printing details for Event "+(j+1));
							printAllRelations(j);
						}
					}
					break;
					case 4:
					{
						logLock.lock();
						msgArray.clear();
						logLock.unlock();
					}
					break;
					case 5:
					{
						System.exit(0);
					}
				}
			}
			else if (MessagePasser.tsType == Constants.TimeStampType.LOGICAL)
			{
				System.out.println();
				System.out.println();
				System.out.println("Please select one of the following options");
				System.out.println("1> View Log");
				System.out.println("2> Discard Logs");
				System.out.println("3> Exit");
							
				int option = 0;
				while( option!=1 && option!=2 && option!=3)
				{
					System.out.println();
					System.out.println();
					System.out.print("Select Option(1 or 2 or 3) : ");
					option = reader.nextInt();
				}
				reader.nextLine();
				
				switch(option)
				{
					case 1:
					{
						if (logicalArray.size() == 0)
						{
							System.out.println();
							System.out.println();
							System.out.println("No relations to display");
						}
						else
						{
							System.out.println();
							System.out.println();
							System.out.println("List of relations");
							for (Map.Entry<String, ArrayList<TimeStampedMessage>> entry : logicalArray.entrySet())
							{
								System.out.println();
								System.out.println();
								System.out.println("Printing all relations for src: "+entry.getKey());
								int count = 0;
								for(TimeStampedMessage ts:entry.getValue())
								{
									if (count != 0)
									{
										System.out.println("->");
									}
									LogicalTimeStamp vin = (LogicalTimeStamp)ts.getTimeStamp();
									System.out.println("(Src: "+ts.getSrc()+", Dst: "+ts.getDest()+ ", Seq Num"+ ts.getSeqNum()+", Kind:"+ ts.getKind()+ ", Duplicate:"+ ts.isDupe() + ", TimeStamp: "+vin.getTime()+ ", Data: "+ ts.getData()+")");
									count++;
									
								}
							}
							
						}
					}
					break;
					
					case 2:
					{
						logicalArray.clear();
					}
					break; 
					
					case 3:
					{
						System.exit(0);
					}
				}			
			}			
		}
	}
	
	public static void printCompare(int curIndex, int testIndex)
	{
		VectorTimeStamp curTS = (VectorTimeStamp)(msgArray.get(curIndex).getTimeStamp());
		TimeStamp testTS = (msgArray.get(testIndex).getTimeStamp());
		VectorTimeStamp vTestTS = (VectorTimeStamp)testTS;
		
		int ret_val = curTS.compareTo(testTS);
		switch(ret_val)
		{
		case -1:
			System.out.println("Event " + (curIndex+1) +" -> Event "+ (testIndex+1));
			break;
		case 0:
			System.out.println("Event " + (curIndex+1) +" || Event "+ (testIndex+1));
			break;
		case 1:
			System.out.println("Event " + (curIndex+1) +" <- Event "+ (testIndex+1));
			break;
		}
	}
	
	
	
	public static void printAllRelations(int option)
	{
		for(int j=0; j<msgArray.size(); j++)
		{
			if (j == option)
				continue;
			printCompare(option, j);							
		}
	}
}