package com.example.smart_learning.seckill.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Map;

@Configuration
public class SeckillKafkaConfig {

    public static final String ORDER_TOPIC = "seckill-order";
    public static final String DLQ_TOPIC = "seckill-order-dlq";
    public static final String CONSUMER_GROUP = "seckill-order-consumer";

    @Bean
    public NewTopic seckillOrderTopic() {
        return TopicBuilder.name(ORDER_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic seckillOrderDlqTopic() {
        return TopicBuilder.name(DLQ_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean("seckillProducerFactory")
    public ProducerFactory<String, String> seckillProducerFactory(KafkaProperties properties) {
        Map<String, Object> config = properties.buildProducerProperties();
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean("seckillKafkaTemplate")
    public KafkaTemplate<String, String> seckillKafkaTemplate(
            @Qualifier("seckillProducerFactory") ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean("seckillConsumerFactory")
    public ConsumerFactory<String, String> seckillConsumerFactory(KafkaProperties properties) {
        Map<String, Object> config = properties.buildConsumerProperties();
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean("seckillKafkaErrorHandler")
    public DefaultErrorHandler seckillKafkaErrorHandler(
            @Qualifier("seckillKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(DLQ_TOPIC, record.partition()));
        // 重试 3 次（间隔 1 秒）后进入死信队列
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.setCommitRecovered(true);
        return handler;
    }

    @Bean("seckillKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> seckillKafkaListenerContainerFactory(
            @Qualifier("seckillConsumerFactory") ConsumerFactory<String, String> consumerFactory,
            @Qualifier("seckillKafkaErrorHandler") DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
