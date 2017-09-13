package org.apache.rocketmq.test.simple;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

public class OnewayProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("group-1");
        producer.setNamesrvAddr("127.0.0.1:9876");
        try {
            producer.start();
            for (int i = 0; i < 100; i++) {
                producer.sendOneway(new Message("topic-3", "tag-1", "key-" + i, ("message-" + i).getBytes(RemotingHelper.DEFAULT_CHARSET)));
            }
        } finally {
            producer.shutdown();
        }
    }

}
