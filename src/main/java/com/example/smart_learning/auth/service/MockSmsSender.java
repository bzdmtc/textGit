package com.example.smart_learning.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void send(String phone, String code) {
        // Demo sender: the code is written to the backend log only.
        log.info("[MOCK SMS] phone={} code={}", phone, code);
    }
}
