package org.apache.rocketmq.test.cluster;

import org.apache.rocketmq.namesrv.NamesrvStartup;

public class RocketMQNamesrv extends RocketMQCluster {

    public RocketMQNamesrv(String userHome) {
        super(userHome);
        this.configFile = "conf/namesrv.conf";
    }

    @Override
    public void start() throws Exception {
        super.start();
        NamesrvStartup.main(new String[]{"-c", getConfigFilePath()});
    }

}
