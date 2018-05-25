package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;

import java.util.*;

public class Candidate extends Stage {
    private HashSet<String> votedForMe = new HashSet<>();

    private final int electionStarted;

    Candidate(ClusterProcess process) {
        super(process, null, process.dbProvider.getEpoch());
        electionStarted = process.currentTime;
        dbProvider.increaseEpoch();
        super.process.otherProcessesInCluster.forEach(x -> send(x, new ElectionRequest(dbProvider.getLastLogItem(), dbProvider.getEpoch())));
    }

    @Override
    public Stage act(Queue<Message> inbox) {
        List<Message> notForMe = new LinkedList<>();
        String myId = null;
        while (!inbox.isEmpty()) {
            Message message = inbox.poll();
            myId = Objects.requireNonNull(message).recipient;

            if (message instanceof ElectionVote) {
                ElectionVote msg = (ElectionVote) message;
                if (msg.epoch == dbProvider.getEpoch()) {
                    votedForMe.add(msg.sender);
                }
            } else if (message instanceof ElectionRequest) {
                ElectionRequest msg = (ElectionRequest) message;
                if (msg.newEpoch > dbProvider.getEpoch()) {
                    process.sendMessage(msg.sender, new ElectionVote(msg.newEpoch));
                    //todo what to do with rest of messages
                    return new Follower(process, null, msg.newEpoch);
                }
            } else if (message instanceof LeaderMessage) {
                LeaderMessage msg = (LeaderMessage) message;
                if (msg.epoch >= dbProvider.getEpoch()) {
                    //todo what to do with messages
                    return new Follower(process, msg.sender, msg.epoch);
                }
            } else {
                notForMe.add(message);
            }

            //todo potentially handle all other requests
        }
        inbox.addAll(notForMe);
        return evaluateElection(myId);
    }

    private Stage evaluateElection(String myId) {
        if ((process.otherProcessesInCluster.size() + 1) / 2 < (votedForMe.size()) + 1) {
            //I won leadership, cool
            return new Leader(process, myId, dbProvider.getEpoch());
        } else if (process.pingThreshold * 2 + electionStarted < process.currentTime) {
            return new Candidate(process);
        } else {
            return this;
        }
    }
}
