package org.apache.rocketmq.test.message;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.test.common.RocketMQProducerConsumer;

import java.util.List;

/**
 * 有序消息
 */
public class OrderlyMessage {

    public static void main(String[] args) throws Exception {
        RocketMQProducerConsumer producerConsumer = new RocketMQProducerConsumer();
        producerConsumer.namesrvAddr("127.0.0.1:9876")
                .group("group-orderly")
                .topic("topic-orderly")
                .producer(DefaultMQProducer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQProducer>() {

                    @Override
                    public void execute(DefaultMQProducer producer, String topicName) throws Exception {
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 5; j++) {
                                final Message message = new Message(topicName, ("message-" + i + "-" + j).getBytes(RemotingHelper.DEFAULT_CHARSET));
                                producer.send(message, new MessageQueueSelector() {

                                    @Override
                                    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                                        int i = (Integer) arg;
                                        return mqs.get(i % mqs.size());
                                    }

                                }, i);
                                System.out.println("生产有序消息: message=" + new String(message.getBody()));
                            }
                        }
                    }

                })
                .consumer(DefaultMQPushConsumer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQPushConsumer>() {

                    @Override
                    public void init(DefaultMQPushConsumer consumer, String topicName) throws Exception {
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe(topicName, "*");
                        consumer.setMessageListener(new MessageListenerOrderly() {

                            @Override
                            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                                context.setAutoCommit(false);
                                System.err.println("接收有序消息: message=" + new String(msgs.get(0).getBody()));
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return ConsumeOrderlyStatus.SUCCESS;
                            }

                        });
                    }

                })
                .start().waitUtilClose();
    }

}
