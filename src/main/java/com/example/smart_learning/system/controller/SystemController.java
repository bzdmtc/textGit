package com.example.smart_learning.system.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.datasource.ReadOnly;
import com.example.smart_learning.security.RequiresPriv;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private static final String KAFKA_CHECK_TOPIC = "system-check";

    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public SystemController(JdbcTemplate jdbcTemplate, KafkaTemplate<String, String> kafkaTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/route-check/read")
    @ReadOnly
    @Transactional(readOnly = true)
    public ApiResponse<RouteCheck> readRoute() {
        return ApiResponse.success(new RouteCheck("READ", databaseName(), userCount()));
    }

    @GetMapping("/route-check/master")
    @Master
    @Transactional(readOnly = true)
    public ApiResponse<RouteCheck> masterRoute() {
        return ApiResponse.success(new RouteCheck("MASTER", databaseName(), userCount()));
    }

    @PostMapping("/kafka-check")
    @RequiresPriv(module = "admin", method = "access")
    public ApiResponse<KafkaCheck> kafkaCheck() throws Exception {
        var result = kafkaTemplate.send(KAFKA_CHECK_TOPIC, "ping").get(5, TimeUnit.SECONDS);
        return ApiResponse.success(new KafkaCheck(KAFKA_CHECK_TOPIC,
                result.getRecordMetadata().partition(), result.getRecordMetadata().offset()));
    }

    private String databaseName() {
        return jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
    }

    private Long userCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Long.class);
    }

    public record RouteCheck(String route, String database, Long userCount) {
    }

    public record KafkaCheck(String topic, int partition, long offset) {
    }
}
