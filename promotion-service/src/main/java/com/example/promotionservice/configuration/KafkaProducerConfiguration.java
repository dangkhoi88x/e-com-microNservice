package com.example.promotionservice.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import java.util.*;

@Configuration
public class KafkaProducerConfiguration {
    @Value("${spring.kafka.bootstrap-servers}") private String bootstrapServers;
    @Bean DefaultKafkaProducerFactory<String, Object> kafkaProducerFactory() { Map<String, Object> config = new HashMap<>(); config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class); return new DefaultKafkaProducerFactory<>(config); }
    @Bean KafkaTemplate<String, Object> kafkaTemplate() { return new KafkaTemplate<>(kafkaProducerFactory()); }
}
