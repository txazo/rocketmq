package org.apache.rocketmq.test.cluster;

import org.apache.rocketmq.debug.NodeNameHolder;

public class RocketMQCluster {

    private static final String CLUSTER_HOME = getClusterHome();

    private String nodeName;
    private String userHome;
    protected String configFile;

    public RocketMQCluster(String nodeName) {
        this.nodeName = nodeName;
        this.userHome = CLUSTER_HOME + "/" + nodeName;
    }

    public void start() throws Exception {
        System.setProperty("user.home", userHome);
        System.setProperty("rocketmq.home.dir", userHome);
        NodeNameHolder.setNodeName(nodeName);
    }

    protected String getConfigFilePath() {
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
