package org.apache.rocketmq.test;

import org.apache.rocketmq.debug.NodeNameHolder;
import org.apache.rocketmq.namesrv.NamesrvStartup;

public class RocketMQNamesrvStartupTest {

    public static void main(String[] args) throws Exception {
        RocketMQInit.init();
        NodeNameHolder.setNodeName("namesrv");
        NamesrvStartup.main(args);
    }

}
