package org.apache.rocketmq.test.cluster;

import org.apache.rocketmq.namesrv.NamesrvStartup;

public class RocketMQCluster {

    private static final String ClusterHome = getClusterHome();

    private String userHome;
    protected String configFile;

    public RocketMQCluster(String userHome) {
        this.userHome = ClusterHome + "/" + userHome;
    }

    public void start() throws Exception {
        System.setProperty("user.home", userHome);
        System.setProperty("rocketmq.home.dir", userHome);
        NamesrvStartup.main(new String[]{"-c", getConfigFilePath()});
    }

    private String getConfigFilePath() {
        return userHome + "/" + configFile;
    }

    private static String getClusterHome() {
        String path = RocketMQCluster.class.getResource("/").getPath();
        int index = path.indexOf("/target");
        if (index > -1) {
            return path.substring(0, index) + "/cluster";
        }
        return path;
    }

}
