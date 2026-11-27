package com.example.smart_learning.question.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.exam.repository.PaperQuestionRepository;
import com.example.smart_learning.exam.service.QuestionGrader;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.permission.service.DataScopeService;
import com.example.smart_learning.question.dto.OptionRequest;
import com.example.smart_learning.question.dto.QuestionDto;
import com.example.smart_learning.question.dto.QuestionOptionDto;
import com.example.smart_learning.question.dto.QuestionRequest;
import com.example.smart_learning.question.entity.Question;
import com.example.smart_learning.question.entity.QuestionOption;
import com.example.smart_learning.question.repository.QuestionOptionRepository;
import com.example.smart_learning.question.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class QuestionAdminService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;

    public QuestionAdminService(QuestionRepository questionRepository,
                                QuestionOptionRepository optionRepository,
                                PaperQuestionRepository paperQuestionRepository,
                                DataScopeService dataScopeService,
                                AuditService auditService) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<QuestionDto> page(String keyword, String subject, String type, String status,
                                        int page, int size) {
        Long ownerId = dataScopeService.isAllScope() ? null : dataScopeService.currentUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<Question> result = questionRepository.search(keyword, subject, type, status, ownerId, pageable);
        return PageResult.of(toDtos(result.getContent()), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public QuestionDto detail(Long id) {
        Question question = requireQuestion(id);
        dataScopeService.assertOwnerAccess(question.getOwnerUserId());
        return toDto(question, optionsOf(id));
    }

    @Transactional
    public QuestionDto create(QuestionRequest request) {
        Question question = new Question();
        question.setOwnerUserId(dataScopeService.currentUserId());
        apply(question, request);
        question = questionRepository.save(question);
        List<QuestionOption> options = replaceOptions(question, request.options());
        QuestionDto dto = toDto(question, options);
        auditService.record("QUESTION_CREATE", "QUESTION", question.getId(), null, dto);
        return dto;
    }

    @Transactional
    public QuestionDto update(Long id, QuestionRequest request) {
        Question question = requireQuestion(id);
        dataScopeService.assertOwnerAccess(question.getOwnerUserId());
        QuestionDto before = toDto(question, optionsOf(id));
        apply(question, request);
        question = questionRepository.save(question);
        List<QuestionOption> options = replaceOptions(question, request.options());
        QuestionDto dto = toDto(question, options);
        auditService.record("QUESTION_UPDATE", "QUESTION", id, before, dto);
        return dto;
    }

    @Transactional
    public QuestionDto changeStatus(Long id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"DISABLED".equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态只能是 ACTIVE/DISABLED");
        }
        Question question = requireQuestion(id);
        dataScopeService.assertOwnerAccess(question.getOwnerUserId());
        if ("DISABLED".equals(normalized) && paperQuestionRepository.countByQuestionId(id) > 0) {
            throw new BusinessException(ErrorCode.QUESTION_IN_USE);
        }
        QuestionDto before = toDto(question, optionsOf(id));
        question.setStatus(normalized);
        question = questionRepository.save(question);
        QuestionDto dto = toDto(question, optionsOf(id));
        auditService.record("QUESTION_STATUS", "QUESTION", id, before, dto);
        return dto;
    }

    private void apply(Question question, QuestionRequest request) {
        String type = normalizeType(request.type());
        validateOptions(type, request.options());
        question.setSubject(request.subject().trim());
        question.setChapter(request.chapter().trim());
        question.setType(type);
        question.setStem(request.stem().trim());
        question.setAnalysis(blankToNull(request.analysis()));
        question.setDifficulty(request.difficulty() == null ? 3 : request.difficulty());
    }

    private List<QuestionOption> replaceOptions(Question question, List<OptionRequest> requests) {
        optionRepository.deleteByQuestionId(question.getId());
        optionRepository.flush();
        List<QuestionOption> options = new ArrayList<>();
        int index = 1;
        Set<String> labels = new HashSet<>();
        for (OptionRequest request : requests) {
            String label = request.label().trim().toUpperCase();
            if (!labels.add(label)) {
                throw new BusinessException(ErrorCode.QUESTION_INVALID, "选项标签重复：" + label);
            }
            QuestionOption option = new QuestionOption();
            option.setQuestionId(question.getId());
            option.setLabel(label);
            option.setContent(request.content().trim());
            option.setIsCorrect(Boolean.TRUE.equals(request.isCorrect()));
            option.setSort(request.sort() == null ? index : request.sort());
            options.add(optionRepository.save(option));
            index++;
        }
        return options;
    }

    private void validateOptions(String type, List<OptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw new BusinessException(ErrorCode.QUESTION_INVALID, "至少需要两个选项");
        }
        if ("JUDGE".equals(type) && options.size() != 2) {
            throw new BusinessException(ErrorCode.QUESTION_INVALID, "判断题必须只有两个选项");
        }
        long correct = options.stream().filter(item -> Boolean.TRUE.equals(item.isCorrect())).count();
        if ("SINGLE".equals(type) || "JUDGE".equals(type)) {
            if (correct != 1) {
                throw new BusinessException(ErrorCode.QUESTION_INVALID,
                        "SINGLE".equals(type) ? "单选题必须且只能有一个正确选项" : "判断题必须且只能有一个正确选项");
            }
        } else if (correct < 2) {
            throw new BusinessException(ErrorCode.QUESTION_INVALID, "多选题至少需要两个正确选项");
        }
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        if (!"SINGLE".equals(normalized) && !"MULTIPLE".equals(normalized) && !"JUDGE".equals(normalized)) {
            throw new BusinessException(ErrorCode.QUESTION_INVALID, "题型只能是 SINGLE/MULTIPLE/JUDGE");
        }
        return normalized;
    }

    private Question requireQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private List<QuestionOption> optionsOf(Long questionId) {
        return optionRepository.findByQuestionIdOrderBySortAscIdAsc(questionId);
    }

    private List<QuestionDto> toDtos(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        List<Long> ids = questions.stream().map(Question::getId).toList();
        Map<Long, List<QuestionOption>> optionMap = optionRepository
                .findByQuestionIdInOrderByQuestionIdAscSortAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        return questions.stream()
                .map(question -> toDto(question, optionMap.getOrDefault(question.getId(), List.of())))
                .toList();
    }

    private QuestionDto toDto(Question question, List<QuestionOption> options) {
        return new QuestionDto(question.getId(), question.getSubject(), question.getChapter(), question.getType(),
                question.getStem(), question.getAnalysis(), question.getDifficulty(), question.getStatus(),
                question.getOwnerUserId(), QuestionGrader.correctAnswerOf(options),
                options.stream().map(QuestionOptionDto::of).toList(), question.getCreatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
