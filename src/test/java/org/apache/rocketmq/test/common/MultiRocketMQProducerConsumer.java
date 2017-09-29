package org.apache.rocketmq.test.common;

import org.apache.rocketmq.client.MQAdmin;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MQConsumer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.debug.NodeNameHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiRocketMQProducerConsumer {

    private String namesrvAddr = "127.0.0.1:9876";
    private RocketMQFactory producerFactory;
    private RocketMQFactory consumerFactory;
    private RocketMQExecutor producerExecutor;
    private RocketMQExecutor consumerExecutor;
    private List<RocketMQConsumer> consumers = new ArrayList<>();
    private List<RocketMQProducer> producers = new ArrayList<>();
    private Map<String, Integer> producerGroup = new HashMap<>();
    private Map<String, Integer> consumerGroup = new HashMap<>();

    public MultiRocketMQProducerConsumer() {
    }

    public MultiRocketMQProducerConsumer producerGroup(String topicName, int producerSize) {
        if (producerSize > 0) {
            this.producerGroup.put(topicName, producerSize);
        }
        return this;
    }

    public MultiRocketMQProducerConsumer consumerGroup(String groupName, int consumerSize) {
        if (consumerSize > 0) {
            this.consumerGroup.put(groupName, consumerSize);
        }
        return this;
    }

    public MultiRocketMQProducerConsumer namesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
        return this;
    }

    public <T extends MQProducer> MultiRocketMQProducerConsumer producer(Class<T> producerClass, RocketMQExecutor<T> executor) {
        producerExecutor = executor;
        producerFactory = new RocketMQFactory(producerClass);
        return this;
    }

    public <T extends MQConsumer> MultiRocketMQProducerConsumer consumer(Class<T> consumerClass, RocketMQExecutor<T> executor) {
        consumerExecutor = executor;
        consumerFactory = new RocketMQFactory(consumerClass);
        return this;
    }

    public MultiRocketMQProducerConsumer start() throws Exception {
        if (consumerGroup.size() > 0 && consumerFactory != null && consumerExecutor != null) {
            for (Map.Entry<String, Integer> entry : consumerGroup.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    RocketMQConsumer consumer = new RocketMQConsumer(entry.getKey() + "-" + i, entry.getKey(), consumerFactory, consumerExecutor);
                    consumers.add(consumer);
                    new Thread(consumer).start();
                }
            }
        }

        if (producerGroup.size() > 0 && producerFactory != null && producerExecutor != null) {
            for (Map.Entry<String, Integer> entry : consumerGroup.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    RocketMQProducer producer = new RocketMQProducer(entry.getKey() + "-" + i, entry.getKey(), producerFactory, producerExecutor);
                    producers.add(producer);
                    new Thread(producer).start();
                }
            }
        }

        return this;
    }

    public synchronized void waitUtilClose() throws Exception {
        wait();
    }

    private abstract class RocketMQAdmin<T extends MQAdmin> implements Runnable {

        private String nodeName;
        private String group;
        protected T admin;
        protected RocketMQFactory<T> factory;
        protected RocketMQExecutor<T> executor;

        public RocketMQAdmin(String nodeName, String group, RocketMQFactory<T> factory, RocketMQExecutor<T> executor) {
            this.nodeName = nodeName;
            this.group = group;
            this.factory = factory;
            this.executor = executor;
        }

        public void init() throws Exception {
            NodeNameHolder.setNodeName(nodeName);

            admin = factory.newInstance(group);
            callMethod(admin, "setNamesrvAddr", new Class<?>[]{String.class}, namesrvAddr);

            if (admin instanceof DefaultMQPushConsumer) {
                ((DefaultMQPushConsumer) admin).setConsumeThreadMin(8);
                ((DefaultMQPushConsumer) admin).setConsumeThreadMax(8);
            }

            executor.init(admin, group);
        }

        public void start() throws Exception {
            callMethod(admin, "start", new Class<?>[]{});
        }

        public void execute() throws Exception {
            executor.execute(admin, group);
        }

        public void shutdown() throws Exception {
            callMethod(admin, "shutdown", new Class<?>[]{});
        }

        @Override
        public void run() {
            try {
                init();
                start();
                execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private class RocketMQProducer<T extends MQProducer> extends RocketMQAdmin<T> {

        public RocketMQProducer(String nodeName, String group, RocketMQFactory<T> factory, RocketMQExecutor<T> executor) {
            super(nodeName, group, factory, executor);
        }

    }

    private class RocketMQConsumer<T extends MQConsumer> extends RocketMQAdmin<T> {

        public RocketMQConsumer(String nodeName, String group, RocketMQFactory<T> factory, RocketMQExecutor<T> executor) {
            super(nodeName, group, factory, executor);
        }

    }

    private static class RocketMQFactory<T extends MQAdmin> {

        private final Class<T> clazz;

        public RocketMQFactory(Class<T> clazz) {
            this.clazz = clazz;
        }

        public T newInstance(String groupName) throws Exception {
            Constructor c = clazz.getDeclaredConstructor(String.class);
            return (T) c.newInstance(groupName);
        }

    }

    public static abstract class RocketMQExecutor<T extends MQAdmin> {

        public void init(T t, String topicName) throws Exception {
        }

        public void execute(T t, String topicName) throws Exception {
        }

    }

    public static Object callMethod(Object target, String methodName, Class<?>[] argsTypes, Object... args) throws Exception {
        return getMethod(target.getClass(), methodName, argsTypes).invoke(target, args);
    }

    public static Method getMethod(Class<?> targetClass, String methodName, Class<?>... argsTypes) throws Exception {
        Method method = targetClass.getMethod(methodName, argsTypes);
        method.setAccessible(true);
        return method;
    }

}
