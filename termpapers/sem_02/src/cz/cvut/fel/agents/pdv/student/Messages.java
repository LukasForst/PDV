package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    final String requestId;

    AppendEntry(int nextIndex, int epoch, LogItem lastItem, Pair<StoreOperationEnums, Pair<String, String>> operation, String requestId) {
        super(nextIndex, epoch, lastItem);
        this.operation = new Pair<>(operation.getFirst(), new Pair<>(operation.getSecond().getFirst(), operation.getSecond().getSecond()));
        this.requestId = requestId;
    }

    AppendEntry(DatabaseProvider dbProvider, Pair<StoreOperationEnums, Pair<String, String>> operation, String requestId) {
        super(dbProvider);
        this.operation = new Pair<>(operation.getFirst(), new Pair<>(operation.getSecond().getFirst(), operation.getSecond().getSecond()));
        this.requestId = requestId;
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
    final String requestId;

    AppendEntryResponse(boolean canPerformOperation, int nextIndex, int epoch, LogItem lasItemInLog, String requestId) {
        super(nextIndex, epoch, lasItemInLog);
        this.canPerformOperation = canPerformOperation;
        this.requestId = requestId;
    }

    AppendEntryResponse(DatabaseProvider provider, boolean canPerformOperation, String requestId) {
        super(provider);
        this.canPerformOperation = canPerformOperation;
        this.requestId = requestId;
    }
}

class RecreateLogAndDataDeepCopy extends RaftMessage {
    final List<LogItem> log;
    final Map<String, String> data;

    RecreateLogAndDataDeepCopy(int nextIndex, int epoch, LogItem lasItemInLog, List<LogItem> log, Map<String, String> data) {
        super(nextIndex, epoch, lasItemInLog);
        this.log = log.stream().map(x -> new LogItem(x.epoch, x.index, x.operation, new Pair<>(x.data.getFirst(), x.data.getSecond()))).collect(Collectors.toList());
        this.data = new HashMap<>(data);
    }

    RecreateLogAndDataDeepCopy(DatabaseProvider dbProvider) {
        this(dbProvider.getNextIndex(), dbProvider.getEpoch(), dbProvider.getLastLogItem(), dbProvider.log, dbProvider.data);
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
