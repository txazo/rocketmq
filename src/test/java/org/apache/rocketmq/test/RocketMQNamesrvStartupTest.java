package org.apache.rocketmq.test;

import org.apache.rocketmq.namesrv.NamesrvStartup;

public class RocketMQNamesrvStartupTest {

    public static void main(String[] args) throws Exception {
        RocketMQEnvInit.init();
        NamesrvStartup.main(args);
    }

}
