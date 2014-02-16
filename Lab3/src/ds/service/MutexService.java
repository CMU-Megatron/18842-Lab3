package ds.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import ds.model.Constants.MutexState;
import ds.model.Group;
import ds.model.TimeStamp;
import ds.model.TimeStampedMessage;
import ds.model.VectorTimeStamp;
import ds.model.Constants.Kind;
import util.MessagePasser;
import util.Node;

public class MutexService {
	MessagePasser msgPasser = null;
	MulticastService multicastService = null;
	String votingSet = null;
	volatile boolean votedMutex = false;
	volatile MutexState state = MutexState.RELEASED;
	volatile ArrayList<String> votes = new ArrayList<String>();
	ArrayList<TimeStampedMessage> reqQueue = new ArrayList<TimeStampedMessage>();
	ReentrantLock serviceLock = new ReentrantLock();

	public MutexService() {
		try {
			msgPasser = MessagePasser.getInstance();
			multicastService = FactoryService.getMultiCastService();
			configureVotingSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void configureVotingSet() {
		/*
		for (String groupName : msgPasser.groups.keySet()) {
			Group grp = msgPasser.groups.get(groupName);
			int grSize = grp.getMemberArray().size();

			if (grSize > 0) {
				System.out.println(groupName);
				System.out.println(((Node)grp.getMemberArray().get(grSize - 1)).getName());
				if (msgPasser.localName.equals(((Node)grp.getMemberArray().get(0)).getName())) {
					this.votingSet = groupName;
					return;
				}
			}
		}
		 */
		this.votingSet = msgPasser.votingList.get(msgPasser.localName);
		System.out.println("\nVoting set: " + votingSet);
	}

	public void acquireMutex() {
		if (this.state == MutexState.HELD) {
			System.out.println("\nWarning : Lock already held! Invalid lock() operation!");
			return;
		}
		System.out.println("\n[State: Requesting lock]");

		Group votingGroup = msgPasser.groups.get(this.votingSet);
		TimeStampedMessage lockReq = new TimeStampedMessage("", Kind.MULTICAST.toString(),
				Kind.REQUEST.toString(), votingGroup.getName());

		/* Increment and attach timestamp */
		TimeStamp gts = votingGroup.updateGroupTSOnSend(msgPasser.localName);
		VectorTimeStamp vts = (VectorTimeStamp)gts;
		//System.out.println("TimeStamp got : "+Arrays.toString(vts.getVector()));

		/* Set the group TimeStamp */
		lockReq.setGroupTimeStamp(gts);

		lockReq.setSrc(msgPasser.localName);
		lockReq.setOrigSrc(msgPasser.localName);

		serviceLock.lock();
		state = MutexState.WANTED;
		votes.clear();
		serviceLock.unlock();

		multicastService.multicast(lockReq);

		int memberCount = 0;
		int voteCount = 0;
		while (true) {
			memberCount = 0;
			voteCount = 0;

			serviceLock.lock();
			
			for (Node n : votingGroup.getMemberArray())
				memberCount++;

			for (String voter : votes)
				voteCount++;
			
			serviceLock.unlock();

			if (memberCount != voteCount)
				continue;

			break;
		}

		serviceLock.lock();
		state = MutexState.HELD;
		votes.clear();
		serviceLock.unlock();
		System.out.println("\n[State: Lock acquired]");
	}

	public void releaseMutex() {
		serviceLock.lock();

		if (this.state != MutexState.HELD) {
			System.out.println("\nWarning : Lock was not held. Invalid unlock() operation!");
			return;
		}

		Group votingGroup = msgPasser.groups.get(this.votingSet);
		TimeStampedMessage unlockReq = new TimeStampedMessage("", Kind.MULTICAST.toString(),
				Kind.RELEASE.toString(), votingGroup.getName());

		/* Increment and attach timestamp */
		TimeStamp gts = votingGroup.updateGroupTSOnSend(msgPasser.localName);
		VectorTimeStamp vts = (VectorTimeStamp)gts;
		//System.out.println("TimeStamp got : " + Arrays.toString(vts.getVector()));

		/* Set the group TimeStamp */
		unlockReq.setGroupTimeStamp(gts);

		unlockReq.setSrc(msgPasser.localName);
		unlockReq.setOrigSrc(msgPasser.localName);

		System.out.println("\n[State: Lock released]");
		state = MutexState.RELEASED;
		serviceLock.unlock();
		multicastService.multicast(unlockReq);
	}

	public void receiveMutexRequest(TimeStampedMessage reqMsg) {
		System.out.println("\n***[Request message received : " + reqMsg.getOrigSrc() + "]");

		serviceLock.lock();
		if (state == MutexState.HELD || votedMutex) {
			reqQueue.add(reqMsg);
			serviceLock.unlock();
		}
		else {
			votedMutex = true;
			serviceLock.unlock();
			voteForMutex(reqMsg);
		}
	}

	public void receiveMutexRelease(TimeStampedMessage relMsg) {
		System.out.println("\n***[Release message received : " + relMsg.getOrigSrc() + "]");
		if (reqQueue.size() > 0) {
			TimeStampedMessage reqMsg = reqQueue.remove(0);
			serviceLock.lock();
			votedMutex = true;
			serviceLock.unlock();
			voteForMutex(reqMsg);
		}
		else {
			serviceLock.lock();
			votedMutex = false;
			serviceLock.unlock();
		}
	}

	public void voteForMutex(TimeStampedMessage reqMsg) {
		System.out.println("\n***[Vote sent : " + reqMsg.getOrigSrc() + "]");
		TimeStampedMessage voteMsg = new TimeStampedMessage(reqMsg);
		voteMsg.setDest(reqMsg.getOrigSrc());
		voteMsg.setSrc(msgPasser.localName);
		voteMsg.setKind("");
		voteMsg.setData(Kind.VOTE.toString());

		if (voteMsg.getDest().equals(msgPasser.localName))
			receiveVote(voteMsg);
		else
			msgPasser.send(voteMsg);
	}

	public void receiveVote(TimeStampedMessage voteMsg) {
		boolean isVoterPresent = false;
		System.out.println("\n***[Received vote from : " + voteMsg.getSrc() + "]");
		serviceLock.lock();

		if (this.state == MutexState.WANTED) {
			for (String voter : votes) {
				if (voter.equals(voteMsg.getSrc()))
					isVoterPresent = true;
			}

			if (!isVoterPresent)
				votes.add(voteMsg.getSrc());
		}

		serviceLock.unlock();
	}
}
