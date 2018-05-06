package cz.cvut.fel.agents.pdv.exclusion;

import cz.cvut.fel.agents.pdv.clocked.ClockedMessage;
import cz.cvut.fel.agents.pdv.clocked.ClockedProcess;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class ExclusionPrimitive {

    /**
     * Stavy, ve kterych se zamek muze nachazet.
     */
    enum AcquisitionState {
        RELEASED,    // Uvolneny   - Proces, ktery vlastni aktualni instanci ExclusionPrimitive o pristup do kriticke
        //              sekce nezada

        WANTED,      // Chteny     - Proces, ktery vlastni aktualni instanci ExclusionPrimitive zada o pristup do
        //              kriticke sekce. Ten mu ale zatim nebyl odsouhlasen ostatnimi procesy.

        HELD         // Vlastneny  - Procesu bylo prideleno pravo pristupovat ke sdilenemu zdroji.
    }

    private ClockedProcess owner;            // Proces, ktery vlastni aktualni instanci ExclusionPrimitive

    private String criticalSectionName;      // Nazev kriticke sekce. POZOR! V aplikaci se muze nachazet vice kritickych
    // sekci s ruznymi nazvy!

    private List<String> allAccessingProcesses;  // Seznam vsech procesu, ktere mohou chtit pristupovat ke kriticke sekci
    // s nazvem 'criticalSectionName' (a tak spolurozhoduji o udelovani pristupu)

    private AcquisitionState state;          // Aktualni stav zamku (vzhledem k procesu 'owner').
    // state==HELD znamena, ze proces 'owner' muze vstoupit do kriticke sekce

    // Doplnte pripadne vlastni datove struktury potrebne pro implementaci
    // Ricart-Agrawalova algoritmu pro vzajemne vylouceni

    public ExclusionPrimitive(ClockedProcess owner, String criticalSectionName, String[] allProcesses) {
        this.owner = owner;
        this.criticalSectionName = criticalSectionName;
        this.allAccessingProcesses = Arrays.stream(allProcesses).filter(x -> !x.equals(owner.id)).collect(Collectors.toList());

        // Na zacatku je zamek uvolneny
        this.state = AcquisitionState.RELEASED;
    }

    private Queue<ClockedMessage> onHoldInbox = new LinkedList<>();
    private Set<String> noResponseYet = new HashSet<>();

    private int wantedTime = Integer.MAX_VALUE;

    private void incrementAndSend(String rcp, ClockedMessage msg) {
        owner.increaseTime();
        owner.send(rcp, msg);
    }

    /**
     * Metoda pro zpracovani nove prichozi zpravy
     *
     * @param m prichozi zprava
     * @return 'true', pokud je zprava 'm' relevantni pro aktualni kritickou sekci.
     */
    public boolean accept(ClockedMessage m) {
        if (!m.recipient.equals(owner.id)) return false;
        if (m instanceof RequestMsg) {
            RequestMsg msg = (RequestMsg) m;
            if (!msg.criticalSectionName.equals(criticalSectionName)) return false;

            if (state == AcquisitionState.RELEASED) {
                incrementAndSend(m.sender, new OkMsg(criticalSectionName));
            } else {
                if (m.sentOn < wantedTime || (m.sentOn == wantedTime && m.sender.compareTo(owner.id) < 0)) {
                    incrementAndSend(m.sender, new OkMsg(criticalSectionName));
                } else {
                    onHoldInbox.add(m);
                }
            }
            return true;
        } else if (m instanceof OkMsg) {
            OkMsg msg = (OkMsg) m;
            if (!msg.criticalSectionName.equals(criticalSectionName)) return false;
            noResponseYet.remove(m.sender);

            if (noResponseYet.size() == 0 && state == AcquisitionState.WANTED) {
                state = AcquisitionState.HELD;
            }
            return true;
        }

        return false;
        // Implementujte zpracovani prijimane zpravy informujici
        // o pristupech ke sdilenemu zdroji podle Ricart-Agrawalova
        // algoritmu. Pokud potrebujete posilat specificke zpravy,
        // vytvorte si pro ne vlastni tridy.
        //
        // POZOR! Ne vsechny zpravy, ktere v teto metode dostanete Vas
        // budou zajimat! Budou Vam prichazet i zpravy, ktere se  napriklad
        // tykaji jinych kritickych sekci. Pokud je zprava nerelevantni, tak
        // ji nezpracovavejte a vratte navratovou hodnotu 'false'. Nekdo jiny
        // se o ni urcite postara :-)
        //
        // Nezapomente se starat o cas procesu 'owner'
        // pomoci metody owner.increaseTime(). Aktualizaci
        // logickeho casu procesu s prijatou zpravou (pomoci maxima) jsme
        // za Vas jiz vyresili.
        //
        // Cas poslani prijate zpravy muzete zjistit dotazem na hodnotu
        // m.sentOn. Aktualni logicky cas muzete zjistit metodou owner.getTime().
        // Zpravy se posilaji pomoci owner.send(prijemce, zprava) a je jim auto-
        // maticky pridelen logicky cas odeslani. Retezec identifikujici proces
        // 'owner' je ulozeny v owner.id.
    }

    public void requestEnter() {
        owner.increaseTime();
        for (String process : allAccessingProcesses) {
            noResponseYet.add(process);
            owner.send(process, new RequestMsg(criticalSectionName));
        }
        state = AcquisitionState.WANTED;
        wantedTime = owner.getTime();

        // Implementujte zadost procesu 'owner' o pristup
        // ke sdilenemu zdroji 'criticalSectionName'
    }

    public void exit() {
        // Implementuje uvolneni zdroje, aby k nemu meli pristup i
        // ostatni procesy z 'allAccessingProcesses', ktere ke zdroji
        // mohou chtit pristupovat

        wantedTime = Integer.MAX_VALUE;
        state = AcquisitionState.RELEASED;

        while (!onHoldInbox.isEmpty()) {
            owner.increaseTime();
            accept(onHoldInbox.poll());
        }
        System.out.println("EXIT from - "+ owner.id);
    }

    public String getName() {
        return criticalSectionName;
    }

    public boolean isHeld() {
        return state == AcquisitionState.HELD;
    }

}

class RequestMsg extends ClockedMessage {
    String criticalSectionName;

    public RequestMsg(String criticalSectionName) {
        this.criticalSectionName = criticalSectionName;
    }
}

class OkMsg extends ClockedMessage {
    String criticalSectionName;

    public OkMsg(String criticalSectionName) {
        this.criticalSectionName = criticalSectionName;
    }
}
