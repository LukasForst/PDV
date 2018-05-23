package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.raft.RaftProcess;
import cz.cvut.fel.agents.pdv.raft.messages.*;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Vasim ukolem bude naimplementovat (pravdepodobne nejenom) tuto tridu. Procesy v clusteru pracuji
 * s logy, kde kazdy zanam ma podobu mapy - kazdy zaznam v logu by mel reprezentovat stav
 * distribuovane databaze v danem okamziku.
 * <p>
 * Vasi implementaci budeme testovat v ruznych scenarich (viz evaluation.RaftRun a oficialni
 * zadani). Nasim cilem je, abyste zvladli implementovat jednoduchou distribuovanou key/value
 * databazi s garancemi podle RAFT.
 */
@SuppressWarnings("unchecked") //this is awful, but what can I do when teachers want it that way
public class ClusterProcess extends RaftProcess<Map<String, String>> {
    private static Random rd = new Random();

    private final DatabaseProvider dbProvider = new DatabaseProvider();

    private final HashSet<Integer> votesInEpochs = new HashSet<>();
    // ostatni procesy v clusteru
    private final List<String> otherProcessesInCluster;
    private int otherProcessesVotedForMe = 0;
    // maximalni spozdeni v siti
    private final int networkDelays;

    private String currentLeader = null;
    private int lastPingReceived = Integer.MIN_VALUE;
    private int pingThreshold;
    private int currentTime = 0;

    private NodeType nodeType;


    public ClusterProcess(String id, Queue<Message> inbox, BiConsumer<String, Message> outbox,
                          List<String> otherProcessesInCluster, int networkDelays) {
        super(id, inbox, outbox);
        this.otherProcessesInCluster = otherProcessesInCluster;
        this.networkDelays = networkDelays;

        pingThreshold = networkDelays == 0 ? 10 : networkDelays * 10;
        pingThreshold = rd.nextInt(pingThreshold) + pingThreshold;
        if(rd.nextBoolean()){
            startElection();
        } else {
            nodeType = NodeType.FOLLOWER;
        }
    }

    @Override
    public Optional<Map<String, String>> getLastSnapshotOfLog() {
        return dbProvider.getLastSnapshotOfLog();
    }

    @Override
    public String getCurrentLeader() {
        return currentLeader;
    }

    // doimplementuje metodu act() podle RAFT

    // krome vlastnich zprav (vasich trid), dostavate typy zprav z balicku raft.messages s rodicem
    // ClientRequest, tak si je projdete, at vite, co je ucelem a obsahem jednotlivych typu.
    // V pripade ClientRequestWithContent dostavate zpravu typu
    // ClientRequestWithContent<StoreOperationEnums, Pair<String, String>>, kde StoreOperationEnums
    // je operace, kterou mate udelat s obsahem paru Pair<String, String>, kde prvni hodnota
    // paru je klic (nikdy neni prazdny) a druha je hodnota (v pripade pozadavku GET je prazdny)

    // dejte si pozor na jednotlive akce podle RAFT. S klientem komunikujte vyhradne pomoci zprav
    // typu ServerResponse v messages

    // na pozadavky klientu odpovidate zpravami typu ServerResponse viz popis podtypu v messages.
    // !!! V TOMTO PRIPADE JE 'requestId' ROVNO POZADAVKU KLIENTA, NA KTERY ODPOVIDATE !!!

    // dalsi podrobnosti naleznete na strance se zadanim


    @Override
    public void act() {
        currentTime++;
        switch (nodeType) {
            case LEADER:
                actLeader(inbox);
                break;
            case FOLLOWER:
                actFollower(inbox);
                break;
            case CANDIDATE:
                actCandidate(inbox);
                break;
        }
    }

    private void actFollower(Queue<Message> inbox) {
        List<Message> notForMe = new LinkedList<>();
        while (!inbox.isEmpty()) {
            Message message = inbox.poll();

            if (message instanceof ClientRequest) {
                ClientRequest msg = (ClientRequest) message;
                send(msg.sender, new ServerResponseLeader(msg.getRequestId(), currentLeader));

            } else if (message instanceof AppendEntry) {
                AppendEntry msg = (AppendEntry) message;
                boolean result = dbProvider.performLogWriteOperation(msg.newEntry.operation,
                        msg.newEntry.value, msg.lasItemInLog);

                send(msg.recipient, new AppendEntryResponse(result, dbProvider.getLastLogItem()));

            } else if (message instanceof AppendEntryConfirmed) {
                AppendEntryConfirmed msg = (AppendEntryConfirmed) message;
                if (LogItem.equals(msg.lasItemInLog,dbProvider.getLastLogItem())) {
                    dbProvider.executeLastOnData();
                }

            } else if (message instanceof LeaderMessage) {
                LeaderMessage msg = (LeaderMessage) message;
                if (msg.epoch >= dbProvider.getEpoch()) {
                    setNewLeader(msg);
                }

            } else if (message instanceof ElectionRequest) {
                ElectionRequest msg = (ElectionRequest) message;
                voteIfCan(msg);
            } else {
                notForMe.add(message);
            }
        }
        inbox.addAll(notForMe);
        if (lastPingReceived + pingThreshold < currentTime) startElection();
    }

