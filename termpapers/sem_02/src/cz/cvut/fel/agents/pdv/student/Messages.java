package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;

interface LeaderMessage {
}

interface FollowerMessage {
}

interface CandidateMessage {
}

abstract class RaftMessage extends Message {
    final LogItem lasItemInLog;
    final int epoch;
    final int nextIndex;

    RaftMessage(int nextIndex, int epoch, LogItem lasItemInLog) {
        this.lasItemInLog = lasItemInLog;
        this.epoch = epoch;
        this.nextIndex = nextIndex;
    }

    RaftMessage(DatabaseProvider dbProvider) {
        this(dbProvider.getNextIndex(), dbProvider.getEpoch(), dbProvider.getLastLogItem());
    }

}

class AppendEntry extends RaftMessage implements LeaderMessage {
    final Pair<StoreOperationEnums, Pair<String, String>> operation;

    AppendEntry(int nextIndex, int epoch, LogItem lastItem, Pair<StoreOperationEnums, Pair<String, String>> operation) {
        super(nextIndex, epoch, lastItem);
        this.operation = new Pair<>(operation.getFirst(), new Pair<>(operation.getSecond().getFirst(), operation.getSecond().getSecond()));
    }

    AppendEntry(DatabaseProvider dbProvider, Pair<StoreOperationEnums, Pair<String, String>> operation) {
        super(dbProvider);
        this.operation = new Pair<>(operation.getFirst(), new Pair<>(operation.getSecond().getFirst(), operation.getSecond().getSecond()));
    }
}

class AppendEntryConfirmed extends RaftMessage implements LeaderMessage {

    AppendEntryConfirmed(int nextIndex, int epoch, LogItem lasItemInLog) {
        super(nextIndex, epoch, lasItemInLog);
    }

    AppendEntryConfirmed(DatabaseProvider provider) {
        super(provider);
    }
}

class AppendEntryResponse extends RaftMessage implements FollowerMessage {
    final boolean canPerformOperation;

    AppendEntryResponse(boolean canPerformOperation, int nextIndex, int epoch, LogItem lasItemInLog) {
        super(nextIndex, epoch, lasItemInLog);
        this.canPerformOperation = canPerformOperation;
    }

    AppendEntryResponse(DatabaseProvider provider, boolean canPerformOperation) {
        super(provider);
        this.canPerformOperation = canPerformOperation;
    }
}

class HearthBeat extends RaftMessage implements LeaderMessage {
    HearthBeat(int nextIndex, int epoch, LogItem lasItemInLog) {
        super(nextIndex, epoch, lasItemInLog);
    }

    HearthBeat(DatabaseProvider provider) {
        super(provider);
    }
}

class ElectionRequest extends RaftMessage implements CandidateMessage {
    ElectionRequest(int nextIndex, int epoch, LogItem lasItemInLog) {
        super(nextIndex, epoch, lasItemInLog);
    }

    ElectionRequest(DatabaseProvider provider) {
        super(provider);
    }

}

class ElectionVote extends RaftMessage implements FollowerMessage {
    ElectionVote(int nextIndex, int epoch, LogItem lasItemInLog) {
        super(nextIndex, epoch, lasItemInLog);
    }

    ElectionVote(DatabaseProvider provider) {
        super(provider);
    }

}
