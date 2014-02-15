package util;

public class Rule 
{
	private String src;
	private String dest;
	private String kind;
	private int seqNum;
	private Boolean isDuplicate;
	
	private String action;

	public Rule(String src, String dest, String kind, int seqNum, Boolean isDuplicate, String action)
	{
		super();
		this.src = src;
		this.dest = dest;
		this.kind = kind;
		this.seqNum = seqNum;
		this.isDuplicate = isDuplicate;
		this.action = action;
	}

	public String getSrc()
	{
		return src;
	}

	public String getDest()
	{
		return dest;
	}

	public String getKind()
	{
		return kind;
	}

	public int getSeqNum()
	{
		return seqNum;
	}

	public Boolean isDuplicate()
	{
		return isDuplicate;
	}

	public String getAction()
	{
		return action;
	}
	
}
