package ds.model;

import java.util.Arrays;

public class VectorTimeStamp extends TimeStamp {
	
	private int[] vectorData;
	
	
	public VectorTimeStamp(int num)
	{
		if (num != 0)
		{
			vectorData = new int[num];
		}
	}
	
	public VectorTimeStamp(TimeStamp ts)
	{
		VectorTimeStamp vts = (VectorTimeStamp)ts;
		int length = vts.getVectorLength();
		this.vectorData = new int[length];
		for(int i=0;i<length;i++)
		{
			this.vectorData[i] = vts.getVector()[i];
		}
	}
	
	public int[] getVector()
	{
		return this.vectorData;
	}
	
	public int getVectorLength()
	{
		return this.vectorData.length;
	}
	
	public void setVector(int index, int data)
	{
		this.vectorData[index] = data;
	}
	
	public int compareTo(TimeStamp timeStamp)
	{
		int returnVal = 0;
		VectorTimeStamp vts = (VectorTimeStamp)timeStamp;
		for(int i=0;i<this.getVectorLength();i++)
		{
			if(this.vectorData[i] > vts.getVector()[i] )
			{
				if(returnVal==0)
				{
					returnVal = 1;
				}
				else if (returnVal<0)
				{
					return 0;
				}
			}
			else if(this.vectorData[i] < vts.getVector()[i] )
			{
				if(returnVal == 0)
				{
					returnVal = -1;
				}
				else if(returnVal>0)
				{
					return 0;
				}
			}
			
		}
		return returnVal;
	}	
}