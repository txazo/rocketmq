package org.apache.rocketmq.debug;

public abstract class NodeNameHolder {

    public static String getNodeName() {
        return System.getProperty("rocketmq.nodeName");
    }

    public static void setNodeName(String nodeName) {
        System.setProperty("rocketmq.nodeName", nodeName);
    }

}
