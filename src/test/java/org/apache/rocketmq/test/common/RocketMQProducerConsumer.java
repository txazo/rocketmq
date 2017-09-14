package org.apache.rocketmq.test.common;

import org.apache.rocketmq.client.MQAdmin;
import org.apache.rocketmq.client.consumer.MQConsumer;
import org.apache.rocketmq.client.producer.MQProducer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CyclicBarrier;

public class RocketMQProducerConsumer {

    private String namesrvAddr;
    private String groupName;
    private String topicName;
    private Thread producerThread;
    private Thread consumerThread;
    private RocketMQProducer producer = new RocketMQProducer();
    private RocketMQConsumer consumer = new RocketMQConsumer();
    private CyclicBarrier barrier = new CyclicBarrier(2);

    public RocketMQProducerConsumer() {
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
        producer.setFactory(new RocketMQFactory(producerClass));
        producer.setExecutor(executor);
        return this;
    }

    public <T extends MQConsumer> RocketMQProducerConsumer consumer(Class<T> consumerClass, RocketMQExecutor<T> executor) {
        consumer.setFactory(new RocketMQFactory(consumerClass));
        consumer.setExecutor(executor);
        return this;
    }

    public RocketMQProducerConsumer start() throws Exception {
        producerThread = new Thread(producer);
        consumerThread = new Thread(consumer);
        producer.setOther(consumerThread);
        consumer.setOther(producerThread);
        producerThread.start();
        consumerThread.start();
        return this;
    }

    public void waitUtilClose() throws Exception {
        producerThread.join();
        consumerThread.join();
    }

    private abstract class RocketMQAdmin<T extends MQAdmin> implements Runnable {

        protected T admin;
        protected RocketMQFactory<T> factory;
        protected RocketMQExecutor<T> executor;
        protected Thread other;

        public RocketMQAdmin() {
        }

        public void init() throws Exception {
            admin = factory.newInstance(groupName);
            callMethod(admin, "setNamesrvAddr", new Class<?>[]{String.class}, new Object[]{namesrvAddr});
            executor.init(admin, topicName);
        }

        public void start() throws Exception {
            callMethod(admin, "start", new Class<?>[]{});
        }

        public void execute() throws Exception {
            barrier.await();
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
                Thread.sleep(1000 * 60 * 60);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    shutdown();
                    other.interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void setFactory(RocketMQFactory<T> factory) {
            this.factory = factory;
        }

        public void setExecutor(RocketMQExecutor<T> executor) {
            this.executor = executor;
        }

        public void setOther(Thread other) {
            this.other = other;
        }

    }

    private class RocketMQProducer<T extends MQProducer> extends RocketMQAdmin<T> {
    }

    private class RocketMQConsumer<T extends MQConsumer> extends RocketMQAdmin<T> {
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
