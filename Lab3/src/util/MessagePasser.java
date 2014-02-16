package util;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ds.model.Constants;
import ds.model.Constants.Kind;
import ds.model.Group;
import ds.model.TimeStamp;
import ds.model.TimeStampedMessage;
import ds.service.FactoryService;

public class MessagePasser{
	private static MessagePasser msgPasser;

	private String configFileName;
	private int seqNum;
	private Node localNode;
	private List<Node> nodes;
	private List<Rule> sendRules;
	private List<Rule> recvRules;
	private Queue<TimeStampedMessage> sendDelayBuf;
	private Lock sendBufLock;
	private Queue<TimeStampedMessage> recvDelayBuf;
	private Queue<TimeStampedMessage> recvBuf;
	private Lock recvBufLock;
	private Map<String, Socket> node2socket;
	private Lock node2socketLock;
	private Lock ruleLock;

	private int localIndex;
	public static Constants.TimeStampType tsType;
	public static HashMap<String, Group> groups;
	public static HashMap <String, String> votingList;
	public static String localName;

	public List<Node> getNodeList()
	{
		return this.nodes;
	}

	public MessagePasser(String configFileName, String localName){
		super();
		this.configFileName = configFileName;
		this.localName = localName;
		this.seqNum = 0;
		this.localNode = new Node();
		this.nodes = new LinkedList<Node>();
		groups = new HashMap<String, Group>();
		this.sendRules = new LinkedList<Rule>();
		this.recvRules = new LinkedList<Rule>();
		this.sendDelayBuf = new LinkedList<TimeStampedMessage>();
		this.sendBufLock = new ReentrantLock();
		this.recvDelayBuf = new LinkedList<TimeStampedMessage>();
		this.recvBuf = new LinkedList<TimeStampedMessage>();
		this.recvBufLock = new ReentrantLock();
		this.node2socket = new HashMap<String, Socket>();
		this.node2socketLock = new ReentrantLock();		
		this.ruleLock = new ReentrantLock();
		this.votingList = new HashMap<String, String>();

		try{
			this.localIndex = ConfigurationParser.parseConfigurationFile(configFileName, localName, localNode, this.nodes, groups, votingList, this.sendRules, this.recvRules);
		}catch(Exception e){
			e.printStackTrace();
		}

		FactoryService.setClockServiceType(tsType, this.nodes.size()+1, localIndex);

		SocketListener socketListener = new SocketListener(this.localNode);
		socketListener.start();
	}

	public static void createInstance(String configFileName, String localName){
		MessagePasser.msgPasser = new MessagePasser(configFileName, localName);
	}

	public static MessagePasser getInstance() throws Exception{
		if(MessagePasser.msgPasser == null){
			throw new Exception("Class not created at start of application");
		}
		return MessagePasser.msgPasser;
	}

	public void send(TimeStampedMessage msg)
	{
		TimeStamp ts = FactoryService.getClockService().updateOnSend();
		msg.setTimeStamp(ts);

		msg.setSrc(this.localName);
		msg.setSeqNum(this.seqNum++);
		String action = this.matchSendRule(msg);
		if(action == null){
			this.sendMsg(msg);
			this.sendBufLock.lock();
			try{
				this.clearSendDelayBuf();
			}finally{
				this.sendBufLock.unlock();
			}
		}else{
			if(action.equals(Constants.actionDrop)){
				return;
			}else if(action.equals(Constants.actionDuplicate)){
				TimeStampedMessage dupeMsg = new TimeStampedMessage(msg);
				dupeMsg.setTimeStamp(ts);
				dupeMsg.setDupe(true);
				this.sendMsg(msg);
				this.sendBufLock.lock();
				try{
					this.clearSendDelayBuf();
				}finally{
					this.sendBufLock.unlock();
				}
				this.sendMsg(dupeMsg);
			}else if(action.equals(Constants.actionDelay)){
				this.sendBufLock.lock();
				try{
					this.sendDelayBuf.add(msg);
				}finally{
					this.sendBufLock.unlock();
				}
			}
		}
	}

	public TimeStampedMessage receive(){
		if(!this.recvBuf.isEmpty()){
			return this.recvBuf.remove();
		}else{
			return null;
		}
	}

