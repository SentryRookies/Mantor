package com.skshieldus.demo1.controller;

import com.skshieldus.demo1.kafka.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class HelloController {

    @Autowired
    KafkaProducer kafkaProducer;

    @Value("${data.test}")
    String test;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, " + test + "!";
    }

    @PostMapping("/kafkasend")
    public String sendMessage(@RequestBody String message) {
        kafkaProducer.sendMessage(message);
        return "Message sent to the Kafka Topic java_in_use_topic Successfully";
    }

    


}
