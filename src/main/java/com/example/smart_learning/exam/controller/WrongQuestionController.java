package com.example.smart_learning.exam.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.exam.dto.WrongMasteredRequest;
import com.example.smart_learning.exam.dto.WrongQuestionDto;
import com.example.smart_learning.exam.service.WrongQuestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wrong-questions")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    @GetMapping
    public ApiResponse<PageResult<WrongQuestionDto>> page(@RequestParam(required = false) String subject,
                                                          @RequestParam(required = false) Boolean mastered,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(wrongQuestionService.page(subject, mastered, page, size));
    }

    @PutMapping("/{id}/mastered")
    public ApiResponse<WrongQuestionDto> mastered(@PathVariable Long id,
                                                  @Valid @RequestBody WrongMasteredRequest request) {
        return ApiResponse.success(wrongQuestionService.setMastered(id, request.mastered()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        wrongQuestionService.remove(id);
        return ApiResponse.success();
    }
}
