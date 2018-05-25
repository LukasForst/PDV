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
                RaftMessage msg = (RaftMessage) message;
                processLeaderMessage(msg);
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

    private void processLeaderMessage(RaftMessage leaderMessage) {
        if (leaderMessage.epoch < dbProvider.getEpoch()) return;

        dbProvider.setEpoch(leaderMessage.epoch);
        process.currentLeader = leaderMessage.sender;
        lastPingFromLeader = process.currentTime;

        if (leaderMessage instanceof AppendEntry) {
            AppendEntry msg = (AppendEntry) leaderMessage;


        } else if (leaderMessage instanceof AppendEntryConfirmed) {
            AppendEntryConfirmed msg = (AppendEntryConfirmed) leaderMessage;

        }
    }

    private void voteIfCan(ElectionRequest msg) {
        if (canIVote(msg)) {
            dbProvider.setEpoch(msg.epoch);
            votedInEpochs.add(dbProvider.getEpoch());
            send(msg.sender, new ElectionVote(dbProvider));
        }
    }

    private boolean canIVote(ElectionRequest msg) {
        if (votedInEpochs.contains(msg.epoch)) return false;
        if (msg.epoch <= dbProvider.getEpoch()) return false;
        if (msg.nextIndex < dbProvider.getNextIndex()) return false;
        if (msg.lasItemInLog == null && dbProvider.getLastLogItem() == null) return true;
        if (msg.lasItemInLog == null) return false;
        return msg.lasItemInLog.index >= dbProvider.getLastLogItem().index;
    }

    private Stage checkLeaderActivity() {
        return lastPingFromLeader + process.pingThreshold < process.currentTime ? new Candidate(process) : this;
    }
}
