package org.apache.rocketmq.debug;

public abstract class RocketMQDebug {

    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("rocketmq-debug"));

    public static boolean debug() {
        return DEBUG;
    }

}
