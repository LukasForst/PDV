package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.raft.RaftProcess;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Vasim ukolem bude naimplementovat (pravdepodobne nejenom) tuto tridu. Procesy v clusteru pracuji
 * s logy, kde kazdy zanam ma podobu mapy - kazdy zaznam v logu by mel reprezentovat stav
 * distribuovane databaze v danem okamziku.
 * <p>
 * Vasi implementaci budeme testovat v ruznych scenarich (viz evaluation.RaftRun a oficialni
 * zadani). Nasim cilem je, abyste zvladli implementovat jednoduchou distribuovanou key/data
 * databazi s garancemi podle RAFT.
 */
public class ClusterProcess extends RaftProcess<Map<String, String>> {
    private static Random rd = new Random();

    final DatabaseProvider dbProvider = new DatabaseProvider();

    // ostatni procesy v clusteru
    final List<String> otherProcessesInCluster;

    String currentLeader = null;
    int pingThreshold;
    int currentTime = 0;

    private Stage stage;

    public ClusterProcess(String id, Queue<Message> inbox, BiConsumer<String, Message> outbox,
                          List<String> otherProcessesInCluster, int networkDelays) {
        super(id, inbox, outbox);
        this.otherProcessesInCluster = otherProcessesInCluster;

        pingThreshold = networkDelays == 0 ? 2 : networkDelays * 10;
        pingThreshold = rd.nextInt(pingThreshold * 10) + pingThreshold;
        stage = new Follower(this, null, dbProvider.getEpoch());
    }

    @Override
    public Optional<Map<String, String>> getLastSnapshotOfLog() {
        return dbProvider.getLastSnapshotOfLog();
    }

    @Override
    public String getCurrentLeader() {
        return currentLeader;
    }


    void sendMessage(String x, Message m) {
        send(x, m);
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
        stage = stage.act(inbox);
    }
}
