package org.apache.rocketmq.test;

public abstract class NamesrvAddr {

    private static final String NamesrvAddr = "127.0.0.1:9876";

    public static String get() {
        return NamesrvAddr;
    }

}
