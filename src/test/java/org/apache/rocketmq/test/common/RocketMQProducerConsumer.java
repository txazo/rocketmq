package org.apache.rocketmq.test.common;

import org.apache.rocketmq.client.MQAdmin;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MQConsumer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.debug.NodeNameHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RocketMQProducerConsumer {

    private int consumerSize = 1;
    private String producerName = "producer";
    private String consumerName = "consumer";
    private String namesrvAddr = "127.0.0.1:9876";
    private String groupName;
    private String topicName;
    private Thread producerThread;
    private Thread consumerThread;
    private RocketMQProducer producer;
    private RocketMQFactory producerFactory;
    private RocketMQFactory consumerFactory;
    private RocketMQExecutor producerExecutor;
    private RocketMQExecutor consumerExecutor;
    private List<RocketMQConsumer> consumers;

    public RocketMQProducerConsumer() {
    }

    public RocketMQProducerConsumer consumerSize(int consumerSize) {
        this.consumerSize = consumerSize;
        return this;
    }

    public RocketMQProducerConsumer namesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
        return this;
    }

    public RocketMQProducerConsumer group(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public RocketMQProducerConsumer topic(String topicName) {
        this.topicName = topicName;
        return this;
    }

    public <T extends MQProducer> RocketMQProducerConsumer producer(Class<T> producerClass, RocketMQExecutor<T> executor) {
        producerExecutor = executor;
        producerFactory = new RocketMQFactory(producerClass);
        return this;
    }

    public <T extends MQConsumer> RocketMQProducerConsumer consumer(Class<T> consumerClass, RocketMQExecutor<T> executor) {
        consumerExecutor = executor;
        consumerFactory = new RocketMQFactory(consumerClass);
        return this;
    }

    public RocketMQProducerConsumer start() throws Exception {
        consumers = new ArrayList<>(consumerSize);
        for (int i = 0; i < consumerSize; i++) {
            RocketMQConsumer consumer = new RocketMQConsumer(consumerSize == 1 ? consumerName : (consumerName + "-" + i), consumerFactory, consumerExecutor);
            consumers.add(consumer);
            Thread consumerThread = new Thread(consumer);
            consumerThread.start();
            consumerThread.join();
        }

        producer = new RocketMQProducer(producerName, producerFactory, producerExecutor);
        producerThread = new Thread(producer);
        producerThread.start();
        return this;
    }

    public void waitUtilClose() throws Exception {
        producerThread.join();
        for (RocketMQConsumer consumer : consumers) {
            consumer.shutdown();
        }
        producer.shutdown();
    }

    private abstract class RocketMQAdmin<T extends MQAdmin> implements Runnable {

        private String nodeName;
        protected T admin;
        protected RocketMQFactory<T> factory;
        protected RocketMQExecutor<T> executor;

        public RocketMQAdmin(String nodeName, RocketMQFactory<T> factory, RocketMQExecutor<T> executor) {
            this.nodeName = nodeName;
            this.factory = factory;
            this.executor = executor;
        }

        public void init() throws Exception {
            NodeNameHolder.setNodeName(nodeName);

            admin = factory.newInstance(groupName);
            callMethod(admin, "setNamesrvAddr", new Class<?>[]{String.class}, namesrvAddr);

            if (admin instanceof DefaultMQPushConsumer) {
                ((DefaultMQPushConsumer) admin).setConsumeThreadMin(8);
                ((DefaultMQPushConsumer) admin).setConsumeThreadMax(8);
            }

            executor.init(admin, topicName);
        }

        public void start() throws Exception {
            callMethod(admin, "start", new Class<?>[]{});
        }

        public void execute() throws Exception {
            executor.execute(admin, topicName);
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

        public RocketMQProducer(String nodeName, RocketMQFactory<T> factory, RocketMQExecutor<T> executor) {
            super(nodeName, factory, executor);
        }

    }

    private class RocketMQConsumer<T extends MQConsumer> extends RocketMQAdmin<T> {

        public RocketMQConsumer(String nodeName, RocketMQFactory<T> factory, RocketMQExecutor<T> executor) {
            super(nodeName, factory, executor);
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
