package com.mercury.poker.events;

import com.mercury.poker.network.PodIdentity;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * Kafka 事件发布：房间创建、玩家动作等审计/分析事件（非实时牌局同步）。
 */
public class EventPublisher {

    private static final String DEFAULT_TOPIC = "poker.events";
    private static final EventPublisher INSTANCE = new EventPublisher();

    private final boolean enabled;
    private final String topic;
    private Producer<String, String> producer;

    private EventPublisher() {
        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            enabled = false;
            topic = DEFAULT_TOPIC;
            return;
        }
        String configuredTopic = System.getenv("KAFKA_EVENTS_TOPIC");
        topic = configuredTopic == null || configuredTopic.isBlank() ? DEFAULT_TOPIC : configuredTopic.trim();
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.trim());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "1");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "poker-aa-" + PodIdentity.getPodName());
        producer = new KafkaProducer<>(properties);
        enabled = true;
        System.out.println("Kafka EventPublisher 已启用，topic=" + topic + " bootstrap=" + bootstrapServers);
    }

    public static EventPublisher getINSTANCE() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void publish(String eventType, Map<String, String> fields) {
        if (!enabled || producer == null) {
            return;
        }
        StringJoiner joiner = new StringJoiner(",");
        joiner.add("\"type\":\"" + escapeJson(eventType) + "\"");
        joiner.add("\"pod\":\"" + escapeJson(PodIdentity.getPodName()) + "\"");
        joiner.add("\"ts\":" + System.currentTimeMillis());
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            joiner.add("\"" + escapeJson(entry.getKey()) + "\":\"" + escapeJson(entry.getValue()) + "\"");
        }
        String payload = "{" + joiner + "}";
        producer.send(new ProducerRecord<>(topic, eventType, payload), (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Kafka 发送失败: " + exception.getMessage());
            }
        });
    }

    public void shutdown() {
        if (producer != null) {
            producer.flush();
            producer.close();
            System.out.println("Kafka EventPublisher 已关闭");
        }
    }

    private String escapeJson(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        return rawValue.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
