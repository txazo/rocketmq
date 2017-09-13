package org.apache.rocketmq.test.utils;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ReflectionUtils {

    private static final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    public static Class<?> findClass(String className) throws ClassNotFoundException {
        Class<?> clazz = classCache.get(className);
        if (clazz == null) {
            clazz = Class.forName(className);
            Class<?> previous = classCache.putIfAbsent(className, clazz);
            if (previous != null) {
                clazz = previous;
            }
        }
        return clazz;
    }

    public static Constructor findConstructor(String className, Object... args) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        Class<?> clazz = findClass(className);
        Constructor constructor = clazz.getDeclaredConstructor(getClassTypes(args));
        constructor.setAccessible(true);
        return constructor;
    }

    public static Object newInstance(String className, Object... args) throws Exception {
        return findConstructor(className, args).newInstance(args);
    }

    public static Method findMethod(Class<?> targetClass, String methodName, Class<?>... argsTypes) throws NoSuchMethodException, SecurityException {
        Method method = targetClass.getMethod(methodName, argsTypes);
        method.setAccessible(true);
        return method;
    }

    public static Object callMethod(Object target, String methodName, Class<?>[] argsTypes, Object... args)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        return findMethod(target.getClass(), methodName, argsTypes).invoke(target, args);
    }

    private static Class<?>[] getClassTypes(Object[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return new Class<?>[]{};
        }
        Class<?>[] argsTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argsTypes[i] = args[i].getClass();
        }
        return argsTypes;
    }

    public static boolean isInterfaceOf(Class<?> targetClass, Class<?> interfaceClass) {
        Class<?>[] interfaceClasses = targetClass.getInterfaces();
        if (ArrayUtils.isNotEmpty(interfaceClasses)) {
            for (Class<?> c : interfaceClasses) {
                if (interfaceClass.equals(c)) {
                    return true;
                }
            }
        }
        return false;
    }

}
