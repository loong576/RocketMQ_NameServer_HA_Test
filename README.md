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

![image-20210615154528229](https://i.loli.net/2021/08/02/GWt9i6OyFCeALZf.png)



## 三、测试准备

### 1.清空消息

```bash
[root@master01 ~]# init 6
[root@master01 ~]# cd /root/logs/rocketmqlogs/
[root@master01 rocketmqlogs]# rm -rf *
[root@master01 rocketmqlogs]# cd /root/store/
[root@master01 store]# rm -rf *
```

以master01为例，首先停止所有rocketmq进程，然后删除日志和存储信息。所有服务器都执行该操作。

### 2.测试前集群查看

启动各节点服务，查看集群状态

![image-20210629101934509](https://i.loli.net/2021/08/02/OJDamiPVyx5Z26F.png)

### 3.新建topic

新增主题topic_broker_test

![image-20210629102044810](https://i.loli.net/2021/08/02/SYlMDy7V2CwX5oq.png)

主题配置如下：

![image-20210629102112190](https://i.loli.net/2021/08/02/cB1dl8awpUOWKbE.png)

查看新增的主题

![image-20210629102149912](https://i.loli.net/2021/08/02/CSqU5NiwJ7b2Wr9.png)

### 4.新建订阅组

新建订阅组group_broker_test

![image-20210629102224264](https://i.loli.net/2021/08/02/lOuJ1YdPrzCpATo.png)

配置如下：

![image-20210629102257920](https://i.loli.net/2021/08/02/bngaVKcjmiX1fFE.png)

查看新建的订阅组

![image-20210629102322052](https://i.loli.net/2021/08/02/f8Tnd4yoa9DJz1k.png)

### 5.producer代码

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
            DefaultMQProducer("group_broker_test");
        // Specify name server addresses.
        producer.setNamesrvAddr("172.16.7.91:9876;172.16.7.92:9876");
        producer.setRetryTimesWhenSendAsyncFailed(2);
        //Launch the instance.
        producer.start();
        for (int i = 0; i < 10000; i++) {
            //Create a message instance, specifying topic, tag and message body.
            Message msg = new Message("topic_broker_test" /* Topic */,
                "TagA" /* Tag */,
                ("Broker HA Test" +
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

使用循环方式产生多条消息

### 6.consumer代码

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
                "group_broker_test");
        consumer.setNamesrvAddr("172.16.7.91:9876;172.16.7.92:9876");

        consumer.subscribe("topic_broker_test", "TagA || tagB");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                System.out.println(Thread.currentThread().getName()
                        + " Receive New Messages: " + msgs);
                MessageExt msg = msgs.get(0);
                if (msg.getTopic().equals("topic_broker_test")) {
                    if (msg.getTags() != null && msg.getTags().equals("tagA")) {
                        // 获取消息体
                        String message = new String(msg.getBody());
                        System.out.println("receive tagA message:" + message);
                    } else if (msg.getTags() != null
                            && msg.getTags().equals("tagB")) {
                        // 获取消息体
                        String message = new String(msg.getBody());
                        System.out.println("receive tagB message:" + message);
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

## 四、消息发送高可用测试

### 1.broker-a master重启

```bash
[root@master01 rocketmq]# init 6
```

| 主机名          | 状态       |
| --------------- | ---------- |
| broker-a master | 发送时重启 |
| broker-a slave  | 正常运行   |
| broker-b master | 正常运行   |
| broker-b slave  | 正常运行   |

发送5000条消息，在消息发送的同时关闭broker-a master

![image-20210730142405417](https://i.loli.net/2021/08/02/6LypRxqQDhwzFfu.png)

消息发送会暂停，一共发送了153条

![image-20210730142548040](https://i.loli.net/2021/08/02/53K9BUF6mlcXOGY.png)

**结论**：消息发送时如果有master宕机，则消息发送会终止，主机起来后消息也不会继续发送。

### 2.broker-a slave重启

```bash
[root@slave01 rocketmq]# init 6
```

| 主机名          | 状态       |
| --------------- | ---------- |
| broker-a master | 宕机       |
| broker-a slave  | 发送时重启 |
| broker-b master | 正常运行   |
| broker-b slave  | 正常运行   |

发送5000条消息，在发送过程中同时重启broker-a slave

![image-20210730142913291](https://i.loli.net/2021/08/02/NBiatkIcgvqdWCQ.png)

消息发送会暂停，一共发送了339条

![image-20210730143235590](https://i.loli.net/2021/08/02/QbNyU2CozYR4ScB.png)

**结论**：消息发送时如果有slave宕机，则消息发送会终止，主机起来后消息也不会继续发送。

### 3.所有 slave关机

| 主机名          | 状态     |
| --------------- | -------- |
| broker-a master | 正常运行 |
| broker-a slave  | 宕机     |
| broker-b master | 正常运行 |
| broker-b slave  | 宕机     |

```bash
[root@slave01 rocketmq]# init 0
[root@slave02 rocketmq]# init 0
```

发送5000条消息，在**发送过程中**同时关闭所有的slave

![image-20210730163418693](https://i.loli.net/2021/08/02/2DiXN1gCVU8IFja.png)

消息发送会暂停，一共发送了401条，这也验证了上面的结论：消息发送时如果有slave宕机，则消息发送会终止

![image-20210730163557990](https://i.loli.net/2021/08/02/Hbit1Ig7vqsnjYT.png)

保持两个slave宕机状态，继续发送5000条消息

![image-20210730164042480](https://i.loli.net/2021/08/02/zOuifBKHTbtnk59.png)

console显示消息记录数为5000条

![image-20210730164128061](https://i.loli.net/2021/08/02/U5QinhsuxOClwtZ.png)

**结论**：slave都宕机不影响消息发送。

### 4.broker-b master关机

| 主机名          | 状态     |
| --------------- | -------- |
| broker-a master | 正常运行 |
| broker-a slave  | 宕机     |
| broker-b master | 宕机     |
| broker-b slave  | 宕机     |

```bash
[root@master02 rocketmq]# init 0
```

发送5000条测试消息，发送前broker-b master关机，只保留broker-a master运行

![image-20210730165238077](https://i.loli.net/2021/08/02/TyZuasngbSQlicU.png)

console显示发送了5000条消息

![image-20210730165328421](https://i.loli.net/2021/08/02/sFt6XLz2xD5kEmy.png)

**结论：**集群只有一台master消息发送正常。

### 5.关闭所有的master

| 主机名          | 状态     |
| --------------- | -------- |
| broker-a master | 宕机     |
| broker-a slave  | 正常运行 |
| broker-b master | 宕机     |
| broker-b slave  | 正常运行 |

关闭所有的master，启动所有的slave，发送5000条消息

消息发送前：

![image-20210730170145062](https://i.loli.net/2021/08/02/8SaTcAPLjid5ywQ.png)

消息发送：

![image-20210730170522542](https://i.loli.net/2021/08/02/OS6EHgJWaf7ZxYI.png)

console报错，消息无法发送

**结论**：master都宕机消息无法正常发送。

## 五、消息消费高可用测试

**在消息消费高可用测试前先清空消息，然后发送1万条消息**

![image-20210802143913501](https://i.loli.net/2021/08/02/e2kzgUs54KREuYZ.png)

### 1.broker-a master关机

```bash
[root@master01 rocketmq]# init 0
```

在消息消费时将broker-a master关机

| 主机名          | 状态       |
| --------------- | ---------- |
| broker-a master | 消费时关机 |
| broker-a slave  | 正常运行   |
| broker-b master | 正常运行   |
| broker-b slave  | 正常运行   |

消费刚发送的1万条消息，消费过程中将broker-a master关机

![image-20210802144359818](https://i.loli.net/2021/08/02/m5Kty8HCIX6uRvJ.png)

console日志显示消息消费了1万条

**结论**：某台master宕机不影响消息消费。

### 2.broker-a slave关机

**先发送1万条消息，然后消费，消费过程中broker-a slave关机**

```bash
[root@slave01 rocketmq]# init 0
```

![image-20210802145752038](https://i.loli.net/2021/08/02/IOKeqAUkdWo6tmp.png)

dashboard的消费统计不是很准确，以eclipse的console日志为准。

| 主机名          | 状态       |
| --------------- | ---------- |
| broker-a master | 消费时关机 |
| broker-a slave  | 消费时关机 |
| broker-b master | 正常运行   |
| broker-b slave  | 正常运行   |

消费刚发送的1万条消息，消费过程中将broker-a slave关机

![image-20210802151356095](https://i.loli.net/2021/08/02/FHGme35xuAaBq6E.png)

console显示消费了1万条

**结论**：某台slave宕机不影响消息消费

### 3.所有slave关机

**先发送1万条消息，然后消费，消费过程中broker-b slave关机**

```bash
[root@slave02 rocketmq]# init 0
```

| 主机名          | 状态       |
| --------------- | ---------- |
| broker-a master | 消费时关机 |
| broker-a slave  | 消费时关机 |
| broker-b master | 正常运行   |
| broker-b slave  | 消费时关机 |

消费刚发送的1万条消息，消费过程中将broker-b slave关机

![image-20210802152600529](https://i.loli.net/2021/08/02/eJWESkNC63coRdX.png)

console消费1万条消息

**结论**：slave都宕机不影响消息消费

### 4.所有master关机

**拉起broker-a slave或者broker-b slave，保持broker-b master开机状态，发送1万条消息，再将所有master关机最后消费**

| 主机名          | 状态     |
| --------------- | -------- |
| broker-a master | 关机     |
| broker-a slave  | 关机     |
| broker-b master | 关机     |
| broker-b slave  | 正常运行 |

消费刚发送的1万条消息

![image-20210802163420602](https://i.loli.net/2021/08/02/PLvuhCGz1l2DpnU.png)

console显示消费了1w条记录

**结论**：master都宕机不影响消息发送

### 5.所有slave关机

**拉起broker-a master或者broker-b master，关闭所有的slave，发送1万条消息，然后消费**

| 主机名          | 状态     |
| --------------- | -------- |
| broker-a master | 正常运行 |
| broker-a slave  | 关机     |
| broker-b master | 关机     |
| broker-b slave  | 关机     |

消费刚发送的1万条消息

![image-20210802165636772](https://i.loli.net/2021/08/02/PYjfgQXJ8zUbGoC.png)

console显示消息全部被消费

**结论**：slave都宕机不影响消息消费

## 六、测试总结

> - 1.消息发送过程中只要有任意一台master或者slave宕机则发送程序暂停；
> - 2.消息发送前slave都宕机不影响消息发送；
> - 3.master或者slave都宕机不影响消息消费；
> - 4.为保证消息正常的收发，集群最小配置为必需要有一台master主机；



