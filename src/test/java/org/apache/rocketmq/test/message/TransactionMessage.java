package org.apache.rocketmq.test.message;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.LocalTransactionExecuter;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionCheckListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.test.common.RocketMQProducerConsumer;

import java.util.List;

/**
 * 事务消息
 */
public class TransactionMessage {

    public static void main(String[] args) throws Exception {
        RocketMQProducerConsumer producerConsumer = new RocketMQProducerConsumer();
        producerConsumer.namesrvAddr("127.0.0.1:9876")
                .group("group-transaction")
                .topic("topic-transaction")
                .producer(TransactionMQProducer.class, new RocketMQProducerConsumer.RocketMQExecutor<TransactionMQProducer>() {

                    @Override
                    public void init(TransactionMQProducer producer, String topicName) throws Exception {
                        // 本地事务回查
                        producer.setTransactionCheckListener(new TransactionCheckListener() {

                            @Override
                            public LocalTransactionState checkLocalTransactionState(MessageExt msg) {
                                System.out.printf("本地事务消息回查: message=%s%n", new String(msg.getBody()));
                                int id = NumberUtils.toInt(msg.getProperty("id"), -1);
                                if (id > 0) {
                                    if (id % 2 == 0) {
                                        return LocalTransactionState.COMMIT_MESSAGE;
                                    } else {
                                        return LocalTransactionState.ROLLBACK_MESSAGE;
                                    }
                                }
                                return LocalTransactionState.UNKNOW;
                            }

                        });
                    }

                    @Override
                    public void execute(TransactionMQProducer producer, String topicName) throws Exception {
                        for (int i = 0; ; i++) {
                            Message message = new Message(topicName, ("message-" + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
                            message.putUserProperty("id", String.valueOf(i));
                            producer.sendMessageInTransaction(message, new LocalTransactionExecuter() {

                                @Override
                                public LocalTransactionState executeLocalTransactionBranch(Message msg, Object arg) {
                                    int i = (Integer) arg;
                                    if (i % 2 == 0) {
                                        System.out.printf("本地事务commit: message=%s%n", new String(msg.getBody()));
                                        return LocalTransactionState.COMMIT_MESSAGE;
                                    } else {
                                        System.out.printf("本地事务rollback: message=%s%n", new String(msg.getBody()));
                                        return LocalTransactionState.ROLLBACK_MESSAGE;
                                    }
                                }

                            }, i);
                            System.out.printf("生产事务消息: message=%s%n", new String(message.getBody()));
                            Thread.sleep(1000);
                        }
                    }

                })
                .consumer(DefaultMQPushConsumer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQPushConsumer>() {

                    @Override
                    public void init(DefaultMQPushConsumer consumer, String topicName) throws Exception {
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe(topicName, "*");
                        consumer.registerMessageListener(new MessageListenerConcurrently() {

                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                                System.err.printf("消费事务消息: message=%s%n" + new String(msgs.get(0).getBody()));
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }

                        });
                    }

                }).start().waitUtilClose();
    }

}
