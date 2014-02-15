package test;

import java.net.InetAddress;
import java.util.List;
import java.util.Scanner;
import util.MessagePasser;
import util.Node;
import ds.model.TimeStampedMessage;
import ds.model.Message;

public class TestAlice{
	public static void main(String[] args){
		try{
			System.out.println(InetAddress.getLocalHost().getHostAddress());
			MessagePasser.createInstance("test.conf", "alice");
			MessagePasser msgPasser = MessagePasser.getInstance();
			
			
			List<Node> nodes = msgPasser.getNodeList();
			
			System.out.println("Welcome to the COMMUNICATOR!");
			System.out.println("--------");
			Scanner reader = new Scanner(System.in);
			while(true)
			{
				System.out.println("Please select one of the following options");
				System.out.println("1> Send");
				System.out.println("2> Receive");
				
				int option = 0;
				while( option!=1 && option!=2 )
				{
					System.out.print("Select Option(1 or 2) : ");
					option = reader.nextInt();
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
						while(kind.toString().equals(""))
						{
							System.out.print("Kind of message: ");
							kind = reader.nextLine();
						}
						System.out.println();
						
						String message="";
						while(message.toString().equals(""))
						{
							System.out.print("Message text: ");
							message = reader.nextLine();
						}
						System.out.println();
						
						//send message
						TimeStampedMessage msg1 = new TimeStampedMessage(nodes.get(option-1).getName(), kind, message);
						msgPasser.send(msg1);
						
						System.out.println("Message succesfully sent!");
						System.out.println();
					}
					break;
						
					case 2:
					{
						Message msg1 = msgPasser.receive();
						
						if(msg1!=null)
							System.out.println(msg1.getData());
						else
							System.out.println("No pending messages");
						
						
						
						System.out.println();
					}
						
					break;
				}
				
			}
			
			
//			Thread.sleep(10000);
//			System.out.println("Start sending...");
//			Message msg = new Message("bob", "Test", "Hello");
//			msgPasser.send(msg);
//			//Thread.sleep(5000);
//			System.out.println("Send another...");
//			Message msg1 = new Message("bob", "Test", "World");
//			msgPasser.send(msg1);
			
		//	Thread.sleep(5000);
//			Thread.sleep(5000);
//			Message msg1;
//			for(int i=0;i<15;i++)
//			{
//				msg1 = new Message("bob", "Test", "a to b "+i);
//				msgPasser.send(msg1);
//			}
//			Thread.sleep(10000);
//			
//			for(int j=0;j<15;j++)
//			{
//				msg1 = msgPasser.receive();
//				System.out.println(msg1.getData());
//			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
