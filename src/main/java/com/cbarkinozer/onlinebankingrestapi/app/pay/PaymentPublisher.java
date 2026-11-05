package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.util.Map;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentPublisher {
    private final DefaultKafkaProducerFactory<String, String> factory;
    private final KafkaTemplate<String, String> template;

    public PaymentPublisher(@Value("${cbarkinozer.kafka.adddress}") String address) {
        factory = new DefaultKafkaProducerFactory<>(Map.of(
                "bootstrap.servers", address,
                "key.serializer", StringSerializer.class,
                "value.serializer", StringSerializer.class));
        template = new KafkaTemplate<>(factory);
    }

    public void publish(String id, String event) {
        template.send("outbound-payments", id, event);
    }

    @javax.annotation.PreDestroy
    public void close() {
        factory.destroy();
    }
}
