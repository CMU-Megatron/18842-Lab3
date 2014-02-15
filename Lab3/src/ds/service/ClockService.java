package ds.service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ds.model.TimeStamp;

public abstract class ClockService {
	
	protected TimeStamp timeStamp;
	protected Lock timeLock;
	
	public ClockService()
	{
		timeLock = new ReentrantLock();
	}
	
	
	public TimeStamp getCurrentTime()
	{
		return this.timeStamp;
	}
	
	public abstract TimeStamp updateOnSend();
	
	public abstract TimeStamp updateOnRecv(TimeStamp recvTs);
	
	public abstract void printTimeStamp();

}
