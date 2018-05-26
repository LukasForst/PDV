package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;

import java.io.Serializable;

class LogItem implements Serializable {
    final int epoch;
    final int index;
    final StoreOperationEnums operation;
    final Pair<String, String> data;

    LogItem(int epoch, int index, StoreOperationEnums operation, Pair<String, String> data) {
        this.epoch = epoch;
        this.index = index;
        this.operation = operation;
        this.data = new Pair<>(data.getFirst(), data.getSecond());
    }

    LogItem(LogItem logItem) {
        this(logItem.epoch, logItem.index, logItem.operation, logItem.data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogItem logItem = (LogItem) o;

        if (epoch != logItem.epoch) return false;
        if (!operation.equals(logItem.operation)) return false;
        if (index != logItem.index) return false;
        return data.getFirst().equals(logItem.data.getFirst()) && data.getSecond().equals(logItem.data.getSecond());
    }

    @Override
    public int hashCode() {
        int result = epoch;
        result = 31 * result + data.hashCode();
        result = 31 * result + operation.hashCode();
        result = 31 * result + index;
        return result;
    }

    static boolean equals(LogItem l1, LogItem l2){
        if(l1 == null && l2 == null){
            return true;
        } else if(l1 == null || l2 == null){
            return false;
        } else {
            return l1.equals(l2);
        }
    }
}
