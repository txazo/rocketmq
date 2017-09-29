package org.apache.rocketmq.debug;

import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class CommandRequestCode {

    private static final Map<Integer, String> REQUEST_CODES = new HashMap<>();
    private static final Map<Integer, String> RESPONSE_CODES = new HashMap<>();

    static {
        initCode(RequestCode.class, REQUEST_CODES);
        initCode(ResponseCode.class, RESPONSE_CODES);
    }

    private static void initCode(Class<?> classType, Map<Integer, String> codes) {
        try {
            Field[] fields = classType.getFields();
            for (Field field : fields) {
                field.setAccessible(true);
                codes.put(field.getInt(null), field.getName().toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String requestCommand(int code) {
        return REQUEST_CODES.get(code);
    }

    public static String responseCommand(int code) {
        return RESPONSE_CODES.get(code);
    }

}
