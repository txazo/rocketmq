package org.apache.rocketmq.test;

import org.apache.rocketmq.broker.BrokerStartup;
import org.apache.rocketmq.debug.NodeNameHolder;

public class RocketMQBrokerStartupTest {

    public static void main(String[] args) throws Exception {
        RocketMQInit.init();
        NodeNameHolder.setNodeName("broker");
        BrokerStartup.main("-n 127.0.0.1:9876".split("\\s"));
    }

}
