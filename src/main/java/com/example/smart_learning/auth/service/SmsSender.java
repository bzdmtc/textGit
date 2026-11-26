package com.example.smart_learning.auth.service;

public interface SmsSender {

    void send(String phone, String code);
}
