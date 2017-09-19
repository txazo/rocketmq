package org.apache.rocketmq.test.cluster;

import java.util.ArrayList;
import java.util.List;

public class RocketMQBrokerClusterTest {

    public static void main(String[] args) throws Exception {
        String namesrvAddr = "127.0.0.1:9876;127.0.0.1:9877";

        List<String> brokerHomes = new ArrayList<>();
        brokerHomes.add("/Users/txazo/TxazoProject/rocketmq/cluster/broker-1-master");
        brokerHomes.add("/Users/txazo/TxazoProject/rocketmq/cluster/broker-1-slave");
        brokerHomes.add("/Users/txazo/TxazoProject/rocketmq/cluster/broker-2-master");
        brokerHomes.add("/Users/txazo/TxazoProject/rocketmq/cluster/broker-2-slave");

        for (String brokerHome : brokerHomes) {
            new RocketMQBroker(brokerHome, namesrvAddr).start();
            System.out.printf("[Broker] Node started: %s%n%n", brokerHome);
        }

        System.out.println("[Broker] Cluster started");
    }

}
