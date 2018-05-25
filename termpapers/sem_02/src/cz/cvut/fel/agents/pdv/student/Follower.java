package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.raft.messages.ClientRequestWhoIsLeader;
import cz.cvut.fel.agents.pdv.raft.messages.ServerResponseLeader;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class Follower extends Stage {
    private HashSet<Integer> votedInEpochs = new HashSet<>();
    private int lastPingFromLeader;

    Follower(ClusterProcess process, String leader, int epoch) {
        super(process, leader, epoch);
        lastPingFromLeader = process.currentTime;
    }

    @Override
    public Stage act(Queue<Message> inbox) {
        List<Message> notForMe = new LinkedList<>();
        while (!inbox.isEmpty()) {
            Message message = inbox.poll();

            if (message instanceof ClientRequestWhoIsLeader) {
                ClientRequestWhoIsLeader msg = (ClientRequestWhoIsLeader) message;
                send(msg.sender, new ServerResponseLeader(msg.getRequestId(), process.getCurrentLeader()));
            } else if (message instanceof LeaderMessage) {
                LeaderMessage msg = (LeaderMessage) message;
                if (msg.epoch >= dbProvider.getEpoch()) {
                    dbProvider.setEpoch(msg.epoch);
                    process.currentLeader = msg.sender;
                    lastPingFromLeader = process.currentTime;
                }

            } else if (message instanceof ElectionRequest) {
                ElectionRequest msg = (ElectionRequest) message;
                voteIfCan(msg);
            } else {
                notForMe.add(message);
            }
        }

        inbox.addAll(notForMe);
        return checkLeaderActivity();
    }

    private void voteIfCan(ElectionRequest msg) {
        if (canIVote(msg)) {
            dbProvider.setEpoch(msg.newEpoch);
            votedInEpochs.add(dbProvider.getEpoch());
            send(msg.sender, new ElectionVote(dbProvider.getEpoch()));
        }
    }

    private boolean canIVote(ElectionRequest msg) {
        if (votedInEpochs.contains(msg.newEpoch)) return false;
        if (msg.newEpoch <= dbProvider.getEpoch()) return false;
        if (msg.lastLogItem == null && dbProvider.getLastLogItem() == null) return true;
        if (msg.lastLogItem == null) return false;
        return msg.lastLogItem.index >= dbProvider.getLastLogItem().index;
    }

    private Stage checkLeaderActivity() {
        return lastPingFromLeader + process.pingThreshold < process.currentTime ? new Candidate(process) : this;
    }
}
