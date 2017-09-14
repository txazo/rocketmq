package org.apache.rocketmq.test.message;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.test.common.RocketMQProducerConsumer;

import java.util.List;

/**
 * 延时消息
 */
public class DelayMessage {

    public static void main(String[] args) throws Exception {
        RocketMQProducerConsumer producerConsumer = new RocketMQProducerConsumer();
        producerConsumer.namesrvAddr("127.0.0.1:9876")
                .group("group-delay")
                .topic("topic-delay")
                .producer(DefaultMQProducer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQProducer>() {

                    @Override
                    public void execute(DefaultMQProducer producer, String topicName) throws Exception {
                        for (int i = 0; i <= 10; i++) {
                            Message message = new Message(topicName, ("message-" + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
                            // 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                            // 1  2  3   4   5  6  7  8  9  10 11 12 13 14  15  16  17 18
                            // level=3, 延迟10s
                            message.setDelayTimeLevel(3);
                            producer.send(message);
                            System.out.printf("生产延时消息: level=%d message=%s%n", message.getDelayTimeLevel(), new String(message.getBody()));
                            Thread.sleep(2000);
                        }
                    }

                })
                .consumer(DefaultMQPushConsumer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQPushConsumer>() {

                    @Override
                    public void init(DefaultMQPushConsumer consumer, String topicName) throws Exception {
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe(topicName, "*");
                        consumer.registerMessageListener(new MessageListenerConcurrently() {

                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                                Message message = msgs.get(0);
                                System.err.printf("接收延时消息: level=%d message=%s%n", message.getDelayTimeLevel(), new String(message.getBody()));
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }

                        });
                    }

                }).start().waitUtilClose();
    }

}
