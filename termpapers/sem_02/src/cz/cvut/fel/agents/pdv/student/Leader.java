package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;
import cz.cvut.fel.agents.pdv.raft.messages.*;

import java.util.*;

class Leader extends Stage {
    private Pair<ClientRequestWithContent, HashSet<String>> waitingOperation = null;
    private Set<String> processedClientRequests = new HashSet<>();

    Leader(ClusterProcess process, String leader, int epoch) {
        super(process, leader, epoch);
        super.process.otherProcessesInCluster.forEach(x -> send(x, new HearthBeat(dbProvider)));
    }

    @Override
    public Stage act(Queue<Message> inbox) {
        List<Message> consumeLater = new LinkedList<>();
        List<RaftMessage> leaderMessages = new LinkedList<>();
        while (!inbox.isEmpty()) {
            Message message = inbox.poll();

            if (message instanceof RaftMessage) { // check if it belongs to this epoch
                RaftMessage msg = (RaftMessage) message;
                if (!dbProvider.compareLastLog(msg)) {
                    //todo what if it has diferent logs?
//                    continue;
                }
            }

            if (message instanceof ClientRequestWhoIsLeader) {
                ClientRequest msg = (ClientRequest) message;
                send(message.sender, new ServerResponseLeader(msg.getRequestId(), msg.recipient));
            } else if (message instanceof ClientRequestWithContent) {
                ClientRequestWithContent msg = (ClientRequestWithContent) message;

                if (waitingOperation != null) {
                    consumeLater.add(msg);
                    continue;
                }

                if (processedClientRequests.contains(msg.getRequestId())) {
                    continue;
                }

                processedClientRequests.add(msg.getRequestId());
                StoreOperationEnums operation = StoreOperationEnums.valueOf(msg.getOperation().getName());
                //noinspection unchecked this is because we need to cast it from object
                Pair<String, String> data = (Pair<String, String>) msg.getContent();

                if (operation == StoreOperationEnums.GET) {
                    send(msg.sender, new ServerResponseWithContent<>(msg.getRequestId(), dbProvider.dataGetOrDefault(data.getFirst())));
                } else {
                    waitingOperation = new Pair<>(msg, new HashSet<>());
                    process.otherProcessesInCluster.forEach(x -> send(x, new AppendEntry(dbProvider, new Pair<>(operation, data), msg.getRequestId())));
                    dbProvider.addNonVerified(new AppendEntry(dbProvider, new Pair<>(operation, data), msg.getRequestId()));
                }

            } else if (message instanceof AppendEntryResponse) {
                AppendEntryResponse msg = (AppendEntryResponse) message;
                if (waitingOperation == null) {
                    //todo that's weird
                    continue;
                }

                if (msg.canPerformOperation && msg.requestId.equals(waitingOperation.getFirst().getRequestId())) {
                    waitingOperation.getSecond().add(msg.sender);
                } else {
                    send(msg.sender, new RecreateLogAndDataDeepCopy(dbProvider));
                    //noinspection unchecked this is because we need to cast it from object
                    send(msg.sender, new AppendEntry(dbProvider,
                            new Pair<>(StoreOperationEnums.valueOf(waitingOperation.getFirst().getOperation().getName()),
                                    (Pair<String, String>) waitingOperation.getFirst().getContent()),
                            msg.requestId));
                }

            } else if (message instanceof LeaderMessage) {
                //this is safe because every interface is extending this abstract class
                @SuppressWarnings("ConstantConditions") RaftMessage msg = (RaftMessage) message;
                leaderMessages.add(msg);

            } else if (message instanceof ElectionRequest) {
                ElectionRequest msg = (ElectionRequest) message;
                if (msg.epoch > dbProvider.getEpoch()) {
                    dbProvider.setEpoch(msg.epoch);

                    send(msg.sender, new ElectionVote(dbProvider));

                    //todo what to do with other messages
                    return new Follower(process, null, dbProvider.getEpoch());
                }
            } else if (message instanceof ForwardedClientRequest) {
                ForwardedClientRequest msg = (ForwardedClientRequest) message;
                inbox.add(msg.originalMessage);
            } else {
                consumeLater.add(message);
            }
        }

        inbox.addAll(consumeLater);
        process.otherProcessesInCluster.forEach(x -> send(x, new HearthBeat(dbProvider)));
        checkWaitingOperation();
        return handleLeaderMessages(leaderMessages);
    }

    private void checkWaitingOperation() {
        if (waitingOperation != null && (process.otherProcessesInCluster.size() + 1) / 2 < (waitingOperation.getSecond().size() + 1)) {
            send(waitingOperation.getFirst().sender, new ServerResponseConfirm(waitingOperation.getFirst().getRequestId()));
            waitingOperation = null;
            dbProvider.writeNonVerified_UNSAFE();
        }
    }

    private Stage handleLeaderMessages(List<RaftMessage> messages) {
        if (messages.size() != 0) {
            messages.sort((m1, m2) -> Integer.compare(m2.epoch, m1.epoch));
            RaftMessage biggestEpoch = messages.get(0);
            if (biggestEpoch.epoch > dbProvider.getEpoch()) {
                return new Follower(process, biggestEpoch.sender, biggestEpoch.epoch);
            }
        }

        return this;
    }
}
