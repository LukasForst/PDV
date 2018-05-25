package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;

import java.util.*;

class DatabaseProvider {
    final List<LogItem> log = new LinkedList<>();
    final Map<String, String> data = new HashMap<>();

    private LogItem notVerified = null;

    private int epoch = 0;

    Optional<Map<String, String>> getLastSnapshotOfLog() {
        if (data.isEmpty()) return Optional.empty();
        return Optional.of(new HashMap<>(data));
    }

    int getEpoch() {
        return epoch;
    }

    int getNextIndex() {
        return log.size();
    }


    void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    void increaseEpoch() {
        epoch++;
    }

    boolean compareLastLog(int nextIndex, int epoch, LogItem lastLogItem) {
        return nextIndex == log.size() && this.epoch == epoch && LogItem.equals(lastLogItem, getLastLogItem());
    }

    boolean compareLastLog(RaftMessage msg) {
        return compareLastLog(msg.nextIndex, msg.epoch, msg.lasItemInLog);
    }

    void addNonVerified(AppendEntry msg) {
        assert msg.operation.getFirst() != StoreOperationEnums.GET;
        notVerified = new LogItem(getEpoch(), getNextIndex(), msg.operation.getFirst(), msg.operation.getSecond());
    }

    void writeNonVerified_UNSAFE() {
        writeNonVerified(notVerified);
    }

    private void writeNonVerified(LogItem lastItemInLog) {
        if (LogItem.equals(lastItemInLog, notVerified)) {
            assert notVerified != null;
            assert notVerified.operation != StoreOperationEnums.GET;

            log.add(notVerified);
            if (notVerified.operation == StoreOperationEnums.PUT) {
                data.put(notVerified.data.getFirst(), notVerified.data.getSecond());
            } else if (notVerified.operation == StoreOperationEnums.APPEND) {
                data.put(notVerified.data.getFirst(), data.getOrDefault(notVerified.data.getFirst(), "") + notVerified.data.getSecond());
            }
            notVerified = null;
        }
    }

    void writeNonVerified(AppendEntryConfirmed msg) {
        writeNonVerified(msg.lasItemInLog);
    }

    void replicateLog(RecreateLogAndDataDeepCopy msg) {
        log.clear();
        data.clear();
        epoch = msg.epoch;
        log.addAll(msg.log);
        data.putAll(msg.data);
    }

    String dataGetOrDefault(String key) {
        return data.getOrDefault(key, null);
    }

    Pair<Boolean, String> performReadOperation(StoreOperationEnums operation, Pair<String, String> content) {
        if (operation == StoreOperationEnums.GET) {
            return new Pair<>(true, data.getOrDefault(content.getFirst(), null));
        }
        return new Pair<>(false, null);
    }

    LogItem getLastLogItem() {
        return log.size() == 0 ? null : log.get(log.size() - 1);
    }

    void executeLastOnData() {
        LogItem it = getLastLogItem();
        executeOnData(it.operation, it.data);
    }

    void executeOnData(StoreOperationEnums operation, Pair<String, String> content) {
        switch (operation) {
            case PUT:
                data.put(content.getFirst(), content.getSecond());
                break;
            case APPEND:
                data.put(content.getFirst(), data.getOrDefault(content.getFirst(), "") + content.getSecond());
                break;
        }
    }

    boolean performLogWriteOperation(StoreOperationEnums operation, Pair<String, String> content, LogItem lastLogItem) {
        if (!canPerform(lastLogItem)) return false;
        log.add(new LogItem(epoch, log.size(), operation, content));
        return true;
    }

    private boolean canPerform(LogItem last) {
        if (log.isEmpty() && last == null) return true;
        if (log.isEmpty()) return false;
        return LogItem.equals(getLastLogItem(), last);
    }
}
