package org.apache.rocketmq.test.cluster;

import org.apache.rocketmq.broker.BrokerStartup;

public class RocketMQBroker extends RocketMQCluster {

    private String namesrvAddr;

    public RocketMQBroker(String userHome, String namesrvAddr) {
        super(userHome);
        this.namesrvAddr = namesrvAddr;
        this.configFile = "conf/broker.conf";
    }

    @Override
    public void start() throws Exception {
        super.start();
        System.setProperty("rocketmq.namesrv.addr", namesrvAddr);
        BrokerStartup.main(new String[]{"-c", getConfigFilePath()});
    }

}
