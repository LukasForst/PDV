package cz.cvut.fel.agents.pdv.pdv_cloud.scalar;

import cz.cvut.fel.agents.pdv.pdv_cloud.cloud_utils.IClock;
import cz.cvut.fel.agents.pdv.pdv_cloud.cloud_utils.IProcessClock;

/**
 * Trida reprezentujici skalarni hodiny procesu.
 */
public class ScalarClock implements IProcessClock<Integer> {
    private int logicalTime = 0;

    @Override
    public void update(IClock<Integer> timestamp) {
        logicalTime = Math.max(timestamp.getTime(), logicalTime) + 1;
    }

    @Override
    public void onNewEvent() {
        logicalTime++;
    }

    @Override
    public IClock<Integer> getAsTimeStamp() {
        return new ScalarTimestamp(logicalTime);
    }

    @Override
    public Integer getTime() {
        return logicalTime;
    }

    @Override
    public String toString() {
        return "" + logicalTime;
    }

    @Override
    public boolean isCausalityForProcessViolated(IClock<Integer> timestamp, int process) {

        // TODO
        // implementujte detekci poruseni kauzality na zaklade
        // porovnani lokalnich hodin a casove znamky zpravy od procesu

        return timestamp.getTime() < logicalTime;
    }

}
