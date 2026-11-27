package com.example.smart_learning.exam.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.exam.dto.WrongQuestionDto;
import com.example.smart_learning.exam.entity.WrongQuestion;
import com.example.smart_learning.exam.repository.WrongQuestionRepository;
import com.example.smart_learning.question.dto.QuestionOptionDto;
import com.example.smart_learning.question.entity.Question;
import com.example.smart_learning.question.entity.QuestionOption;
import com.example.smart_learning.question.repository.QuestionOptionRepository;
import com.example.smart_learning.question.repository.QuestionRepository;
import com.example.smart_learning.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class WrongQuestionService {

    private final WrongQuestionRepository wrongRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public WrongQuestionService(WrongQuestionRepository wrongRepository,
                                QuestionRepository questionRepository,
                                QuestionOptionRepository optionRepository) {
        this.wrongRepository = wrongRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<WrongQuestionDto> page(String subject, Boolean mastered, int page, int size) {
        Long userId = requireUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<WrongQuestion> result = wrongRepository.search(userId, subject, mastered, pageable);
        List<Long> questionIds = result.getContent().stream().map(WrongQuestion::getQuestionId).distinct().toList();
        Map<Long, Question> questions = questionIds.isEmpty() ? Map.of()
                : questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, List<QuestionOption>> options = questionIds.isEmpty() ? Map.of()
                : optionRepository.findByQuestionIdInOrderByQuestionIdAscSortAscIdAsc(questionIds).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));
        List<WrongQuestionDto> items = result.getContent().stream()
                .map(wrong -> {
                    Question question = questions.get(wrong.getQuestionId());
                    if (question == null) {
                        return null;
                    }
                    List<QuestionOption> questionOptions = options.getOrDefault(question.getId(), List.of());
                    return new WrongQuestionDto(wrong.getId(), question.getId(), question.getSubject(),
                            question.getChapter(), question.getType(), question.getStem(), question.getAnalysis(),
                            QuestionGrader.correctAnswerOf(questionOptions), wrong.getWrongCount(),
                            wrong.getLastWrongAt(), wrong.getMastered(),
                            questionOptions.stream().map(QuestionOptionDto::of).toList());
                })
                .filter(item -> item != null)
                .toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional
    public WrongQuestionDto setMastered(Long id, boolean mastered) {
        Long userId = requireUserId();
        WrongQuestion wrong = wrongRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WRONG_QUESTION_NOT_FOUND));
        wrong.setMastered(mastered);
        wrong = wrongRepository.save(wrong);
        Question question = questionRepository.findById(wrong.getQuestionId()).orElse(null);
        List<QuestionOption> options = optionRepository.findByQuestionIdOrderBySortAscIdAsc(wrong.getQuestionId());
        return new WrongQuestionDto(wrong.getId(), wrong.getQuestionId(),
                question == null ? null : question.getSubject(),
                question == null ? null : question.getChapter(),
                question == null ? null : question.getType(),
                question == null ? null : question.getStem(),
                question == null ? null : question.getAnalysis(),
                QuestionGrader.correctAnswerOf(options), wrong.getWrongCount(), wrong.getLastWrongAt(),
                wrong.getMastered(), options.stream().map(QuestionOptionDto::of).toList());
    }

    @Transactional
    public void remove(Long id) {
        Long userId = requireUserId();
        WrongQuestion wrong = wrongRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WRONG_QUESTION_NOT_FOUND));
        wrongRepository.delete(wrong);
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
