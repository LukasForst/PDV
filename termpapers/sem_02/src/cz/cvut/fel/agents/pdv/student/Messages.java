package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;

import java.util.*;
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

    AppendEntry(DatabaseProvider dbProvider, Pair<StoreOperationEnums, Pair<String, String>> operation, String requestId) {
        super(dbProvider);
        this.operation = new Pair<>(operation.getFirst(), new Pair<>(operation.getSecond().getFirst(), operation.getSecond().getSecond()));
        this.requestId = requestId;
    }
}

class AppendEntryConfirmed extends RaftMessage implements LeaderMessage {
    final String requestId;

    AppendEntryConfirmed(DatabaseProvider provider, String requestId) {
        super(provider);
        this.requestId = requestId;
    }
}

class AppendEntryResponse extends RaftMessage implements FollowerMessage {
    final boolean canPerformOperation;
    final String requestId;

    AppendEntryResponse(DatabaseProvider provider, boolean canPerformOperation, String requestId) {
        super(provider);
        this.canPerformOperation = canPerformOperation;
        this.requestId = requestId;
    }
}

class RecreateLogAndDataDeepCopy extends RaftMessage {
    final List<LogItem> log;
    final Map<String, String> data;
    final LogItem notConfirmed;

    RecreateLogAndDataDeepCopy(DatabaseProvider dbProvider) {
        super(dbProvider.getNextIndex(), dbProvider.getEpoch(), dbProvider.getLastLogItem());
        this.log = dbProvider.log.stream().map(LogItem::new).collect(Collectors.toList());
        this.data = new HashMap<>(dbProvider.data);
        this.notConfirmed = dbProvider.getNotVerified();
    }
}

class HearthBeat extends RaftMessage implements LeaderMessage {
    HearthBeat(DatabaseProvider provider) {
        super(provider);
    }
}

class ElectionRequest extends RaftMessage implements CandidateMessage {
    ElectionRequest(DatabaseProvider provider) {
        super(provider);
    }

}

class ElectionVote extends RaftMessage implements FollowerMessage {
    ElectionVote(DatabaseProvider provider) {
        super(provider);
    }
}

class DoYouHaveThisRecordRequest extends RaftMessage {
    DoYouHaveThisRecordRequest(int nextIndex, int epoch, LogItem lasItemInLog) {
        super(nextIndex, epoch, lasItemInLog);
    }

    DoYouHaveThisRecordRequest(LogItem logItem) {
        super(logItem.index, logItem.epoch, logItem);
    }
}

class DoYouHaveThisRecordResponse extends RaftMessage {
    final boolean response;

    DoYouHaveThisRecordResponse(LogItem logItem, boolean response) {
        super(logItem.index, logItem.epoch, logItem);
        this.response = response;
    }

}

class AppendTheseFromIndex extends Message {
    final int startIndex;
    final Queue<LogItem> logs;

    public AppendTheseFromIndex(int startIndex, Queue<LogItem> logs) {
        this.startIndex = startIndex;
        this.logs = new LinkedList<>();
        Queue<LogItem> back = new LinkedList<>(logs);
        while (!back.isEmpty()) {
            logs.add(new LogItem(Objects.requireNonNull(back.poll())));
        }
    }
}
