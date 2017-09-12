package org.apache.rocketmq.test;

import org.apache.rocketmq.broker.BrokerStartup;

public class RocketMQBrokerStartupTest {

    public static void main(String[] args) throws Exception {
        RocketMQInit.init();
        BrokerStartup.main("-n 127.0.0.1:9876".split("\\s"));
    }

}
