package org.apache.rocketmq.test.cluster;

import org.apache.rocketmq.namesrv.NamesrvStartup;

public class RocketMQCluster {

    private String userHome;
    protected String configFile;

    public RocketMQCluster(String userHome) {
        this.userHome = userHome;
    }

    public void start() throws Exception {
        System.setProperty("user.home", userHome);
        System.setProperty("rocketmq.home.dir", userHome);
        NamesrvStartup.main(new String[]{"-c", getConfigFilePath()});
    }

    private String getConfigFilePath() {
        return userHome + "/" + configFile;
    }

}
