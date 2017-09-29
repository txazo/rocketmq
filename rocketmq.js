// Concumer Producer -> Namesrv 30s
get_routeinto_by_topic =
{
    "topic": "topic-multi",
    "_nodeName": "consumer-1"
};

// Broker -> Namesrv 30s
register_broker =
{
    "brokerId": "0",
    "clusterName": "DefaultCluster",
    "brokerAddr": "10.32.66.148:10911",
    "haServerAddr": "10.32.66.148:10912",
    "brokerName": "broker-1",
    "_nodeName": "broker-1-master"
};

// Broker -> Concumer 消费者变更
notify_consumer_ids_changed =
{
    "_nodeName": "broker-1-master",
    "consumerGroup": "consumer"
};

// Concumer Producer -> Broker 30s
heart_beat = {
    "_nodeName": "producer-0"
};

// Producer -> Broker Master 单条消息
send_message_v2 = {
    "producerGroup": "producer",
    "topic": "topic-multi",
    "defaultTopic": "TBW102",
    "defaultTopicQueueNums": "4",
    "queueId": "2",
    "sysFlag": "0",
    "bornTimestamp": "1506681800873",
    "flag": "0",
    "properties": "UNIQ_KEY\u00010A204294828F2FF4ACD0943684A80001\u0002WAIT\u0001true\u0002",
    "reconsumeTimes": "0",
    "unitMode": "false",
    "batch": "false",
    "_nodeName": "producer-0"
};

// Concumer -> Broker
pull_message = {
    "queueId": "2",
    "maxMsgNums": "32",
    "sysFlag": "3",
    "suspendTimeoutMillis": "15000",
    "commitOffset": "5725",
    "topic": "topic-multi",
    "queueOffset": "5726",
    "subVersion": "1506681831385",
    "_nodeName": "consumer-0",
    "consumerGroup": "consumer"
};

// Broker Slave -> Broker Master 60s
get_all_consumer_offset =
{
    "_nodeName": "broker-2-slave"
};

// Broker Slave -> Broker Master 60s
get_all_delay_offset =
{
    "_nodeName": "broker-1-slave"
};

// Broker Slave -> Broker Master 60s
get_all_topic_config = {
    "_nodeName": "broker-1-slave"
};

// Broker Slave -> Broker Master 60s
get_all_subscriptiongroup_config = {
    "_nodeName": "broker-2-slave"
};

// Concumer -> Broker Master
get_consumer_list_by_group = {
    "_nodeName": "consumer-0", "consumerGroup": "consumer"
};

// Concumer -> Broker Master
query_consumer_offset =
{
    "queueId": "3",
    "topic": "topic-multi",
    "_nodeName": "consumer-0",
    "consumerGroup": "consumer"
};

// Concumer -> Broker Master
update_consumer_offset = {
    "queueId": "0",
    "commitOffset": "37150",
    "topic": "topic-multi",
    "_nodeName": "consumer-0",
    "consumerGroup": "consumer"
};
