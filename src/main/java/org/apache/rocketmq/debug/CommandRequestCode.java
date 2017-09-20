package org.apache.rocketmq.debug;

import org.apache.rocketmq.common.protocol.RequestCode;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class CommandRequestCode {

    private static final Map<Integer, String> CODES = new HashMap<>();

    static {
        try {
            Field[] fields = RequestCode.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                CODES.put(field.getInt(null), field.getName().toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String command(int code) {
        return CODES.get(code);
    }

}
