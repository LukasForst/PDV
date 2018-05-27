package cz.cvut.fel.agents.pdv.student;

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

    private boolean compareLastLog(int nextIndex, int epoch, LogItem lastLogItem) {
        return nextIndex == log.size() && this.epoch == epoch && LogItem.equals(lastLogItem, getLastLogItem());
    }

    boolean compareLastLog(RaftMessage msg) {
        return compareLastLog(msg.nextIndex, msg.epoch, msg.lasItemInLog);
    }

    void addNonVerified(AppendEntry msg) {
        assert msg.operation.getFirst() != StoreOperationEnums.GET;
        notVerified = new LogItem(getEpoch(), getNextIndex(), msg.operation.getFirst(), msg.operation.getSecond(), msg.requestId);
    }

    void writeNonVerified_UNSAFE() {
        writeNonVerified(notVerified);
    }

    private void writeNonVerified(LogItem lastItemInLog) {
        assert notVerified != null;
        assert LogItem.equals(lastItemInLog, notVerified);
        assert notVerified.operation != StoreOperationEnums.GET;

        write(notVerified);
        notVerified = null;
    }

    private void write(LogItem toWrite) {
        log.add(toWrite);
        if (toWrite.operation == StoreOperationEnums.PUT) {
            data.put(toWrite.data.getFirst(), toWrite.data.getSecond());
        } else if (toWrite.operation == StoreOperationEnums.APPEND) {
            data.put(toWrite.data.getFirst(), data.getOrDefault(toWrite.data.getFirst(), "") + toWrite.data.getSecond());
        }
    }

    LogItem getNotVerified() {
        return notVerified;
    }

    void writeNonVerified(AppendEntryConfirmed msg) {
        writeNonVerified(msg.lasItemInLog);
    }

    String dataGetOrDefault(String key) {
        return data.getOrDefault(key, null);
    }

    LogItem getLastLogItem() {
        return log.size() == 0 ? null : log.get(log.size() - 1);
    }

    void appendTheseFromIndex(AppendTheseFromIndex msg) {
        data.clear();

        Queue<LogItem> newLog = new LinkedList<>();
        for (int i = 0; i < msg.startIndex && i < log.size(); i++) {
            newLog.add(new LogItem(log.get(i)));
        }
        log.clear();

        while (!msg.logs.isEmpty()) {
            newLog.add(new LogItem(Objects.requireNonNull(msg.logs.poll())));
        }

        while (!newLog.isEmpty()) {
            write(newLog.poll());
        }
    }


}
