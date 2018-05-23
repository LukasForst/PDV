package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;

abstract class LeaderMessage extends Message{
    final LogItem lasItemInLog;
    final int epoch;

    protected LeaderMessage(LogItem lasItemInLog, int epoch) {
        this.lasItemInLog = lasItemInLog;
        this.epoch = epoch;
    }

    int getLastIndex(){
        return lasItemInLog == null ? 0 : lasItemInLog.index;
    }
}

abstract class FollowerMessage extends Message{

}

abstract class CandidateMessage extends Message{

}

class AppendEntry extends LeaderMessage {
    final LogItem newEntry;

    AppendEntry(LogItem lastItem, int epoch,LogItem newEntry) {
        super(lastItem, epoch);
        this.newEntry = newEntry;
    }
}

class AppendEntryConfirmed extends LeaderMessage{

    AppendEntryConfirmed(LogItem logItemToPerform, int epoch) {
        super(logItemToPerform, epoch);
    }
}

class AppendEntryResponse extends FollowerMessage{
    final boolean canPerformOperation;
    final LogItem lastLogItem;

    AppendEntryResponse(boolean canPerformOperation, LogItem lastLogItem) {
        this.canPerformOperation = canPerformOperation;
        this.lastLogItem = lastLogItem;
    }
}

class HearthBeat extends LeaderMessage{
    final int epoch;

    HearthBeat(LogItem logItem, int epoch) {
        super(logItem, epoch);
        this.epoch = epoch;
    }
}

class ElectionRequest extends CandidateMessage {
    final LogItem lastLogItem;
    final int newEpoch;

    ElectionRequest(LogItem lastLogItem, int newEpoch) {
        this.lastLogItem = lastLogItem;
        this.newEpoch = newEpoch;
    }

    ElectionRequest(int newEpoch){
        lastLogItem = null;
        this.newEpoch = newEpoch;
    }
}

class ElectionVote extends FollowerMessage{
    final int epoch;

    public ElectionVote(int epoch) {
        this.epoch = epoch;
    }
}
