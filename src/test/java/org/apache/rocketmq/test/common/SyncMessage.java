package org.apache.rocketmq.test.common;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.List;

public class SyncMessage {

    public static void main(String[] args) throws Exception {
        RocketMQProducerConsumer producerConsumer = new RocketMQProducerConsumer();
        producerConsumer
                .namesrvAddr("127.0.0.1:9876")
                .group("group-1")
                .producer(DefaultMQProducer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQProducer>() {

                    @Override
                    public void execute(DefaultMQProducer producer) throws Exception {
                        for (int i = 0; ; i++) {
                            SendResult result = producer.send(new Message("topic-1", "tag-1", "key-" + i, ("message-" + i).getBytes(RemotingHelper.DEFAULT_CHARSET)));
                            System.out.println(result);
                            Thread.sleep(1000);
                        }
                    }

                })
                .consumer(DefaultMQPushConsumer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQPushConsumer>() {

                    @Override
                    public void execute(DefaultMQPushConsumer consumer) throws Exception {
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe("topic-1", "*");
                        consumer.registerMessageListener(new MessageListenerOrderly() {

                            @Override
                            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                                for (MessageExt message : msgs) {
                                    System.out.printf(Thread.currentThread().getName() + " Receive New Messages: " + message + "%n");
                                }
                                return ConsumeOrderlyStatus.SUCCESS;
                            }

                        });
                    }

                })
                .start()
                .waitUtilClose();
    }

}
