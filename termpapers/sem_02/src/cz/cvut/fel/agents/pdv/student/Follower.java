package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.raft.messages.ClientRequest;
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
        lastPingFromLeader = super.process.currentTime;
    }

    @Override
    public Stage act(Queue<Message> inbox) {
        List<Message> notForMe = new LinkedList<>();
        while (!inbox.isEmpty()) {
            Message message = inbox.poll();

            if (message instanceof ClientRequest) {
                ClientRequest msg = (ClientRequest) message;
                send(msg.sender, new ServerResponseLeader(msg.getRequestId(), process.currentLeader));

            } else if (message instanceof LeaderMessage) {
                RaftMessage msg = (RaftMessage) message;
                processLeaderMessage(msg);

            } else if (message instanceof ElectionRequest) {
                ElectionRequest msg = (ElectionRequest) message;
                voteIfCan(msg);

            } else if (message instanceof RecreateLogAndDataDeepCopy) {
                RecreateLogAndDataDeepCopy msg = (RecreateLogAndDataDeepCopy) message;
                //todo probably do not replicate everything
                dbProvider.replicateLog(msg);

            } else {
                notForMe.add(message);
            }
        }

        inbox.addAll(notForMe);
        return checkLeaderActivity();
    }

    private void processLeaderMessage(RaftMessage leaderMessage) {
        if (leaderMessage.epoch < dbProvider.getEpoch()) {
            return;
        }

        dbProvider.setEpoch(leaderMessage.epoch);
        process.currentLeader = leaderMessage.sender;
        lastPingFromLeader = process.currentTime;

        if (leaderMessage instanceof AppendEntry) {
            appendEntry((AppendEntry) leaderMessage);

        } else if (leaderMessage instanceof AppendEntryConfirmed) {
            AppendEntryConfirmed msg = (AppendEntryConfirmed) leaderMessage;
            if (LogItem.equals(msg.lasItemInLog, dbProvider.getNotVerified())) {
                dbProvider.writeNonVerified(msg);
            }
        }
    }

    private void appendEntry(AppendEntry msg) {
        if (dbProvider.compareLastLog(msg)) {
            dbProvider.addNonVerified(msg);
            send(msg.sender, new AppendEntryResponse(dbProvider, true, msg.requestId));
        } else {
            send(msg.sender, new AppendEntryResponse(dbProvider, false, msg.requestId));
        }
    }

    private void voteIfCan(ElectionRequest msg) {
        if (canIVote(msg)) {
            process.currentLeader = null;
            lastPingFromLeader = process.currentTime;
            dbProvider.setEpoch(msg.epoch);
            votedInEpochs.add(dbProvider.getEpoch());
            send(msg.sender, new ElectionVote(dbProvider));
        }
    }

    private boolean canIVote(ElectionRequest msg) {
        if (votedInEpochs.contains(msg.epoch)) return false;
        if (msg.epoch < dbProvider.getEpoch()) return false;
        if (msg.epoch == dbProvider.getEpoch() && msg.nextIndex < dbProvider.getNextIndex()) return false;
        return msg.epoch > dbProvider.getEpoch() || msg.nextIndex >= dbProvider.getNextIndex();
    }

    private Stage checkLeaderActivity() {
        return lastPingFromLeader + process.pingThreshold < process.currentTime ? new Candidate(process) : this;
    }
}
