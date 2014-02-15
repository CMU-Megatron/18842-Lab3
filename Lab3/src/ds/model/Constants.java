package ds.model;
public class Constants{
	public static final String actionDrop = "drop";
	public static final String actionDuplicate = "duplicate";
	public static final String actionDelay = "delay";
	
	public static enum TimeStampType {LOGICAL,VECTOR,NONE}; 
	public static enum Kind {ACK, TIMESTAMP, UNICAST, MULTICAST, REQUEST, INQUIRE, RELINQUISH, RELEASE, VOTE};
	public static enum MutexState {HELD, RELEASED, WANTED};
}
