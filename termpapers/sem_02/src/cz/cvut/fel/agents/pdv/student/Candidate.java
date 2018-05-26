package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.raft.messages.ClientRequest;
import cz.cvut.fel.agents.pdv.raft.messages.ServerResponseLeader;

import java.util.*;

public class Candidate extends Stage {
    private HashSet<String> votedForMe = new HashSet<>();

    private final int electionStarted;

    Candidate(ClusterProcess process) {
        super(process, null, process.dbProvider.getEpoch());
        electionStarted = process.currentTime;
        dbProvider.increaseEpoch();
        super.process.otherProcessesInCluster.forEach(x -> send(x, new ElectionRequest(dbProvider)));
    }

    @Override
    public Stage act(Queue<Message> inbox) {
        List<Message> notForMe = new LinkedList<>();
        Stage resultStage = null;
        String myId = null;

        while (!inbox.isEmpty() && resultStage == null) {
            Message message = inbox.poll();
            myId = Objects.requireNonNull(message).recipient;

            if (message instanceof ElectionVote) {
                electionVote((ElectionVote) message);

            } else if (message instanceof ElectionRequest) {
                resultStage = electionRequest((ElectionRequest) message);

            } else if (message instanceof LeaderMessage) {
                resultStage = leaderMessage((RaftMessage) message, inbox);

            } else {
                notForMe.add(message);
            }
        }
        inbox.addAll(notForMe);
        return evaluateElection(myId, resultStage);
    }

    private void electionVote(ElectionVote msg) {
        if (msg.epoch == dbProvider.getEpoch()) {
            votedForMe.add(msg.sender);
        }
    }

    private Stage electionRequest(ElectionRequest msg) {
        if (canIVote(msg)) {
            dbProvider.setEpoch(msg.epoch);
            process.sendMessage(msg.sender, new ElectionVote(dbProvider));
            return new Follower(process, null, dbProvider.getEpoch());
        }
        return null;
    }

    private boolean canIVote(ElectionRequest msg) {
        if (msg.epoch < dbProvider.getEpoch()) return false;
        return msg.epoch == dbProvider.getEpoch() && msg.nextIndex <= dbProvider.getNextIndex();
    }


    private Stage leaderMessage(RaftMessage msg, Queue<Message> inbox) {
        if (msg.epoch >= dbProvider.getEpoch() || msg.nextIndex > dbProvider.getNextIndex()) {
            inbox.stream()
                    .filter(x -> x instanceof ClientRequest)
                    .map(x -> (ClientRequest) x)
                    .forEach(x -> send(x.sender, new ServerResponseLeader(x.getRequestId(), msg.sender)));
            inbox.removeIf(x -> x instanceof ClientRequest);
            inbox.add(msg);
            send(msg.sender, new RecreateLogAndDataDeepCopyRequest());
            return new Follower(process, msg.sender, msg.epoch);
        }
        return null;
    }


    private Stage evaluateElection(String myId, Stage resultStage) {
        if (resultStage != null) return resultStage;
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
