package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.raft.RaftProcess;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiConsumer;

/**
 * Vasim ukolem bude naimplementovat (pravdepodobne nejenom) tuto tridu. Procesy v clusteru pracuji
 * s logy, kde kazdy zanam ma podobu mapy - kazdy zaznam v logu by mel reprezentovat stav
 * distribuovane databaze v danem okamziku.
 *
 * Vasi implementaci budeme testovat v ruznych scenarich (viz evaluation.RaftRun a oficialni
 * zadani). Nasim cilem je, abyste zvladli implementovat jednoduchou distribuovanou key/value
 * databazi s garancemi podle RAFT.
 */
public class ClusterProcess extends RaftProcess<Map<String, String>> {

  // ostatni procesy v clusteru
  private final List<String> otherProcessesInCluster;
  // maximalni spozdeni v siti
  private final int networkDelays;

  public ClusterProcess(String id, Queue<Message> inbox, BiConsumer<String, Message> outbox,
      List<String> otherProcessesInCluster, int networkDelays) {
    super(id, inbox, outbox);
    this.otherProcessesInCluster = otherProcessesInCluster;
    this.networkDelays = networkDelays;
  }

  @Override
  public Optional<Map<String, String>> getLastSnapshotOfLog() {

    // komentar viz deklarace
    return Optional.empty();
  }

  @Override
  public String getCurrentLeader() {

    // komentar viz deklarace
    return null;
  }

  @Override
  public void act() {

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
  }
}
