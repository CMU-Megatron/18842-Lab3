package util;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import ds.model.Group;
import ds.model.TimeStamp;
import ds.model.TimeStampedMessage;
import ds.model.Constants.Kind;
import ds.model.VectorTimeStamp;
import ds.service.FactoryService;
import ds.service.MulticastService;
import ds.service.MutexService;


public class Application
{
	public static long intervalTime = 60000;
	public static MutexService mService = null;

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

		Scanner reader = new Scanner(System.in);

		System.out.println("Welcome to the COMMUNICATOR!");
		int option = 0;
		System.out.println("Please enter the interval time for ack timeout(sec) : ");
		intervalTime = reader.nextLong() * 1000;
		reader.nextLine();
		System.out.println();
		System.out.println("---------------------");

		MessagePasser msgPasser = MessagePasser.getInstance();
		MulticastService mcService = FactoryService.getMultiCastService();
		mService = FactoryService.getMutexService();

		List<Node> nodes = msgPasser.getNodeList();
		Node loggerNode=null;
		for(Node node:nodes)
		{
			if(node.getName().equals("logger"))
			{
				loggerNode = node;
				break;
			}
		}

		while(true)
		{
			System.out.println("Please select one of the following options");
			System.out.println("1> Send");
			System.out.println("2> Multicast");
			System.out.println("3> Receive");
			System.out.println("4> Print Current Time Stamp");
			System.out.println("5> Increment Local Time Stamp");
			System.out.println("6> Acquire Lock");
			System.out.println("7> ReleaseLock");
			System.out.println("8> Exit");

			option = 0;
			while( option!=1 && option!=2 && option!=3 && option!=4 && option!=5 && option!=6 && option!=7 && option!=8)
			{
				System.out.print("Select Option(1 or 2 or 3 or 4 or 5 or 6 or 7 or 8) : ");
				try {
					option = reader.nextInt();
				} catch(Exception e) {
					option = 0;
				}
			}
			reader.nextLine();
			System.out.println();

			switch(option)
			{
			case 1:
			{
				System.out.println("Please select a destination");
				int i=0;
				for(Node node:nodes)
				{
					if (node == loggerNode)
						continue;
					i++;
					System.out.println(i+"> "+node.getName());
				}
				option = 0;
				while(option<1 || option>i)
				{
					System.out.print("Select Destination: ");
					option = reader.nextInt();
				}
				reader.nextLine();
				System.out.println();

				String kind="";
				while(kind.toString().equals("") || kind.toString().equals(Kind.MULTICAST.toString()))
				{
					System.out.print("Kind of message: ");
					kind = reader.nextLine();
				}
				System.out.println();

				String shouldLog = "";
				while( !shouldLog.equals("y") && !shouldLog.equals("n") )
				{
					System.out.print("Should the message be logged (y for yes and n for no): ");
					shouldLog = reader.nextLine();
				}
				System.out.println();

				String message="";
				while(message.toString().equals(""))
				{
					System.out.print("Message text: ");
					message = reader.nextLine();
				}
				System.out.println();

				TimeStampedMessage msg1 = new TimeStampedMessage(nodes.get(option-1).getName(), kind, message);
				msgPasser.send(msg1);

				if(shouldLog.equals("y"))
				{
					Object logMessage = msg1;
					TimeStampedMessage logMsg = new TimeStampedMessage(loggerNode.getName(),"log", logMessage);
					msgPasser.send(logMsg);
				}

				System.out.println("Finished sending message.");
				System.out.println();
			}
			break;

			case 2:
			{
				int i = 0;
				option = 0;
				while(option<1 || option>msgPasser.groups.size())
				{
					System.out.println("Select a multicast group: ");
					i = 0;
					for(String groupName:msgPasser.groups.keySet())
					{						
						Group grp = msgPasser.groups.get(groupName);

						if (grp.isMember(msgPasser.localName))
						{
							i++;
							System.out.println();
							System.out.println(i+"> "+grp.getName());
							grp.printGroupInfo();
						}
						else
							continue;
					}
					option = reader.nextInt();
				}
				reader.nextLine();
				System.out.println();

				String message="";
				while(message.toString().equals(""))
				{
					System.out.print("Message text: ");
					message = reader.nextLine();
				}
				System.out.println();

				i = 0;
				String group = null;
				for(String groupName:msgPasser.groups.keySet())
				{
					Group grp = msgPasser.groups.get(groupName);

					if (grp.isMember(msgPasser.localName))
						i++;
					else
						continue;

					if (i == option)
					{
						group = groupName;
						break;
					}
				}

				System.out.println("Selected group: "+group);
				TimeStampedMessage mmsg = new TimeStampedMessage("", Kind.MULTICAST.toString(), message, group);
				Group selectedGroup = msgPasser.groups.get(group);

				/* Increment and attach timestamp */
				TimeStamp gts = selectedGroup.updateGroupTSOnSend(msgPasser.localName);
				VectorTimeStamp vts = (VectorTimeStamp)gts;
				//System.out.println("TimeStamp got : "+Arrays.toString(vts.getVector()));
				if (gts == null)
				{
					System.out.println("Wrong group or wrong node");
					continue;
				}

				/* Set the group TimeStamp */
				mmsg.setGroupTimeStamp(gts);

				mmsg.setSrc(msgPasser.localName);
				mmsg.setOrigSrc(msgPasser.localName);
				MulticastService.hbMapLock.lock();
				mcService.multicast(mmsg);
				MulticastService.hbMapLock.unlock();
				System.out.println();
			}
			break;

			case 3:
			{
				TimeStampedMessage msg1 = msgPasser.receive();

				if(msg1!=null)
				{
					System.out.println(msg1.getData());
					System.out.println();
					String shouldLog = "";
					while( !shouldLog.equals("y") && !shouldLog.equals("n") )
					{
						System.out.print("Should the received message be logged (y for yes and n for no): ");
						shouldLog = reader.nextLine();
					}
					System.out.println();
					if(shouldLog.equals("y"))
					{
						Object logMessage = msg1;
						TimeStampedMessage logMsg = new TimeStampedMessage(loggerNode.getName(),"log", logMessage);
						msgPasser.send(logMsg);
					}
				}
				else
					System.out.println("No pending messages");

				System.out.println();
			}

			break;

			case 4:
			{
				FactoryService.getClockService().printTimeStamp();
				System.out.println();
			}
			break;

			case 5:
			{
				TimeStamp ts = FactoryService.getClockService().updateOnSend();

				System.out.println("Updated Time Stamp:");
				FactoryService.getClockService().printTimeStamp();
				String shouldLog = "";
				while( !shouldLog.equals("y") && !shouldLog.equals("n") )
				{
					System.out.print("Should this event be logged (y for yes and n for no): ");
					shouldLog = reader.nextLine();
				}
				System.out.println();
				if(shouldLog.equals("y"))
				{
					//String logMessage = " received message from "+msg1.getSrc()+": "+msg1.getData().toString();
					String logMessage="";
					while( logMessage.equals(""))
					{
						System.out.print("Event to associate with this message: ");
						logMessage = reader.nextLine();
					}
					System.out.println();
					TimeStampedMessage tsm = new TimeStampedMessage("", "", logMessage);
					tsm.setTimeStamp(ts);

					TimeStampedMessage logMsg = new TimeStampedMessage(loggerNode.getName(),"log", tsm);
					msgPasser.send(logMsg);
				}
				System.out.println();
			}
			break;
			
			case 6:
			{
				mService.acquireMutex();
			}
			break;
			
			case 7:
			{
				mService.releaseMutex();
			}
			break;

			case 8:
			{
				System.out.println("==========================");
				System.out.println("  Have a Good Day!! :)");
				System.out.println("==========================");
				System.exit(0);
			}
			}

		}




	}
}
