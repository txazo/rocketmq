package org.apache.rocketmq.test.message;

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
                                System.out.println("本地事务消息回查: message=" + new String(msg.getBody()));
                                return LocalTransactionState.COMMIT_MESSAGE;
                            }

                        });
                    }

                    @Override
                    public void execute(TransactionMQProducer producer, String topicName) {
                        for (int i = 0; ; i++) {
                            try {
                                Message message = new Message(topicName, ("message-" + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
                                producer.sendMessageInTransaction(message, new LocalTransactionExecuter() {

                                    @Override
                                    public LocalTransactionState executeLocalTransactionBranch(Message msg, Object arg) {
                                        int i = (Integer) arg;
                                        if (i % 2 == 0) {
                                            System.out.println("本地事务commit: message=" + new String(msg.getBody()));
                                            return LocalTransactionState.COMMIT_MESSAGE;
                                        } else {
                                            System.out.println("本地事务rollback: message=" + new String(msg.getBody()));
                                            return LocalTransactionState.ROLLBACK_MESSAGE;
                                        }
                                    }

                                }, i);
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                })
                .consumer(DefaultMQPushConsumer.class, new RocketMQProducerConsumer.RocketMQExecutor<DefaultMQPushConsumer>() {

                    @Override
                    public void init(DefaultMQPushConsumer consumer, String topicName) throws Exception {
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe(topicName, "*");
                        consumer.setConsumeConcurrentlyMaxSpan(1);
                        consumer.registerMessageListener(new MessageListenerConcurrently() {

                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                                Message message = msgs.get(0);
                                System.out.println("接收事务消息: message=" + new String(message.getBody()));
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }

                        });
                    }

                }).start().waitUtilClose();
    }

}
