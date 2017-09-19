package org.apache.rocketmq.test.cluster;

public class RocketMQNamesrv extends RocketMQCluster {

    public RocketMQNamesrv(String userHome) {
        super(userHome);
        this.configFile = "conf/namesrv.conf";
    }

}
