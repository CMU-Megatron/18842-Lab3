package ds.model;

public class LogicalTimeStamp extends TimeStamp implements Comparable<LogicalTimeStamp>
{
	private int time;
	
	public LogicalTimeStamp()
	{
		this.time = 0;
	}
	
	public LogicalTimeStamp(TimeStamp currentTime) 
	{
		this.time = ((LogicalTimeStamp)currentTime).getTime();
	}

	public int getTime()
	{
		return this.time;
	}
	
	public int incrementTime()
	{
		this.time++;
		return this.time;
	}

	public void setTime(int timeToSet) 
	{	
		this.time = timeToSet;
	}
	
	@Override
	public int compareTo(LogicalTimeStamp arg0) {
		// Ascending Order Sorting
		int ret_val;
		ret_val = this.time - arg0.time;
		return ret_val;
	}

}