	private void sendMsg(TimeStampedMessage msg)
	{	
		String dest = msg.getDest();
		Socket socket = null;
		this.node2socketLock.lock();
		try{
			if(this.node2socket.containsKey(dest)){
				socket = this.node2socket.get(dest);
			}else{
				for(Node n : this.nodes){					
					if(n.getName().equals(dest)){
						socket = new Socket(n.getIp(), n.getPort());
						break;
					}
				}
				MessageHandler msgHandler = new MessageHandler(socket);
				msgHandler.start();
				this.addSocketToMap(dest, socket);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			this.node2socketLock.unlock();
		}
		try{
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(msg);
			oos.flush();
		}catch(Exception e){
			removeSocketFromMap(dest);
			e.printStackTrace();
			System.out.println("Failed to send message to destination,try again");
		}	

	}

	private void clearSendDelayBuf(){
		while(!this.sendDelayBuf.isEmpty()){
			this.sendMsg((TimeStampedMessage)this.sendDelayBuf.remove());
		}
	}

	public void addSocketToMap(String nodeName, Socket socket){
		this.node2socketLock.lock();
		try{
			if(!this.node2socket.containsKey(nodeName)){
				this.node2socket.put(nodeName, socket);
			}
		}finally{
			this.node2socketLock.unlock();
		}
	}

	public void removeSocketFromMap(String src){
		this.node2socketLock.lock();
		try{
			this.node2socket.remove(src);
		}finally{
			this.node2socketLock.unlock();
		}
	}

	public void addMsgToBuf(TimeStampedMessage msg)
	{	
		String action = this.matchRecvRule(msg);
		if(action == null){
			this.recvBufLock.lock();
			try{
				if (!FactoryService.getMultiCastService().handleMulticastService(msg))
				{
					this.addToRecvBuf(msg);
					this.clearRecvDelayBuf();
				}
			}finally{
				this.recvBufLock.unlock();
			}
		}else{
			if(action.equals(Constants.actionDrop)){
				return;
			}else if(action.equals(Constants.actionDuplicate)){
				this.recvBufLock.lock();
				try{
					TimeStampedMessage dupeMsg = new TimeStampedMessage(msg);
					dupeMsg.setTimeStamp( msg.getTimeStamp() );
					dupeMsg.setDupe(true);	
					this.addToRecvBuf(msg);
					this.clearRecvDelayBuf();
					this.addToRecvBuf(dupeMsg);
				}finally{
					this.recvBufLock.unlock();
				}
			}else if(action.equals(Constants.actionDelay)){
				this.recvBufLock.lock();
				try{
					this.recvDelayBuf.add(msg);
				}finally{
					this.recvBufLock.unlock();
				}
			}
		}
	}

	private boolean equalsIfNotNull(String str1,String str2)
	{
		if(str1==null || str2==null)
		{
			return true;
		}
		else
		{
			return str1.equals(str2);
		}
	}

	private String matchSendRule(TimeStampedMessage msg) {
		this.ruleLock.lock();
		ConfigurationParser.checkAndUpdateRulesIfChanged(this.configFileName, this.sendRules, this.recvRules);
		for(Rule r : this.sendRules){
			if( equalsIfNotNull(msg.getSrc(),r.getSrc()) 
					&& equalsIfNotNull(msg.getDest(),r.getDest()) 
					&& equalsIfNotNull(msg.getKind(),r.getKind()) 
					&& ( r.getSeqNum() < 0 || msg.getSeqNum() == r.getSeqNum() )
					&& ( r.isDuplicate() == null ||  msg.isDupe() == r.isDuplicate()) )
			{
				this.ruleLock.unlock();
				return r.getAction();
			}
		}
		this.ruleLock.unlock();
		return null;
	}

	private String matchRecvRule(TimeStampedMessage msg) {
		this.ruleLock.lock();
		ConfigurationParser.checkAndUpdateRulesIfChanged(this.configFileName, this.sendRules, this.recvRules);
		for(Rule r : this.recvRules){
			if( equalsIfNotNull(msg.getSrc(),r.getSrc()) 
					&& equalsIfNotNull(msg.getDest(),r.getDest()) 
					&& equalsIfNotNull(msg.getKind(),r.getKind()) 
					&& ( r.getSeqNum() < 0 || msg.getSeqNum() == r.getSeqNum() )
					&& ( r.isDuplicate() == null ||  msg.isDupe() == r.isDuplicate()) )
			{
				this.ruleLock.unlock();
				return r.getAction();
			}
		}
		this.ruleLock.unlock();
		return null;
	}

	public void clearRecvDelayBuf(){
		while(!this.recvDelayBuf.isEmpty()){
			TimeStampedMessage delayedMsg = this.recvDelayBuf.remove();

			if (!FactoryService.getMultiCastService().handleMulticastService(delayedMsg))
				addToRecvBuf(delayedMsg);
		}
	}

	public void addToRecvBuf(TimeStampedMessage msg1)
	{
		TimeStampedMessage msg = new TimeStampedMessage(msg1);
		msg.setTimeStamp(FactoryService.getClockService().updateOnRecv(msg1.getTimeStamp()));
		this.recvBuf.add(msg);
	}

}
