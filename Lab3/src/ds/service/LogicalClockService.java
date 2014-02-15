package ds.service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ds.model.LogicalTimeStamp;
import ds.model.TimeStamp;

public class LogicalClockService extends ClockService
{
	private Lock csLock;
	public LogicalClockService()
	{
		this.timeStamp = new LogicalTimeStamp();
		this.csLock = new ReentrantLock();
	}
	
	public TimeStamp updateOnSend()
	{
		this.csLock.lock();
		((LogicalTimeStamp)this.timeStamp).incrementTime();
		TimeStamp ts = new LogicalTimeStamp(this.getCurrentTime());
		this.csLock.unlock();
		return ts;
	}
	
	public TimeStamp updateOnRecv(TimeStamp rcvTime)
	{
		LogicalTimeStamp ltp = (LogicalTimeStamp)rcvTime;
		this.csLock.lock();
		int timeToSet = Math.max(ltp.getTime(), ((LogicalTimeStamp)this.timeStamp).getTime()+1);
		((LogicalTimeStamp)this.timeStamp).setTime(timeToSet);
		TimeStamp ts = new LogicalTimeStamp(this.getCurrentTime());
		this.csLock.unlock();
		return ts;
	}
	
	public void printTimeStamp()
	{
		System.out.println("(Vector) ["+((LogicalTimeStamp)this.timeStamp).getTime()+"]");
	}
	
}
