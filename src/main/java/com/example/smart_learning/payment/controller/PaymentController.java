package com.example.smart_learning.payment.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.payment.dto.ChannelDto;
import com.example.smart_learning.payment.dto.PayRequest;
import com.example.smart_learning.payment.dto.PaymentResultDto;
import com.example.smart_learning.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/channels")
    public ApiResponse<List<ChannelDto>> channels() {
        return ApiResponse.success(paymentService.channels());
    }

    @PostMapping("/{orderNo}/pay")
    public ApiResponse<PaymentResultDto> pay(@PathVariable String orderNo,
                                             @Valid @RequestBody PayRequest request) {
        return ApiResponse.success(paymentService.pay(orderNo, request.channel()));
    }
}
