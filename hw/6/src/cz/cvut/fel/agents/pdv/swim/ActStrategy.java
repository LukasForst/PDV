package cz.cvut.fel.agents.pdv.swim;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Trida s implementaci metody act() pro proces Failure Detector. Tuto tridu (a tridy pouzivanych zprav) budete
 * odevzdavat. Do tridy si muzete doplnit vlastni pomocne datove struktury. Hodnoty muzete inicializovat primo
 * v konstruktoru. Klicova je metoda act(), kterou vola kazda instance tridy FailureDetectorProcess ve sve metode
 * act(). Tuto metodu naimplementujte podle protokolu SWIM predstaveneho na prednasce.
 * <p>
 * Pokud si stale jeste nevite rady s frameworkem, inspiraci muzete nalezt v resenych prikladech ze cviceni.
 */
public class ActStrategy {
    private int tics = 0;
    private int messagesSent = 0;


    // maximalni zpozdeni zprav
    private final int maxDelayForMessages;
    private final int timeToDetectKilledProcess;
    private final int upperBoundOnMessages;
    private final List<String> otherProcesses;

    private final HashMap<String, Integer> ackWaitingProcess = new HashMap<>();
    private final HashMap<String, Integer> reAckWaitingProcess = new HashMap<>();

    private static final Random rd = new Random();
    private int currentIndex;
    // Definujte vsechny sve promenne a datove struktury, ktere budete potrebovat

    public ActStrategy(int maxDelayForMessages, List<String> otherProcesses,
                       int timeToDetectKilledProcess, int upperBoundOnMessages) {
        this.maxDelayForMessages = maxDelayForMessages;
        this.timeToDetectKilledProcess = timeToDetectKilledProcess;
        this.otherProcesses = otherProcesses;
        this.upperBoundOnMessages = upperBoundOnMessages;

        currentIndex = rd.nextInt(otherProcesses.size());
    }

    /**
     * Metoda je volana s kazdym zavolanim metody act v FailureDetectorProcess. Metodu implementujte
     * tak, jako byste implementovali metodu act() v FailureDetectorProcess, misto pouzivani send()
     * pridejte zpravy v podobe paru - prijemce, zprava do listu. Zpravy budou nasledne odeslany.
     * <p>
     * Diky zavedeni teto metody muzeme kontrolovat pocet odeslanych zprav vasi implementaci.
     */
    public List<Pair<String, Message>> act(Queue<Message> inbox, String disseminationProcess) {
        tics++;
        // Od DisseminationProcess muzete dostat zpravu typu DeadProcessMessage, ktera Vas
        // informuje o spravne detekovanem ukoncenem procesu.
        // DisseminationProcess muzete poslat zpravu o detekovanem "mrtvem" procesu.
        // Zprava musi byt typu PFDMessage.
        List<Pair<String, Message>> result = new LinkedList<>();
        processInbox(inbox, result);

        checkCurrentWaiting(result, disseminationProcess);

        result.add(new Pair<>(otherProcesses.get(currentIndex), new PingMsg()));
        ackWaitingProcess.put(otherProcesses.get(currentIndex), tics);
        currentIndex = (currentIndex + 1) % otherProcesses.size();

        messagesSent += result.size();
        if (messagesSent > upperBoundOnMessages) {
            result.clear();
        }
        return result;
    }

    private void checkCurrentWaiting(List<Pair<String, Message>> result, String disseminationProcess) {
        int maxTime = (maxDelayForMessages + 1) * 2;
        List<String> toRemove = new LinkedList<>();
        for (String process : ackWaitingProcess.keySet()) {
            if (ackWaitingProcess.get(process) + maxTime <= tics) {
                toRemove.add(process);
                sendRePing(process, result, 4);
            }
        }

        toRemove.forEach(ackWaitingProcess::remove);
        toRemove.clear();

        maxTime = maxTime * 2;
        for (String process : reAckWaitingProcess.keySet()) {
            if (reAckWaitingProcess.get(process) + maxTime < tics) {
                toRemove.add(process);
                result.add(new Pair<>(disseminationProcess, new PFDMessage(process)));
            }
        }
        toRemove.forEach(reAckWaitingProcess::remove);
    }

