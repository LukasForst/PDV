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
//        if (messagesSent >= upperBoundOnMessages) return result;

        checkCurrentWaiting(result, disseminationProcess);
        List<String> live = otherProcesses.stream().filter(x -> !ackWaitingProcess.containsKey(x) && !reAckWaitingProcess.containsKey(x)).collect(Collectors.toList());

        if (live.size() != 0 && ackWaitingProcess.isEmpty() && reAckWaitingProcess.isEmpty()) {
            Collections.shuffle(live);
            String target = live.get(rd.nextInt(live.size()));
            result.add(new Pair<>(target, new PingMsg()));
            ackWaitingProcess.put(target, tics);
        }
        messagesSent += result.size();
//        if (messagesSent > upperBoundOnMessages) {
//            result = result.stream().filter(x -> x.getSecond() instanceof AckMsg).limit(messagesSent - upperBoundOnMessages).collect(Collectors.toList());
//        }

        return result;
    }

    private void checkCurrentWaiting(List<Pair<String, Message>> result, String disseminationProcess) {
        int maxTime = (maxDelayForMessages + 1) * 2;
        List<String> toRemove = new LinkedList<>();
        for (String process : ackWaitingProcess.keySet()) {
            if (ackWaitingProcess.get(process) + maxTime < tics) {
                toRemove.add(process);
                sendRePing(process, result);
            }
        }

        toRemove.forEach(ackWaitingProcess::remove);
        toRemove.clear();

        maxTime = maxTime * 2 + 4;
        for (String process : reAckWaitingProcess.keySet()) {
            if (reAckWaitingProcess.get(process) + maxTime < tics) {
                toRemove.add(process);
                result.add(new Pair<>(disseminationProcess, new PFDMessage(process)));
            }
        }
        toRemove.forEach(reAckWaitingProcess::remove);
    }

    private void sendRePing(String target, List<Pair<String, Message>> result) {
        int size = 10;
        List<String> a = otherProcesses.stream()
                .filter(x -> !ackWaitingProcess.keySet().contains(x) && !reAckWaitingProcess.keySet().contains(x) && !x.equals(target))
                .collect(Collectors.toList());

        List<String> proxies;
        if (a.size() != 0) {
            Collections.shuffle(a);
            proxies = a;
        } else {
            proxies = otherProcesses.stream().filter(x -> !reAckWaitingProcess.keySet().contains(x) && !x.equals(target)).collect(Collectors.toList());
            if (proxies.size() < size) {
                proxies.addAll(otherProcesses.stream().filter(x -> !proxies.contains(x) && !x.equals(target)).limit(size - proxies.size()).collect(Collectors.toList()));
            }
        }

        reAckWaitingProcess.put(target, tics);
        for (int i = 0; i < size; i++) {
            result.add(new Pair<>(proxies.get(rd.nextInt(proxies.size())), new PingReqMsg(target)));
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
                result.add(new Pair<>(msg.sender, new AckMsg()));
            } else if (m instanceof AckMsg) {
                AckMsg msg = (AckMsg) m;

                if (requests.containsKey(msg.sender)) {
                    result.add(new Pair<>(requests.get(msg.sender), new AckMsg(msg.sender)));
                    requests.remove(msg.getOriginalSender());
                }

                ackWaitingProcess.remove(msg.getOriginalSender());
                ackWaitingProcess.remove(msg.sender);
                reAckWaitingProcess.remove(msg.getOriginalSender());
                reAckWaitingProcess.remove(msg.sender);
            } else if (m instanceof PFDMessage) {
                PFDMessage msg = (PFDMessage) m;

            } else if (m instanceof DeadProcessMessage) {
                DeadProcessMessage msg = (DeadProcessMessage) m;
                ackWaitingProcess.remove(msg.getProcessID());
                reAckWaitingProcess.remove(msg.getProcessID());
                otherProcesses.remove(msg.getProcessID());
                requests.remove(msg.getProcessID());
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
