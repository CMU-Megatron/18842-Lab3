package ds.service;
import java.util.Arrays;

import ds.model.*;

class VectorClockService extends ClockService {
	
	/* Indicator of the current Process index, only for Vector TimeStamp */
	private int index;
	
	
	/* Constructor for Vector Clock, args: process count from yaml and current process index */
	VectorClockService(int numberOfProcesses, int processIndex)
	{
		super();
		timeStamp = new VectorTimeStamp(numberOfProcesses);
		index = processIndex;
	}
	
	/* Returns current local TimeStamp for the process */
	public TimeStamp getCurrentTimeStamp()
	{
		return timeStamp;
	}
	
	/* Update Logical TimeStamp while receiving a message */
	public TimeStamp updateOnRecv(TimeStamp recvTs)
	{
		VectorTimeStamp timestamp = (VectorTimeStamp)timeStamp;
		VectorTimeStamp recvTS = (VectorTimeStamp)recvTs;
		
		this.timeLock.lock();
		for (int i=0; i < timestamp.getVectorLength() ; i++)
		{
			if (timestamp.getVector()[i] < recvTS.getVector()[i])
			{
				timestamp.setVector(i, recvTS.getVector()[i]);
			}
		}
		timestamp.setVector(index, (timestamp.getVector()[index] + 1));
		TimeStamp ts = new VectorTimeStamp(this.getCurrentTime());
		this.timeLock.unlock();
		return ts;
	}	
	
	/* Update Vector TimeStamp before sending the message */
	public TimeStamp updateOnSend()
	{
		this.timeLock.lock();
		VectorTimeStamp timestamp = (VectorTimeStamp)timeStamp;
		timestamp.setVector(index, (timestamp.getVector()[index] + 1));
		TimeStamp ts = new VectorTimeStamp(this.getCurrentTime());
		this.timeLock.unlock();
		return ts;
	}

	public void printTimeStamp()
	{
		System.out.println( "(Logical)"+Arrays.toString( ((VectorTimeStamp)this.timeStamp).getVector()) );
	}
}
