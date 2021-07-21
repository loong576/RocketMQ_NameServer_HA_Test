package com.my.maven.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

public class Producer {
    public static void main(String[] args) throws Exception {
        //Instantiate with a producer group name.
        DefaultMQProducer producer = new
            DefaultMQProducer("group_test_123");
        // Specify name server addresses.
        producer.setNamesrvAddr("172.16.7.91:9876;172.16.7.92:9876");
        producer.setRetryTimesWhenSendAsyncFailed(2);
        //Launch the instance.
        producer.start();
        for (int i = 0; i < 10000; i++) {
            //Create a message instance, specifying topic, tag and message body.
            Message msg = new Message("topic_test_123" /* Topic */,
                "TagA" /* Tag */,
                ("NameServer Test" +
                    i).getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
            );
            //Call send message to deliver message to one of brokers.
            SendResult sendResult = producer.send(msg);
            System.out.printf("%s%n", sendResult);
        }
        //Shut down once the producer instance is not longer in use.
        producer.shutdown();
    }
}
