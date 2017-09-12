package org.apache.rocketmq.test;

import org.apache.rocketmq.common.MixAll;

public class RocketMQEnvInit {

    public static void init() throws Exception {
        String rocketHome = getRocketMQHome();
        System.setProperty(MixAll.ROCKETMQ_HOME_PROPERTY, rocketHome);

        System.out.println("ROCKETMQ_HOME: " + rocketHome);
        System.out.println("");
    }

    private static String getRocketMQHome() {
        String path = RocketMQEnvInit.class.getResource("/").getPath();
        int index = path.indexOf("/target");
        if (index > -1) {
            return path.substring(0, index) + "/rocketmq";
        }
        return path;
    }

}
