package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;

import java.util.Queue;

public abstract class Stage {
    final ClusterProcess process;
    final DatabaseProvider dbProvider;

    public Stage(ClusterProcess process, String leader, int epoch) {
        this.process = process;
        this.process.currentLeader = leader;
        this.dbProvider = process.dbProvider;
        this.dbProvider.setEpoch(epoch);
    }

    void send(String x, Message m){
        process.sendMessage(x, m);
    }

    abstract Stage act(Queue<Message> inbox);
}
