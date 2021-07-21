package com.my.maven.rocketmq;

import java.util.List;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

public class Consumer {

    public static void main(String[] args) throws InterruptedException,
            MQClientException {

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                "group_test_123");
        consumer.setNamesrvAddr("172.16.7.91:9876;172.16.7.92:9876");

        consumer.subscribe("topic_test_123", "TagA || TagB");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                System.out.println(Thread.currentThread().getName()
                        + " Receive New Messages: " + msgs);
                MessageExt msg = msgs.get(0);
                if (msg.getTopic().equals("topic_test_123")) {
                    if (msg.getTags() != null && msg.getTags().equals("TagA")) {
                        // 获取消息体
                        String message = new String(msg.getBody());
                        System.out.println("receive TagA message:" + message);
                    } else if (msg.getTags() != null
                            && msg.getTags().equals("TagB")) {
                        // 获取消息体
                        String message = new String(msg.getBody());
                        System.out.println("receive TagB message:" + message);
                    }

                }
                // 成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.out.println("Consumer Started.");
    }

}