    private void voteIfCan(ElectionRequest msg){
        if (canIVote(msg)) {
            dbProvider.setEpoch(msg.newEpoch);
            votesInEpochs.add(dbProvider.getEpoch());
            send(msg.sender, new ElectionVote(dbProvider.getEpoch()));
        }
    }

    private boolean canIVote(ElectionRequest msg){
        if(votesInEpochs.contains(msg.newEpoch)) return false;
        if(msg.newEpoch <= dbProvider.getEpoch()) return false;
        if(msg.lastLogItem == null && dbProvider.getLastLogItem() == null) return true;
        if(msg.lastLogItem == null) return  false;
        return msg.lastLogItem.index >= dbProvider.getLastLogItem().index;
    }

    private void startElection() {
        nodeType = NodeType.CANDIDATE;
        dbProvider.increaseEpoch();
        electionStarted = currentTime;
        votedForMe.clear();
        for (String target : otherProcessesInCluster) {
            Message m = new ElectionRequest(dbProvider.getEpoch());
            super.send(target, m);
        }
        votesInEpochs.add(dbProvider.getEpoch());
    }

    private HashSet<String> waitingForResponseSet = new HashSet<>();

    private void actLeader(Queue<Message> inbox) {
        List<Message> notForMe = new LinkedList<>();

        while (!inbox.isEmpty()) {
            Message message = inbox.poll();

            if (message instanceof ClientRequestWhoIsLeader) {
                ClientRequest msg = (ClientRequest) message;
                send(message.recipient, new ServerResponseLeader(msg.getRequestId(), currentLeader));
            } else if (message instanceof LeaderMessage) {
                LeaderMessage msg = (LeaderMessage) message;
                setNewLeader(msg);
            } else if (message instanceof ClientRequestWithContent) {
                ClientRequestWithContent msg = (ClientRequestWithContent) message;

            } else {
                notForMe.add(message);
            }
        }
        inbox.addAll(notForMe);
        otherProcessesInCluster.forEach(x -> send(x, new HearthBeat(dbProvider.getLastLogItem(), dbProvider.getEpoch())));
    }

    private void setNewLeader(LeaderMessage msg) {
        dbProvider.setEpoch(msg.epoch);
        nodeType = NodeType.FOLLOWER;
        currentLeader = msg.sender;
        lastPingReceived = currentTime;
        electionStarted = Integer.MAX_VALUE;
        votedForMe.clear();
    }

    private HashSet<String> votedForMe = new HashSet<>();
    private int electionStarted = Integer.MAX_VALUE;

    private void actCandidate(Queue<Message> inbox) {
        String myId = null;
        List<Message> notForMe = new LinkedList<>();

        while (!inbox.isEmpty()) {
            Message message = inbox.poll();

            if (message instanceof LeaderMessage) {
                LeaderMessage msg = (LeaderMessage) message;
                if (msg.epoch > dbProvider.getEpoch()) {
                    setNewLeader(msg);
                }

            } else if(message instanceof ElectionRequest){
                ElectionRequest msg = (ElectionRequest) message;
                voteIfCan(msg);
            } else if (message instanceof ElectionVote) {
                ElectionVote msg = (ElectionVote) message;
                myId = msg.recipient;
                if (msg.epoch == dbProvider.getEpoch()) {
                    votedForMe.add(msg.sender);
                }
            } else {
                notForMe.add(message);
            }
        }
        inbox.addAll(notForMe);

        if ((otherProcessesInCluster.size() + 1) / 2 < votedForMe.size()) {
            //I won leadership, cool
            nodeType = NodeType.LEADER;
            currentLeader = myId;
            votedForMe.clear();
            otherProcessesInCluster.forEach(x -> send(x, new HearthBeat(dbProvider.getLastLogItem(), dbProvider.getEpoch())));
        } else if (pingThreshold * 2 + electionStarted < currentTime) {
            startElection();
        }
    }
}
