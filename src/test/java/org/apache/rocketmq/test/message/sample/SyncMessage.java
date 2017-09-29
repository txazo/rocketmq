package org.apache.rocketmq.test.message.sample;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.test.common.RocketMQProducerConsumer;

public class SyncMessage {

    public static void main(String[] args) throws Exception {
        RocketMQProducerConsumer producerConsumer = new RocketMQProducerConsumer();
        producerConsumer.namesrvAddr("127.0.0.1:9876;127.0.0.1:9877")
                .group("group-test-1")
                .topic("topic-test-1")
                .producer(DefaultMQProducer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQProducer>() {

                    @Override
                    public void execute(DefaultMQProducer producer, String topicName) throws Exception {
                        Message message = new Message(topicName, "message".getBytes(RemotingHelper.DEFAULT_CHARSET));
                        producer.send(message);
                    }

                }).start().waitUtilClose();
    }

}
