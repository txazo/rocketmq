package org.apache.rocketmq.test.filter;

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
 * tag过滤
 */
public class TagFilter {

    private static final String[] TAGS = {"tag-1", "tag-2", "tag-3", "tag-4", "tag-5"};

    public static void main(String[] args) throws Exception {
        RocketMQProducerConsumer producerConsumer = new RocketMQProducerConsumer();
        producerConsumer.namesrvAddr("127.0.0.1:9876")
                .group("group-tag")
                .topic("topic-tag")
                .producer(DefaultMQProducer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQProducer>() {

                    @Override
                    public void execute(DefaultMQProducer producer, String topicName) throws Exception {
                        for (int i = 0; ; i++) {
                            Message message = new Message(topicName, TAGS[i % TAGS.length], ("message-" + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
                            producer.send(message);
                            System.out.printf("生产Tag消息: tag=%s message=%s%n", message.getTags(), new String(message.getBody()));
                            Thread.sleep(500);
                        }
                    }

                })
                .consumer(DefaultMQPushConsumer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQPushConsumer>() {

                    @Override
                    public void init(DefaultMQPushConsumer consumer, String topicName) throws Exception {
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe(topicName, "tag-1 || tag-2 || tag-3");
                        consumer.setMessageListener(new MessageListenerConcurrently() {

                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                                Message message = msgs.get(0);
                                System.err.printf("消费Tag消息: tag=%s message=%s%n", message.getTags(), new String(message.getBody()));
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }

                        });
                    }

                }).start().waitUtilClose();
    }

}
