package org.apache.rocketmq.test.simple;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

public class SyncProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("group-1");
        producer.setNamesrvAddr("127.0.0.1:9876");
        try {
            producer.start();
            for (int i = 0; i < 100; i++) {
                SendResult result = producer.send(new Message("topic-1", "tag-1", "key-" + i, ("message-" + i).getBytes(RemotingHelper.DEFAULT_CHARSET)));
                System.out.println(result);
            }
        } finally {
            producer.shutdown();
        }
    }

}
