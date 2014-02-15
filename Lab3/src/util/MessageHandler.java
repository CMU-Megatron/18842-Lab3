package util;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

import ds.model.Message;
import ds.model.TimeStampedMessage;

public class MessageHandler extends Thread{
	private Socket socket;
	
	public MessageHandler(Socket socket){
		this.socket = socket;
	}
	
	public void run(){
		MessagePasser msgPasser = null;
		String src = null;
		try{
			msgPasser = MessagePasser.getInstance();
			while(true){
				ObjectInputStream ois = new ObjectInputStream(this.socket.getInputStream());
				TimeStampedMessage msg = (TimeStampedMessage)ois.readObject();
				src = msg.getSrc();
				msgPasser.addSocketToMap(src, this.socket);
				msgPasser.addMsgToBuf(msg);
			}
		}catch(Exception e){
			//e.printStackTrace();
			msgPasser.removeSocketFromMap(src);
		}
	}
}
