package ds.service;

import java.util.ArrayList;
import java.util.Arrays;

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
	boolean votedMutex = false;
	MutexState state = MutexState.RELEASED;
	ArrayList<String> votes = new ArrayList<String>();
	ArrayList<TimeStampedMessage> reqQueue = new ArrayList<TimeStampedMessage>();

	public MutexService() {
		try {
			msgPasser = MessagePasser.getInstance();
			multicastService = FactoryService.mcService;
			configureVotingSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void configureVotingSet() {
		for (String groupName : msgPasser.groups.keySet()) {
			Group grp = msgPasser.groups.get(groupName);

			if (grp.getMemberArray().size() > 0) {
				if (groupName.equals(((Node)grp.getMemberArray().get(0)).getName())) {
					this.votingSet = groupName;
					return;
				}
			}
		}
	}

	public void acquireMutex() {
		System.out.println("[State: Requesting lock]");
		Group votingGroup = msgPasser.groups.get(this.votingSet);
		TimeStampedMessage lockReq = new TimeStampedMessage("", Kind.REQUEST.toString(), "", votingGroup.getName());

		/* Increment and attach timestamp */
		TimeStamp gts = votingGroup.updateGroupTSOnSend(msgPasser.localName);
		VectorTimeStamp vts = (VectorTimeStamp)gts;
		System.out.println("TimeStamp got : "+Arrays.toString(vts.getVector()));

		/* Set the group TimeStamp */
		lockReq.setGroupTimeStamp(gts);

		lockReq.setSrc(msgPasser.localName);
		lockReq.setOrigSrc(msgPasser.localName);

		state = MutexState.WANTED;
		votes.clear();

		multicastService.multicast(lockReq);

		while (votes.size() != votingGroup.numOfMembers())
			continue;

		state = MutexState.HELD;
		votes.clear();
		System.out.println("[State: Lock acquired]");
	}

	public void releaseMutex() {
		state = MutexState.RELEASED;

		Group votingGroup = msgPasser.groups.get(this.votingSet);
		TimeStampedMessage unlockReq = new TimeStampedMessage("", Kind.RELEASE.toString(), "", votingGroup.getName());

		/* Increment and attach timestamp */
		TimeStamp gts = votingGroup.updateGroupTSOnSend(msgPasser.localName);
		VectorTimeStamp vts = (VectorTimeStamp)gts;
		System.out.println("TimeStamp got : " + Arrays.toString(vts.getVector()));

		/* Set the group TimeStamp */
		unlockReq.setGroupTimeStamp(gts);

		unlockReq.setSrc(msgPasser.localName);
		unlockReq.setOrigSrc(msgPasser.localName);

		multicastService.multicast(unlockReq);
		System.out.println("[State: Lock released]");
	}

	public void receiveMutexRequest(TimeStampedMessage reqMsg) {
		if (state == MutexState.HELD || votedMutex)
			reqQueue.add(reqMsg);
		else {
			voteForMutex(reqMsg);
			votedMutex = true;
		}
	}

	public void receiveMutexRelease(TimeStampedMessage relMsg) {
		if (reqQueue.size() > 0) {
			TimeStampedMessage reqMsg = reqQueue.remove(0);
			voteForMutex(reqMsg);
			votedMutex = true;
		}
		else
			votedMutex = false;
	}

	public void voteForMutex(TimeStampedMessage reqMsg) {
		TimeStampedMessage voteMsg = new TimeStampedMessage(reqMsg);
		voteMsg.setDest(reqMsg.getOrigSrc());
		voteMsg.setSrc(msgPasser.localName);
		voteMsg.setKind(Kind.VOTE.toString());
		msgPasser.send(voteMsg);
	}

	public void receiveVote(TimeStampedMessage voteMsg) {
		votes.add(voteMsg.getSrc());
	}
}
