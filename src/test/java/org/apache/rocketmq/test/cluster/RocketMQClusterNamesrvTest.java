package org.apache.rocketmq.test.cluster;

import java.util.ArrayList;
import java.util.List;

public class RocketMQClusterNamesrvTest {

    public static void main(String[] args) throws Exception {
        List<String> namesrvHomes = new ArrayList<>();
        namesrvHomes.add("cluster/namesrv-1");
        namesrvHomes.add("cluster/namesrv-2");

        for (String namesrvHome : namesrvHomes) {
            new RocketMQNamesrv(namesrvHome).start();
            System.out.printf("[Namesrv] Node started: %s%n%n", namesrvHome);
        }

        System.out.println("[Namesrv] Cluster started");
    }

}
