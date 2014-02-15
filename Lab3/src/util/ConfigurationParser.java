package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import ds.model.Constants;
import ds.model.Group;

public class ConfigurationParser
{
	private static long lastModified;
	
	public static int parseConfigurationFile(String fileName, String localName, Node localNode, List<Node> nodeList, HashMap<String, Group> groupList, List<Rule> sendRuleList, List<Rule> receiveRuleList)
	{
		int localIndex = -1;
		try
		{
			File file = new File(fileName);
			lastModified = file.lastModified();
			InputStream input = new FileInputStream(file);
		    Yaml yaml = new Yaml();
		    Object data = yaml.load(input);
		    
		    LinkedHashMap<String,Object> level1Map = (LinkedHashMap)data;
		    
		  //  System.out.println(level1Map.get("timer").toString());
		    
		     String timeStampType = (String)(level1Map.get("timer").toString());
		     if(timeStampType.equals("logical"))
		     {
		    	 MessagePasser.tsType = Constants.TimeStampType.LOGICAL;
		     }
		     else
		     {
		    	 MessagePasser.tsType = Constants.TimeStampType.VECTOR;
		     }
		    
		    
		    ArrayList<HashMap> nodes = (ArrayList)level1Map.get("configuration");
		    ArrayList<HashMap> sendRules = (ArrayList)level1Map.get("sendRules");
		    ArrayList<HashMap> receiveRules = (ArrayList)level1Map.get("receiveRules");
		    ArrayList<HashMap> groups = (ArrayList)level1Map.get("groups");
		    
		    List<Node> localNodeList = new LinkedList<Node>();
	
		    Node node ;
		    
		    int index = -1;
		    for(HashMap<String,Object> nodeProps : nodes)
	    	{
		    	index++;
		    	node = new Node(nodeProps.get("name").toString(),nodeProps.get("ip").toString(),(Integer)nodeProps.get("port"));
		    	if(nodeProps.get("name").toString().equals(localName))
		    	{
		    		//localNode = node;
		    		localNode.setName(node.getName());
		    		localNode.setIp(node.getIp());
		    		localNode.setPort(node.getPort());
		    		localIndex = index;
		    		localNodeList.add(node);
		    	}
		    	else
		    	{
		    		nodeList.add(node);
		    		localNodeList.add(node);
		    	}
	    	}
		    
		    Rule rule;
		    for(HashMap<String,Object> sendRule : sendRules)
	    	{
		    	String src = sendRule.get("src") != null ? sendRule.get("src").toString() : null ; 
		    	String dest = sendRule.get("dest") != null ? sendRule.get("dest").toString() : null ; 
		    	String kind = sendRule.get("kind") != null ? sendRule.get("kind").toString() : null ;
		    	String action = sendRule.get("action") != null ? sendRule.get("action").toString() : null ;
		    	int seqNum = sendRule.get("seqNum") != null ? (Integer)sendRule.get("seqNum") : -1 ;
		    	Boolean dup = sendRule.get("duplicate")!=null ? new Boolean((boolean)Boolean.valueOf(sendRule.get("duplicate").toString())) : null;
	
				rule = new Rule(src,dest,kind,seqNum,dup,action);
				sendRuleList.add(rule);
	    	}
	
		    for(HashMap<String,Object> receiveRule : receiveRules)
	    	{
		    	String src = receiveRule.get("src") != null ? receiveRule.get("src").toString() : null ; 
		    	String dest = receiveRule.get("dest") != null ? receiveRule.get("dest").toString() : null ; 
		    	String kind = receiveRule.get("kind") != null ? receiveRule.get("kind").toString() : null ;
		    	String action = receiveRule.get("action") != null ? receiveRule.get("action").toString() : null ;
		    	int seqNum = receiveRule.get("seqNum") != null ? (Integer)receiveRule.get("seqNum") : -1 ;
		    	Boolean dup = receiveRule.get("duplicate")!=null ? new Boolean((boolean)Boolean.valueOf(receiveRule.get("duplicate").toString())) : null;
	
				rule = new Rule(src,dest,kind,seqNum,dup,action);
	    		receiveRuleList.add(rule);
	    	}
		    
		    /* Groups parsing and list generation */
		    Group group;
		    for(HashMap<String,Object> groupProps : groups)
	    	{
		    	group = new Group(groupProps.get("name").toString());
		    	ArrayList<String> members = new ArrayList<String>();
		    	members = (ArrayList<String>)groupProps.get("members");
		    	for (Node n1 : localNodeList)
		    	{
		    		if (members.contains(n1.getName()))
		    		{
		    			group.addToGroup(n1);
		    		}
		    	}
		    	group.createGroupTimeStamp(group.getMemberArray().size());
		    	groupList.put(group.getName(), group);	
	    	}
		    
	
		    
	   
	    
			input.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return localIndex;
	}
	
	public static void checkAndUpdateRulesIfChanged(String fileName, List<Rule> sendRuleList, List<Rule> receiveRuleList)
	{
		try
		{
			File file = new File(fileName);
			long modified = file.lastModified();
			
			if(modified<=lastModified)
			{
				return;
			}
			
			InputStream input = new FileInputStream(file);
		    Yaml yaml = new Yaml();
		    Object data = yaml.load(input);
		    
		    LinkedHashMap<String,ArrayList> level1Map = (LinkedHashMap)data;
		    
		   
		    ArrayList<HashMap> sendRules = (ArrayList)level1Map.get("sendRules");
		    ArrayList<HashMap> receiveRules = (ArrayList)level1Map.get("receiveRules");
		    
		    sendRuleList.clear();
		    receiveRuleList.clear();
		    
		    Rule rule;
		    for(HashMap<String,Object> sendRule : sendRules)
	    	{
		    	String src = sendRule.get("src") != null ? sendRule.get("src").toString() : null ; 
		    	String dest = sendRule.get("dest") != null ? sendRule.get("dest").toString() : null ; 
		    	String kind = sendRule.get("kind") != null ? sendRule.get("kind").toString() : null ;
		    	String action = sendRule.get("action") != null ? sendRule.get("action").toString() : null ;
		    	int seqNum = sendRule.get("seqNum") != null ? (Integer)sendRule.get("seqNum") : -1 ;
		    	Boolean dup = sendRule.get("duplicate")!=null ? new Boolean((boolean)Boolean.valueOf(sendRule.get("duplicate").toString())) : null;
	
				rule = new Rule(src,dest,kind,seqNum,dup,action);
				sendRuleList.add(rule);
	    	}
	
		    for(HashMap<String,Object> receiveRule : receiveRules)
	    	{
		    	String src = receiveRule.get("src") != null ? receiveRule.get("src").toString() : null ; 
		    	String dest = receiveRule.get("dest") != null ? receiveRule.get("dest").toString() : null ; 
		    	String kind = receiveRule.get("kind") != null ? receiveRule.get("kind").toString() : null ;
		    	String action = receiveRule.get("action") != null ? receiveRule.get("action").toString() : null ;
		    	int seqNum = receiveRule.get("seqNum") != null ? (Integer)receiveRule.get("seqNum") : -1 ;
		    	Boolean dup = receiveRule.get("duplicate")!=null ? new Boolean((boolean)Boolean.valueOf(receiveRule.get("duplicate").toString())) : null;
	
				rule = new Rule(src,dest,kind,seqNum,dup,action);
	    		receiveRuleList.add(rule);
	    	}
	   
	    
			input.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
