package org.apache.rocketmq.test.simple;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class Consumer {

    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("group-1");
        try {
            consumer.setNamesrvAddr("127.0.0.1:9876");
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
            consumer.subscribe("topic-1", "*");
            consumer.registerMessageListener(new MessageListenerConcurrently() {

                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                    for (MessageExt message : msgs) {
                        System.out.printf(Thread.currentThread().getName() + " Receive New Messages: " + message + "%n");
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }

            });
            consumer.start();
            Thread.sleep(1000 * 60 * 60);
        } finally {
            consumer.shutdown();
        }
    }

}
