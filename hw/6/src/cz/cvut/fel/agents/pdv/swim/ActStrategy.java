package cz.cvut.fel.agents.pdv.swim;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Trida s implementaci metody act() pro proces Failure Detector. Tuto tridu (a tridy pouzivanych zprav) budete
 * odevzdavat. Do tridy si muzete doplnit vlastni pomocne datove struktury. Hodnoty muzete inicializovat primo
 * v konstruktoru. Klicova je metoda act(), kterou vola kazda instance tridy FailureDetectorProcess ve sve metode
 * act(). Tuto metodu naimplementujte podle protokolu SWIM predstaveneho na prednasce.
 *
 * Pokud si stale jeste nevite rady s frameworkem, inspiraci muzete nalezt v resenych prikladech ze cviceni.
 */
public class ActStrategy {

    // maximalni zpozdeni zprav
    private final int maxDelayForMessages;
    private final List<String> otherProcesses;

    // Definujte vsechny sve promenne a datove struktury, ktere budete potrebovat

    public ActStrategy(int maxDelayForMessages, List<String> otherProcesses,
                       int timeToDetectKilledProcess, int upperBoundOnMessages) {
        this.maxDelayForMessages = maxDelayForMessages;
        this.otherProcesses = otherProcesses;

        // Doplne inicializaci
    }

    /**
     * Metoda je volana s kazdym zavolanim metody act v FailureDetectorProcess. Metodu implementujte
     * tak, jako byste implementovali metodu act() v FailureDetectorProcess, misto pouzivani send()
     * pridejte zpravy v podobe paru - prijemce, zprava do listu. Zpravy budou nasledne odeslany.
     * <p>
     * Diky zavedeni teto metody muzeme kontrolovat pocet odeslanych zprav vasi implementaci.
     */
    public List<Pair<String, Message>> act(Queue<Message> inbox, String disseminationProcess) {

        // Od DisseminationProcess muzete dostat zpravu typu DeadProcessMessage, ktera Vas
        // informuje o spravne detekovanem ukoncenem procesu.
        // DisseminationProcess muzete poslat zpravu o detekovanem "mrtvem" procesu.
        // Zprava musi byt typu PFDMessage.


        return null;
    }

}
