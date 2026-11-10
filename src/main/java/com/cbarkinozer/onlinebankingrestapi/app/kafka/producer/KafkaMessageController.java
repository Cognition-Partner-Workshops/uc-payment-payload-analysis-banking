package com.cbarkinozer.onlinebankingrestapi.app.kafka.producer;

import com.cbarkinozer.onlinebankingrestapi.app.kafka.dto.LogMessage;
import com.cbarkinozer.onlinebankingrestapi.app.log.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka-messages")
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageController {

    private final LogService logService;

    @PostMapping
    public void sendMessage(@RequestBody LogMessage logMessage){

        log.info("Starting to produce");

        logService.log(logMessage);

        log.info("Message sent from producer");
    }
}
