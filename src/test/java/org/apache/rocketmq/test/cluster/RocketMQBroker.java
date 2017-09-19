package org.apache.rocketmq.test.cluster;

public class RocketMQBroker extends RocketMQCluster {

    private String namesrvAddr;

    public RocketMQBroker(String userHome, String namesrvAddr) {
        super(userHome);
        this.namesrvAddr = namesrvAddr;
        this.configFile = "conf/broker.conf";
    }

    @Override
    public void start() throws Exception {
        System.setProperty("rocketmq.namesrv.addr", namesrvAddr);
        super.start();
    }

}
