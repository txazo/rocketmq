package org.apache.rocketmq.test.message;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.test.common.RocketMQProducerConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量消息
 */
public class BatchMessage {

    public static void main(String[] args) throws Exception {
        RocketMQProducerConsumer producerConsumer = new RocketMQProducerConsumer();
        producerConsumer.namesrvAddr("127.0.0.1:9876")
                .group("group-batch")
                .topic("topic-batch")
                .producer(DefaultMQProducer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQProducer>() {

                    @Override
                    public void execute(DefaultMQProducer producer, String topicName) throws Exception {
                        for (int i = 0; ; i++) {
                            List<Message> messages = new ArrayList<>();
                            for (int j = 0; j < 10; j++) {
                                messages.add(new Message(topicName, ("message-" + i + "-" + j).getBytes(RemotingHelper.DEFAULT_CHARSET)));
                            }
                            SendResult result = producer.send(messages);
                            System.out.printf("生产批量消息: %s%n", result);
                            Thread.sleep(1000);
                        }
                    }

                })
                .consumer(DefaultMQPushConsumer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQPushConsumer>() {

                    @Override
                    public void init(DefaultMQPushConsumer consumer, String topicName) throws Exception {
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe(topicName, "*");
                        consumer.setMessageListener(new MessageListenerConcurrently() {

                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                                for (MessageExt message : msgs) {
                                    System.err.printf("消费批量消息: message=%s%n", new String(message.getBody()));
                                }
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }

                        });
                    }

                }).start().waitUtilClose();
    }

}
