package org.apache.rocketmq.test.message;

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
import org.apache.rocketmq.test.common.RocketMQProducerConsumer;

import java.util.List;

/**
 * 异步消息
 */
public class AsyncMessage {

    public static void main(String[] args) throws Exception {
        RocketMQProducerConsumer producerConsumer = new RocketMQProducerConsumer();
        producerConsumer.namesrvAddr("127.0.0.1:9876")
                .group("group-async")
                .topic("topic-async")
                .producer(DefaultMQProducer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQProducer>() {

                    @Override
                    public void init(DefaultMQProducer producer, String topicName) throws Exception {
                        producer.setRetryTimesWhenSendAsyncFailed(0);
                    }

                    @Override
                    public void execute(DefaultMQProducer producer, String topicName) throws Exception {
                        for (int i = 0; ; i++) {
                            final Message message = new Message(topicName, ("message-" + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
                            producer.send(message, new SendCallback() {

                                @Override
                                public void onSuccess(SendResult result) {
                                    System.out.println("发送异步消息: message=" + new String(message.getBody()));
                                }

                                @Override
                                public void onException(Throwable e) {
                                    System.err.println("发送异步消息失败: message=" + new String(message.getBody()));
                                }

                            });
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
                                MessageExt message = msgs.get(0);
                                System.err.println("消费异步消息: message=" + new String(message.getBody()));
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }

                        });
                    }

                }).start().waitUtilClose();
    }

}