    private void sendRePing(String target, List<Pair<String, Message>> result, int times) {
        List<String> a = otherProcesses.stream()
                .filter(x -> !ackWaitingProcess.keySet().contains(x) && !reAckWaitingProcess.keySet().contains(x))
                .collect(Collectors.toList());
        Collections.shuffle(a);
        a = a.stream().limit(times).collect(Collectors.toList());

        List<String> proxies = new ArrayList<>();
        if (a.size() == times) {
            proxies = a;
        } else {
            proxies = new ArrayList<>(a);
            a = otherProcesses.stream().filter(x -> !reAckWaitingProcess.keySet().contains(x)).collect(Collectors.toList());

//            if (a.size() == 0) {
//                a = new ArrayList<>(otherProcesses);
//                Collections.shuffle(a);
//            }

            while (proxies.size() != times && a.size() != 0) {
                proxies.add(a.get(rd.nextInt(a.size())));
            }
        }
        for (String proxy : proxies) {
            reAckWaitingProcess.put(target, tics);
            result.add(new Pair<>(proxy, new PingReqMsg(target)));
        }
    }

    private HashMap<String, String> requests = new HashMap<>();

    private void processInbox(Queue<Message> inbox, List<Pair<String, Message>> result) {
        while (!inbox.isEmpty()) {
            Message m = inbox.poll();
            if (m instanceof PingReqMsg) {
                PingReqMsg msg = (PingReqMsg) m;
                requests.put(msg.targetId, msg.sender);

                result.add(new Pair<>(msg.targetId, new PingMsg()));
            } else if (m instanceof PingMsg) {
                PingMsg msg = (PingMsg) m;
                result.add(new Pair<>(msg.getOriginalSender(), new AckMsg()));
            } else if (m instanceof AckMsg) {
                AckMsg msg = (AckMsg) m;

                if (requests.containsKey(msg.getOriginalSender())) {
                    result.add(new Pair<>(requests.get(msg.getOriginalSender()), new AckMsg(msg.getOriginalSender())));
                    requests.remove(msg.getOriginalSender());
                }

                ackWaitingProcess.remove(msg.getOriginalSender());
                reAckWaitingProcess.remove(msg.getOriginalSender());
            } else if (m instanceof PFDMessage) {
                PFDMessage msg = (PFDMessage) m;

            } else if (m instanceof DeadProcessMessage) {
                DeadProcessMessage msg = (DeadProcessMessage) m;
                ackWaitingProcess.remove(msg.getProcessID());
                reAckWaitingProcess.remove(msg.getProcessID());
                otherProcesses.remove(msg.getProcessID());
            } else if (m instanceof ReAckMsg) {
                ReAckMsg msg = (ReAckMsg) m;
                result.add(new Pair<>(msg.targetId, new AckMsg(msg.sender)));
            }
        }
    }
}

class PingReqMsg extends Message {
    final String targetId;

    PingReqMsg(String targetId) {
        this.targetId = targetId;
    }
}

class PingMsg extends Message {
    private final String originalSender;

    PingMsg(String originalSender) {
        this.originalSender = originalSender;
    }

    PingMsg() {
        this.originalSender = "";
    }

    String getOriginalSender() {
        return originalSender.equals("") ? super.sender : originalSender;
    }
}

class AckMsg extends Message {
    private final String originalSender;

    AckMsg(String originalSender) {
        this.originalSender = originalSender;
    }

    AckMsg() {
        this.originalSender = "";
    }

    String getOriginalSender() {
        return originalSender.equals("") ? super.sender : originalSender;
    }
}

class ReAckMsg extends Message {
    final String targetId;

    public ReAckMsg(String targetId) {
        this.targetId = targetId;
    }
}
