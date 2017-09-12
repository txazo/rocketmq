package org.apache.rocketmq.test;

import org.apache.rocketmq.broker.BrokerStartup;

public class RocketMQBrokerStartupTest {

    /**
     * ROCKETMQ_HOME=/Users/txazo/TxazoProject/rocketmq/rocketmq
     */
    public static void main(String[] args) {
        BrokerStartup.main("-n 127.0.0.1:9876".split("\\s"));
    }

}
