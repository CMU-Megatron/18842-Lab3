package ds.service;

import ds.model.Constants;

public class FactoryService 
{

	public static Constants.TimeStampType clockServiceType = Constants.TimeStampType.NONE;
	public static ClockService clockService = null;
	public static MulticastService mcService = null;
	public static MutexService mService = null;
	static int numProcesses=0;
	static int currProcIndex = 0;
	
	public static void setClockServiceType(Constants.TimeStampType type,int num,int index)
	{
		clockServiceType = type;
		numProcesses = num;
		currProcIndex = index;
	}
	
	
	public static ClockService getClockService()
	{
		if(clockServiceType==Constants.TimeStampType.NONE)
			return null;
		
		if(clockService == null)
		{
			if(clockServiceType == Constants.TimeStampType.LOGICAL)
			{
				clockService = new LogicalClockService();
			}
			else
			{
				clockService = new VectorClockService(numProcesses,currProcIndex);
			}
		}
		return clockService;
	}
	
	public static synchronized MulticastService getMultiCastService()
	{
		if (mcService == null)
		{
			mcService = new MulticastService();
		}
		return mcService;
	}
	
	public static synchronized MutexService getMutexService()
	{
		getMultiCastService();
		
		if (mService == null)
		{
			mService = new MutexService();
		}
		return mService;
	}
}
