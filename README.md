## 一、环境说明

| ip地址      | 主机名       | 操作系统版本 | RocketMQ版本 |  JDK版本  | maven版本 | 备注            |
| ----------- | ------------ | :----------: | :----------: | :-------: | :-------: | --------------- |
| 172.16.7.91 | nameserver01 |  centos 7.6  |    4.8.0     | 1.8.0_291 |    3.6    | Name Server集群 |
| 172.16.7.92 | nameserver03 |  centos 7.6  |    4.8.0     | 1.8.0_291 |    3.6    | Name Server集群 |
| 172.16.7.93 | master01     |  centos 7.6  |    4.8.0     | 1.8.0_291 |    3.6    | Broker集群1     |
| 172.16.7.94 | slave01      |  centos 7.6  |    4.8.0     | 1.8.0_291 |    3.6    | Broker集群1     |
| 172.16.7.95 | master02     |  centos 7.6  |    4.8.0     | 1.8.0_291 |    3.6    | Broker集群2     |
| 172.16.7.96 | slave02      |  centos 7.6  |    4.8.0     | 1.8.0_291 |    3.6    | Broker集群2     |

## 二、部署概况

![image-20210615154528229](https://i.loli.net/2021/07/21/A9ndYvmKxWw4lj7.png)

## 三、消息正常发送

### 1.Producer代码

```bash
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
        for (int i = 0; i < 100; i++) {
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
```

发送消息NameServer Test0--NameServer Test99

### 2.运行Producer

![image-20210721151024864](https://i.loli.net/2021/07/21/rxu2zOW4onFPQwm.png)

### 3.发送查看

日志：

```bash
SendResult [sendStatus=SEND_OK, msgId=AC1B09F602B873D16E936A4028160000, offsetMsgId=AC10075D00002A9F0000000000000000, messageQueue=MessageQueue [topic=topic_test_123, brokerName=broker-a, queueId=8], queueOffset=0]
SendResult [sendStatus=SEND_OK, msgId=AC1B09F602B873D16E936A4028470001, offsetMsgId=AC10075D00002A9F00000000000000CA, messageQueue=MessageQueue [topic=topic_test_123, brokerName=broker-a, queueId=9], queueOffset=0]
SendResult [sendStatus=SEND_OK, msgId=AC1B09F602B873D16E936A40284C0002, offsetMsgId=AC10075D00002A9F0000000000000194, messageQueue=MessageQueue [topic=topic_test_123, brokerName=broker-a, queueId=10], queueOffset=0]
SendResult [sendStatus=SEND_OK, msgId=AC1B09F602B873D16E936A4028520003, offsetMsgId=AC10075D00002A9F000000000000025E, messageQueue=MessageQueue [topic=topic_test_123, brokerName=broker-a, queueId=11], queueOffset=0]
SendResult [sendStatus=SEND_OK, msgId=AC1B09F602B873D16E936A4028550004, offsetMsgId=AC10075D00002A9F0000000000000328, messageQueue=MessageQueue [topic=topic_test_123, brokerName=broker-a, queueId=12], queueOffset=0]
SendResult [sendStatus=SEND_OK, msgId=AC1B09F602B873D16E936A4028590005, offsetMsgId=AC10075D00002A9F00000000000003F2, messageQueue=MessageQueue [topic=topic_test_123, brokerName=broker-a, queueId=13], queueOffset=0]
……
```

一共100条发送记录

console查看

![image-20210721151325317](https://i.loli.net/2021/07/21/HUJQcPWoTgMCnih.png)

broker-a和broker-b两个分片各发送了52和48条消息

### 4.Consumer代码

```bash
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
```

### 5.运行Consumer

![image-20210721151738029](https://i.loli.net/2021/07/21/G2oW6PDL3veTtQ4.png)

### 6.消费查看

消费日志：

![image-20210721151810222](https://i.loli.net/2021/07/21/S1COBJmxcHFvV5P.png)

```bash
Consumer Started.
ConsumeMessageThread_6 Receive New Messages: [MessageExt [queueId=13, storeSize=203, queueOffset=2, sysFlag=0, bornTimestamp=1626851390760, bornHost=/172.16.7.1:49540, storeTimestamp=1626851374215, storeHost=/172.16.7.95:10911, msgId=AC10075F00002A9F00000000000023AD, commitLogOffset=9133, bodyCRC=938317384, reconsumeTimes=0, preparedTransactionOffset=0, toString()=Message{topic='topic_test_123', flag=0, properties={MIN_OFFSET=0, MAX_OFFSET=3, CONSUME_START_TIME=1626851850431, UNIQ_KEY=AC1B09F602B873D16E936A402D280055, CLUSTER=MyRocketmq, WAIT=true, TAGS=TagA}, body=[78, 97, 109, 101, 83, 101, 114, 118, 101, 114, 32, 84, 101, 115, 116, 56, 53], transactionId='null'}]]
receive TagA message:NameServer Test85
ConsumeMessageThread_1 Receive New Messages: [MessageExt [queueId=13, storeSize=203, queueOffset=0, sysFlag=0, bornTimestamp=1626851390103, bornHost=/172.16.7.1:49540, storeTimestamp=1626851373560, storeHost=/172.16.7.95:10911, msgId=AC10075F00002A9F0000000000000A4D, commitLogOffset=2637, bodyCRC=1248836315, reconsumeTimes=0, preparedTransactionOffset=0, toString()=Message{topic='topic_test_123', flag=0, properties={MIN_OFFSET=0, MAX_OFFSET=3, CONSUME_START_TIME=1626851850427, UNIQ_KEY=AC1B09F602B873D16E936A402A970015, CLUSTER=MyRocketmq, WAIT=true, TAGS=TagA}, body=[78, 97, 109, 101, 83, 101, 114, 118, 101, 114, 32, 84, 101, 115, 116, 50, 49], transactionId='null'}]]
receive TagA message:NameServer Test21
ConsumeMessageThread_9 Receive New Messages: [MessageExt [queueId=5, storeSize=203, queueOffset=1, sysFlag=0, bornTimestamp=1626851390599, bornHost=/172.16.7.1:49540, storeTimestamp=1626851374054, storeHost=/172.16.7.95:10911, msgId=AC10075F00002A9F00000000000010A5, commitLogOffset=4261, bodyCRC=458807620, reconsumeTimes=0, preparedTransactionOffset=0, toString()=Message{topic='topic_test_123', flag=0, properties={MIN_OFFSET=0, MAX_OFFSET=3, CONSUME_START_TIME=1626851850434, UNIQ_KEY=AC1B09F602B873D16E936A402C87002D, CLUSTER=MyRocketmq, WAIT=true, TAGS=TagA}, body=[78, 97, 109, 101, 83, 101, 114, 118, 101, 114, 32, 84, 101, 115, 116, 52, 53], transactionId='null'}]]
receive TagA message:NameServer Test45
ConsumeMessageThread_13 Receive New Messages: [MessageExt [queueId=9, storeSize=203, queueOffset=2, sysFlag=0, bornTimestamp=1626851390743, bornHost=/172.16.7.1:49540, storeTimestamp=1626851374198, storeHost=/172.16.7.95:10911, msgId=AC10075F00002A9F0000000000002081, commitLogOffset=8321, bodyCRC=813716049, reconsumeTimes=0, preparedTransactionOffset=0, toString()=Message{topic='topic_test_123', flag=0, properties={MIN_OFFSET=0, MAX_OFFSET=3, CONSUME_START_TIME=1626851850435, UNIQ_KEY=AC1B09F602B873D16E936A402D170051, CLUSTER=MyRocketmq, WAIT=true, TAGS=TagA}, body=[78, 97, 109, 101, 83, 101, 114, 118, 101, 114, 32, 84, 101, 115, 116, 56, 49], transactionId='null'}]]
receive TagA message:NameServer Test81
ConsumeMessageThread_4 Receive New Messages: [MessageExt [queueId=5, storeSize=203, queueOffset=0, sysFlag=0, bornTimestamp=1626851389767, bornHost=/172.16.7.1:49540, storeTimestamp=1626851373464, storeHost=/172.16.7.95:10911, msgId=AC10075F00002A9F00000000000003F5, commitLogOffset=1013, bodyCRC=256673844, reconsumeTimes=0, preparedTransactionOffset=0, toString()=Message{topic='topic_test_123', flag=0, properties={MIN_OFFSET=0, MAX_OFFSET=3, CONSUME_START_TIME=1626851850433, UNIQ_KEY=AC1B09F602B873D16E936A402947000D, CLUSTER=MyRocketmq, WAIT=true, TAGS=TagA}, body=[78, 97, 109, 101, 83, 101, 114, 118, 101, 114, 32, 84, 101, 115, 116, 49, 51], transactionId='null'}]]
receive TagA message:NameServer Test13
ConsumeMessageThread_13 Receive New Messages: [MessageExt [queueId=12, storeSize=203, queueOffset=1, sysFlag=0, bornTimestamp=1626851390629, bornHost=/172.16.7.1:49540, storeTimestamp=1626851374084, storeHost=/172.16.7.95:10911, msgId=AC10075F00002A9F0000000000001632, commitLogOffset=5682, bodyCRC=472350118, reconsumeTimes=0, preparedTransactionOffset=0, toString()=Message{topic='topic_test_123', flag=0, properties={MIN_OFFSET=0, MAX_OFFSET=3, CONSUME_START_TIME=1626851850436, UNIQ_KEY=AC1B09F602B873D16E936A402CA50034, CLUSTER=MyRocketmq, WAIT=true, TAGS=TagA}, body=[78, 97, 109, 101, 83, 101, 114, 118, 101, 114, 32, 84, 101, 115, 116, 53, 50], transactionId='null'}]]
receive TagA message:NameServer Test52
……
```

一共消费了100条消息

console查看

![image-20210721151940928](https://i.loli.net/2021/07/21/2yDMtKEN1oURAPS.png)

发现消息消费一共也是100条

## 四、关闭一台nameserver节点

### 1.关闭nameserver02

```bash
[root@nameserver02 ~]# init 0
```

### 2.发送消息

再次发送100条消息

![image-20210721152715374](https://i.loli.net/2021/07/21/cPDqYo2x1TyXnFS.png)

![image-20210721152758850](https://i.loli.net/2021/07/21/gFML5czEORdGxqA.png)

查看日志和console，发现新增消息100条

### 3.消息消费

![image-20210721153322549](https://i.loli.net/2021/07/21/p1tieqvaAJIzsQG.png)

![image-20210721153338304](https://i.loli.net/2021/07/21/gpUiQEX19TmuOPo.png)

消息消费也新增100条

### 4.结论

**结论一：**当一个nameserver节点宕机时，不影响消息发送和消费。

## 五、关闭所有nameserver节点

### 1.消息发送

继续发送10000条消息，发送的同时继续关闭nameserver01

```bash
[root@nameserver01 ~]# init 0
```

![image-20210721154611968](https://i.loli.net/2021/07/21/16rmYgUcMkh5fXV.png)

![image-20210721154625773](https://i.loli.net/2021/07/21/Hcb4mTzRySfx2Zv.png)

发送10000条消息，发送的同时关闭nameserver01，发现消息只发送了367条

### 2.消息消费

![image-20210721155028198](https://i.loli.net/2021/07/21/byW9NTkaszHRnOo.png)

发现无法消费，无消费记录

### 3.结论

**结论二：**当所有nameserver宕机时，消息发送和接收都会无法进行。

## 六、开启nameserver01

### 1.nameserver01开机

消息发送和消费会恢复，但是会丢消息

![image-20210721155944381](https://i.loli.net/2021/07/21/QcurKyod6CbHTPY.png)



![image-20210721160153113](https://i.loli.net/2021/07/21/e5acfwASmvb4hUq.png)

### 2.结论

**结论三：**当nameserver集群恢复时，部分消息会恢复发送和消费，同时出现部分消息丢失情况。

## 七、总结

**总结：**为保证RocketMQ集群能正常对外提供服务，需至少保证有一台nameserver服务器处于运行状态；当所有nameserver服务器宕机时，消息无法发送和消费。





单机版RocketMQ搭建详见：[Centos7.6搭建RocketMQ4.8全纪录](https://blog.51cto.com/u_3241766/2908731)

集群版RocketMQ搭建详见：[RocketMQ4.8集群搭建全纪录](https://blog.51cto.com/u_3241766/2930669)

集群启停详见：[RocketMQ集群启停手册](https://blog.51cto.com/u_3241766/2965870)

集群消息收发测试：[RocketMQ集群消息收发测试全纪录](https://blog.51cto.com/u_3241766/3052355)

![image-20210721162945755](https://i.loli.net/2021/07/21/41LsKFX562DenHU.png)
