package org.apache.rocketmq.test.order;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.List;

public class OrderedProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("group-1");
        try {
            producer.setNamesrvAddr("127.0.0.1:9876");
            producer.start();
            String[] tags = new String[]{"tagA", "tagB", "tagC", "tagD", "tagE"};
            for (int i = 0; i < 100; i++) {
                int orderId = i % 10;
                SendResult result = producer.send(new Message("topic-1", tags[i % tags.length], "key-" + i, ("message-" + 1).getBytes()),
                        new MessageQueueSelector() {

                            @Override
                            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                                int id = (Integer) arg;
                                int index = id % mqs.size();
                                return mqs.get(index);
                            }

                        }, orderId);
                System.out.printf("%s%n", result);
            }
        } finally {
            producer.shutdown();
        }
    }

}
