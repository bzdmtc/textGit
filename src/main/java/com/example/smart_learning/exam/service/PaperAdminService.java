package com.example.smart_learning.exam.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.exam.dto.PaperDetailDto;
import com.example.smart_learning.exam.dto.PaperDto;
import com.example.smart_learning.exam.dto.PaperQuestionDto;
import com.example.smart_learning.exam.dto.PaperQuestionRequest;
import com.example.smart_learning.exam.dto.PaperRequest;
import com.example.smart_learning.exam.entity.Paper;
import com.example.smart_learning.exam.entity.PaperQuestion;
import com.example.smart_learning.exam.repository.PaperQuestionRepository;
import com.example.smart_learning.exam.repository.PaperRepository;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.permission.service.DataScopeService;
import com.example.smart_learning.question.dto.QuestionOptionDto;
import com.example.smart_learning.question.entity.Question;
import com.example.smart_learning.question.entity.QuestionOption;
import com.example.smart_learning.question.repository.QuestionOptionRepository;
import com.example.smart_learning.question.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Master
public class PaperAdminService {

    private final PaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;

    public PaperAdminService(PaperRepository paperRepository,
                             PaperQuestionRepository paperQuestionRepository,
                             QuestionRepository questionRepository,
                             QuestionOptionRepository optionRepository,
                             DataScopeService dataScopeService,
                             AuditService auditService) {
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<PaperDto> page(String keyword, String type, String subject, String status,
                                     int page, int size) {
        Long ownerId = dataScopeService.isAllScope() ? null : dataScopeService.currentUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<Paper> result = paperRepository.search(keyword, type, subject, status, ownerId, pageable);
        List<Paper> papers = result.getContent();
        Map<Long, Long> counts = papers.isEmpty() ? Map.of()
                : paperQuestionRepository.findByPaperIdIn(papers.stream().map(Paper::getId).toList()).stream()
                .collect(Collectors.groupingBy(PaperQuestion::getPaperId, Collectors.counting()));
        List<PaperDto> items = papers.stream()
                .map(paper -> toDto(paper, counts.getOrDefault(paper.getId(), 0L).intValue()))
                .toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public PaperDetailDto detail(Long id) {
        Paper paper = requirePaper(id);
        dataScopeService.assertOwnerAccess(paper.getOwnerUserId());
        List<PaperQuestion> relations = paperQuestionRepository.findByPaperIdOrderBySortAscIdAsc(id);
        List<Long> questionIds = relations.stream().map(PaperQuestion::getQuestionId).toList();
        Map<Long, Question> questions = questionIds.isEmpty() ? Map.of()
                : questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, item -> item));
        Map<Long, List<QuestionOption>> options = questionIds.isEmpty() ? Map.of()
                : optionRepository.findByQuestionIdInOrderByQuestionIdAscSortAscIdAsc(questionIds).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));
        List<PaperQuestionDto> items = relations.stream()
                .map(relation -> {
                    Question question = questions.get(relation.getQuestionId());
                    if (question == null) {
                        return null;
                    }
                    List<QuestionOption> questionOptions = options.getOrDefault(question.getId(), List.of());
                    return new PaperQuestionDto(question.getId(), question.getSubject(), question.getChapter(),
                            question.getType(), question.getStem(), question.getAnalysis(),
                            QuestionGrader.correctAnswerOf(questionOptions), relation.getScore(), relation.getSort(),
                            questionOptions.stream().map(QuestionOptionDto::of).toList());
                })
                .filter(item -> item != null)
                .toList();
        return new PaperDetailDto(toDto(paper, relations.size()), items);
    }

    @Transactional
    public PaperDetailDto create(PaperRequest request) {
        Paper paper = new Paper();
        paper.setOwnerUserId(dataScopeService.currentUserId());
        paper.setStatus("DRAFT");
        apply(paper, request);
        paper = paperRepository.save(paper);
        replaceQuestions(paper, request.questions());
        PaperDetailDto dto = detailInternal(paper);
        auditService.record("PAPER_CREATE", "PAPER", paper.getId(), null, dto.paper());
        return dto;
    }

    @Transactional
    public PaperDetailDto update(Long id, PaperRequest request) {
        Paper paper = requirePaper(id);
        dataScopeService.assertOwnerAccess(paper.getOwnerUserId());
        PaperDto before = toDto(paper, (int) paperQuestionRepository.countByPaperId(id));
        apply(paper, request);
        paper = paperRepository.save(paper);
        replaceQuestions(paper, request.questions());
        PaperDetailDto dto = detailInternal(paper);
        auditService.record("PAPER_UPDATE", "PAPER", id, before, dto.paper());
        return dto;
    }

    @Transactional
    public PaperDto changeStatus(Long id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"DRAFT".equals(normalized) && !"PUBLISHED".equals(normalized) && !"OFF".equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态只能是 DRAFT/PUBLISHED/OFF");
        }
        Paper paper = requirePaper(id);
        dataScopeService.assertOwnerAccess(paper.getOwnerUserId());
        if ("PUBLISHED".equals(normalized) && paperQuestionRepository.countByPaperId(id) == 0) {
            throw new BusinessException(ErrorCode.PAPER_NO_QUESTIONS);
        }
        PaperDto before = toDto(paper, (int) paperQuestionRepository.countByPaperId(id));
        paper.setStatus(normalized);
        paper = paperRepository.save(paper);
        PaperDto dto = toDto(paper, (int) paperQuestionRepository.countByPaperId(id));
        auditService.record("PAPER_STATUS", "PAPER", id, before, dto);
        return dto;
    }

    @Transactional
    public void disable(Long id) {
        Paper paper = requirePaper(id);
        dataScopeService.assertOwnerAccess(paper.getOwnerUserId());
        PaperDto before = toDto(paper, (int) paperQuestionRepository.countByPaperId(id));
        paper.setStatus("OFF");
        paperRepository.save(paper);
        auditService.record("PAPER_DISABLE", "PAPER", id, before, null);
    }

    private PaperDetailDto detailInternal(Paper paper) {
        List<PaperQuestion> relations = paperQuestionRepository.findByPaperIdOrderBySortAscIdAsc(paper.getId());
        List<Long> questionIds = relations.stream().map(PaperQuestion::getQuestionId).toList();
        Map<Long, Question> questions = questionIds.isEmpty() ? Map.of()
                : questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, item -> item));
        Map<Long, List<QuestionOption>> options = questionIds.isEmpty() ? Map.of()
                : optionRepository.findByQuestionIdInOrderByQuestionIdAscSortAscIdAsc(questionIds).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        List<PaperQuestionDto> items = relations.stream()
                .map(relation -> {
                    Question question = questions.get(relation.getQuestionId());
                    if (question == null) {
                        return null;
                    }
                    List<QuestionOption> questionOptions = options.getOrDefault(question.getId(), List.of());
                    return new PaperQuestionDto(question.getId(), question.getSubject(), question.getChapter(),
                            question.getType(), question.getStem(), question.getAnalysis(),
                            QuestionGrader.correctAnswerOf(questionOptions), relation.getScore(), relation.getSort(),
                            questionOptions.stream().map(QuestionOptionDto::of).toList());
                })
                .filter(item -> item != null)
                .toList();
        return new PaperDetailDto(toDto(paper, relations.size()), items);
    }

    private void apply(Paper paper, PaperRequest request) {
        String type = normalizeType(request.type());
        paper.setTitle(request.title().trim());
        paper.setType(type);
        paper.setSubject(request.subject().trim());
        paper.setChapter(blankToNull(request.chapter()));
        paper.setDescription(blankToNull(request.description()));
        paper.setDurationMin(request.durationMin());
    }

    private void replaceQuestions(Paper paper, List<PaperQuestionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCode.PAPER_NO_QUESTIONS);
        }
        Set<Long> seen = new HashSet<>();
        List<Long> ids = new ArrayList<>();
        for (PaperQuestionRequest request : requests) {
            if (!seen.add(request.questionId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷中存在重复题目");
            }
            ids.add(request.questionId());
        }
        List<Question> questions = questionRepository.findByIdInAndStatus(ids, "ACTIVE");
        if (questions.size() != ids.size()) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND, "存在不存在或已停用的题目");
        }
        paperQuestionRepository.deleteByPaperId(paper.getId());
        paperQuestionRepository.flush();
        BigDecimal total = BigDecimal.ZERO;
        int sort = 1;
        for (PaperQuestionRequest request : requests) {
            PaperQuestion relation = new PaperQuestion();
            relation.setPaperId(paper.getId());
            relation.setQuestionId(request.questionId());
            relation.setScore(request.score());
            relation.setSort(sort++);
            paperQuestionRepository.save(relation);
            total = total.add(request.score());
        }
        paper.setTotalScore(total);
        paperRepository.save(paper);
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        if (!"CHAPTER".equals(normalized) && !"PAST".equals(normalized) && !"MOCK".equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷类型只能是 CHAPTER/PAST/MOCK");
        }
        return normalized;
    }

    private Paper requirePaper(Long id) {
        return paperRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
    }

    private PaperDto toDto(Paper paper, int questionCount) {
        return new PaperDto(paper.getId(), paper.getTitle(), paper.getType(), paper.getSubject(),
                paper.getChapter(), paper.getDescription(), paper.getTotalScore(), paper.getDurationMin(),
                paper.getStatus(), paper.getOwnerUserId(), questionCount, paper.getCreatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
