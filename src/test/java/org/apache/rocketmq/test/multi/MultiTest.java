package org.apache.rocketmq.test.multi;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.test.common.MultiRocketMQProducerConsumer;

import java.util.List;

public class MultiTest {

    public static void main(String[] args) throws Exception {
        MultiRocketMQProducerConsumer producerConsumer = new MultiRocketMQProducerConsumer();
        producerConsumer
                .namesrvAddr("127.0.0.1:9876;127.0.0.1:9877")
                .topic("topic-multi")
                .producerGroup("producer", 2)
                .consumerGroup("consumer", 2)
                .producer(DefaultMQProducer.class, new MultiRocketMQProducerConsumer.RocketMQExecutor<DefaultMQProducer>() {

                    @Override
                    public void init(DefaultMQProducer producer, int index, String nodeName, String topicName) throws Exception {
                        producer.setRetryTimesWhenSendAsyncFailed(0);
                    }

                    @Override
                    public void execute(DefaultMQProducer producer, int index, final String nodeName, String topicName) throws Exception {
                        for (int i = 0; ; i++) {
                            final Message message = new Message(topicName, ("message-" + index + "-" + +i).getBytes(RemotingHelper.DEFAULT_CHARSET));
                            producer.send(message, new SendCallback() {

                                @Override
                                public void onSuccess(SendResult result) {
                                    System.out.printf("[%s]生产异步消息: message=%s%n", nodeName, new String(message.getBody()));
                                }

                                @Override
                                public void onException(Throwable e) {
                                    System.err.printf("[%s]生产异步消息失败: message=%s%n", nodeName, new String(message.getBody()));
                                }

                            });
                            Thread.sleep(1);
                        }
                    }

                })
                .consumer(DefaultMQPushConsumer.class, new MultiRocketMQProducerConsumer.RocketMQExecutor<DefaultMQPushConsumer>() {

                    @Override
                    public void init(DefaultMQPushConsumer consumer, int index, final String nodeName, String topicName) throws Exception {
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe(topicName, "*");
                        consumer.setMessageListener(new MessageListenerConcurrently() {

                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                                System.err.printf("[%s]消费同步消息: message=%s%n", nodeName, new String(msgs.get(0).getBody()));
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }

                        });
                    }

                }).start().waitUtilClose();
    }

}
