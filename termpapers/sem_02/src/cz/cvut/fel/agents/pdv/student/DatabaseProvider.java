package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;

import java.util.*;

class DatabaseProvider {
    final List<LogItem> log = new LinkedList<>();
    final Map<String, String> data = new HashMap<>();
    private int epoch = 0;

    Optional<Map<String, String>> getLastSnapshotOfLog() {
        if (data.isEmpty()) return Optional.empty();
        return Optional.of(new HashMap<>(data));
    }

    int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    void increaseEpoch() {
        epoch++;
    }

    Pair<Boolean, String> performReadOperation(StoreOperationEnums operation, Pair<String, String> content) {
        if (operation == StoreOperationEnums.GET) {
            return new Pair<>(true, data.getOrDefault(content.getFirst(), null));
        }
        return new Pair<>(false, null);
    }

    LogItem getLastLogItem(){
        return log.size() == 0 ? null : log.get(log.size() - 1);
    }

    void executeLastOnData(){
        LogItem it = getLastLogItem();
        executeOnData(it.operation, it.value);
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
