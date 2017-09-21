package org.apache.rocketmq.test.stat;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandLogStat {

    private static final int MAX_LINE = 1000;

    public static void main(String[] args) throws Exception {
        stat("cluster/namesrv-2/logs/rocketmqlogs/command.log", "tmp/namesrv");
        stat("cluster/broker-2-slave/logs/rocketmqlogs/command.log", "tmp/broker");
    }

    private static void stat(String input, String ouput) throws Exception {
        init(ouput);

        Map<String, List<String>> types = readLog(input);

        for (Map.Entry<String, List<String>> entry : types.entrySet()) {
            saveToFile(ouput, entry.getKey(), entry.getValue());
        }
    }

    private static void init(String ouput) throws Exception {
        File file = new File(getProjectHome() + "/" + ouput);
        FileUtils.deleteDirectory(file);
        FileUtils.forceMkdir(file);
    }

    private static Map<String, List<String>> readLog(String input) throws Exception {
        Map<String, List<String>> types = new HashMap<>();
        String type = null;
        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(getProjectHome() + "/" + input))) {
            while ((line = br.readLine()) != null) {
                type = getType(line);
                if (!types.containsKey(type)) {
                    types.put(type, new ArrayList<String>());
                }
                if (types.get(type).size() >= MAX_LINE) {
                    continue;
                }
                types.get(type).add(line);
            }
        }
        return types;
    }

    private static void saveToFile(String ouput, String type, List<String> lines) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(getProjectHome() + "/" + ouput + "/" + type))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }

    private static String getType(String line) {
        String[] array = line.split("\\s+");
        return array[6] + ":" + array[5] + "@" + array[7];
    }

    private static String getProjectHome() {
        String path = CommandLogStat.class.getResource("/").getPath();
        int index = path.indexOf("/target");
        if (index > -1) {
            return path.substring(0, index);
        }
        return path;
    }

}
