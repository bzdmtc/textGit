package com.example.smart_learning.exam.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.exam.dto.AnswerFeedbackDto;
import com.example.smart_learning.exam.dto.ExamAnswerStateDto;
import com.example.smart_learning.exam.dto.ExamOptionDto;
import com.example.smart_learning.exam.dto.ExamQuestionDto;
import com.example.smart_learning.exam.dto.ExamRecordDto;
import com.example.smart_learning.exam.dto.ExamResultDto;
import com.example.smart_learning.exam.dto.ExamResultQuestionDto;
import com.example.smart_learning.exam.dto.ExamSessionDto;
import com.example.smart_learning.exam.dto.PaperDto;
import com.example.smart_learning.exam.dto.PracticeChapterDto;
import com.example.smart_learning.exam.dto.PracticeOverviewDto;
import com.example.smart_learning.exam.dto.PracticeStatsDto;
import com.example.smart_learning.exam.dto.PracticeSubjectDto;
import com.example.smart_learning.exam.dto.QuestionOptionSnapshot;
import com.example.smart_learning.exam.dto.QuestionSnapshot;
import com.example.smart_learning.exam.dto.SaveAnswerRequest;
import com.example.smart_learning.exam.dto.StartExamRequest;
import com.example.smart_learning.exam.dto.StartPracticeRequest;
import com.example.smart_learning.exam.entity.ExamAnswer;
import com.example.smart_learning.exam.entity.ExamRecord;
import com.example.smart_learning.exam.entity.Paper;
import com.example.smart_learning.exam.entity.PaperQuestion;
import com.example.smart_learning.exam.entity.WrongQuestion;
import com.example.smart_learning.exam.repository.ExamAnswerRepository;
import com.example.smart_learning.exam.repository.ExamRecordRepository;
import com.example.smart_learning.exam.repository.PaperQuestionRepository;
import com.example.smart_learning.exam.repository.PaperRepository;
import com.example.smart_learning.exam.repository.WrongQuestionRepository;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class ExamService {

    private static final BigDecimal PRACTICE_SCORE = new BigDecimal("5.00");
    private static final long EXAM_GRACE_SECONDS = 60;

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final PaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final ExamRecordRepository recordRepository;
    private final ExamAnswerRepository answerRepository;
    private final WrongQuestionRepository wrongRepository;
    private final ObjectMapper objectMapper;

    public ExamService(QuestionRepository questionRepository,
                       QuestionOptionRepository optionRepository,
                       PaperRepository paperRepository,
                       PaperQuestionRepository paperQuestionRepository,
                       ExamRecordRepository recordRepository,
                       ExamAnswerRepository answerRepository,
                       WrongQuestionRepository wrongRepository,
                       ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.recordRepository = recordRepository;
        this.answerRepository = answerRepository;
        this.wrongRepository = wrongRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PracticeOverviewDto overview() {
        Long userId = requireUserId();
        List<Object[]> rows = questionRepository.countBySubjectChapter();
        Map<String, List<PracticeChapterDto>> chaptersBySubject = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String subject = String.valueOf(row[0]);
            String chapter = String.valueOf(row[1]);
            int count = ((Number) row[2]).intValue();
            chaptersBySubject.computeIfAbsent(subject, key -> new ArrayList<>())
                    .add(new PracticeChapterDto(chapter, count));
        }
        List<PracticeSubjectDto> subjects = chaptersBySubject.entrySet().stream()
                .map(entry -> new PracticeSubjectDto(entry.getKey(),
                        entry.getValue().stream().mapToInt(PracticeChapterDto::questionCount).sum(),
                        entry.getValue()))
                .toList();
        List<PaperDto> papers = paperRepository.findByStatusOrderByIdDesc("PUBLISHED").stream()
                .map(this::toPaperDto)
                .toList();
        PracticeStatsDto stats = stats(userId);
        List<ExamRecordDto> recentRecords = recordRepository.findTop5ByUserIdOrderByIdDesc(userId).stream()
                .map(this::toRecordDto)
                .toList();
        return new PracticeOverviewDto(subjects, papers, stats, recentRecords);
    }

    @Transactional
    public ExamSessionDto startPractice(StartPracticeRequest request) {
        Long userId = requireUserId();
        String source = "WRONG".equalsIgnoreCase(request.source()) ? "WRONG" : "NORMAL";
        String subject = blankToNull(request.subject());
        String chapter = blankToNull(request.chapter());
        int count = request.count() == null ? 10 : Math.min(Math.max(request.count(), 1), 50);

        List<Question> questions;
        if ("WRONG".equals(source)) {
            List<Long> wrongIds = wrongRepository.findPracticeQuestionIds(userId, subject).stream()
                    .limit(count)
                    .toList();
            if (wrongIds.isEmpty()) {
                throw new BusinessException(ErrorCode.WRONG_QUESTION_EMPTY);
            }
            Map<Long, Question> loaded = questionRepository.findAllById(wrongIds).stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));
            questions = wrongIds.stream().map(loaded::get).filter(item -> item != null).toList();
            if (questions.isEmpty()) {
                throw new BusinessException(ErrorCode.WRONG_QUESTION_EMPTY);
            }
        } else {
            if (subject == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择练习科目");
            }
            questions = questionRepository.findPracticePool(subject, chapter, PageRequest.of(0, count));
            if (questions.isEmpty()) {
                throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND, "该科目/章节暂无可用题目");
            }
        }

        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setMode("PRACTICE");
        record.setSource(source);
        record.setSubject(subject);
        record.setChapter(chapter);
        record.setTitle(practiceTitle(source, subject, chapter));
        record.setStartedAt(now());
        record.setQuestionCount(questions.size());
        record.setTotalScore(PRACTICE_SCORE.multiply(BigDecimal.valueOf(questions.size())));
        record.setStatus("IN_PROGRESS");
        record = recordRepository.save(record);

        createAnswerSnapshots(record, questions, PRACTICE_SCORE);
        return session(record.getId());
    }

    @Transactional
    public ExamSessionDto startExam(StartExamRequest request) {
        Long userId = requireUserId();
        Paper paper = paperRepository.findById(request.paperId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
        if (!"PUBLISHED".equals(paper.getStatus())) {
            throw new BusinessException(ErrorCode.PAPER_UNAVAILABLE);
        }
        List<PaperQuestion> relations = paperQuestionRepository.findByPaperIdOrderBySortAscIdAsc(paper.getId());
        if (relations.isEmpty()) {
            throw new BusinessException(ErrorCode.PAPER_NO_QUESTIONS);
        }
        List<Long> ids = relations.stream().map(PaperQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = questionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        if (questionMap.size() != ids.size()) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND, "试卷中存在已被删除的题目");
        }

        LocalDateTime startedAt = now();
        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setPaperId(paper.getId());
        record.setMode("EXAM");
        record.setSource("NORMAL");
        record.setSubject(paper.getSubject());
        record.setChapter(paper.getChapter());
        record.setTitle(paper.getTitle());
        record.setStartedAt(startedAt);
        record.setDeadlineAt(startedAt.plusMinutes(Math.max(paper.getDurationMin(), 1)));
        record.setQuestionCount(relations.size());
        record.setTotalScore(paper.getTotalScore());
        record.setStatus("IN_PROGRESS");
        record = recordRepository.save(record);

        Map<Long, List<QuestionOption>> optionMap = loadOptions(ids);
        for (PaperQuestion relation : relations) {
            Question question = questionMap.get(relation.getQuestionId());
            QuestionSnapshot snapshot = snapshotOf(question,
                    optionMap.getOrDefault(question.getId(), List.of()), relation.getScore(), relation.getSort());
            ExamAnswer answer = new ExamAnswer();
            answer.setRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setQuestionSnapshot(toJson(snapshot));
            answerRepository.save(answer);
        }
        return session(record.getId());
    }

    @Transactional
    public ExamSessionDto session(Long recordId) {
        Long userId = requireUserId();
        ExamRecord record = requireRecord(recordId, userId);
        if ("IN_PROGRESS".equals(record.getStatus()) && isExamExpired(record, 0)) {
            grade(record);
        }
        return buildSession(record);
    }

    @Transactional
    public AnswerFeedbackDto saveAnswer(Long recordId, SaveAnswerRequest request) {
        Long userId = requireUserId();
        ExamRecord record = requireRecord(recordId, userId);
        if (!"IN_PROGRESS".equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        }
        if ("EXAM".equals(record.getMode()) && isExamExpired(record, EXAM_GRACE_SECONDS)) {
            // 不在本事务内判分（抛异常会回滚）；前端收到 50009 后调用交卷接口完成判分
            throw new BusinessException(ErrorCode.EXAM_TIME_UP);
        }
        ExamAnswer answer = answerRepository.findByRecordIdAndQuestionId(recordId, request.questionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND, "题目不属于本次答题"));
        QuestionSnapshot snapshot = readSnapshot(answer);
        String normalized = QuestionGrader.normalize(request.answer());
        if (!QuestionGrader.isValidSelection(normalized,
                snapshot.options().stream().map(QuestionOptionSnapshot::label).toList())) {
            throw new BusinessException(ErrorCode.ANSWER_INVALID);
        }
        answer.setAnswer(normalized.isEmpty() ? null : normalized);

        if ("PRACTICE".equals(record.getMode())) {
            if (normalized.isEmpty()) {
                answer.setIsCorrect(null);
                answer.setScore(null);
                answerRepository.save(answer);
                return new AnswerFeedbackDto(snapshot.questionId(), "", null, null, null, null);
            }
            boolean correct = QuestionGrader.isCorrect(snapshot.correctAnswer(), normalized);
            BigDecimal earned = correct ? snapshot.score() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            answer.setIsCorrect(correct);
            answer.setScore(earned);
            answerRepository.save(answer);
            if (!correct) {
                recordWrong(userId, snapshot.questionId());
            }
            if (correct && "WRONG".equals(record.getSource())) {
                markMastered(userId, snapshot.questionId());
            }
            return new AnswerFeedbackDto(snapshot.questionId(), normalized, correct, earned,
                    snapshot.correctAnswer(), snapshot.analysis());
        }

        answer.setIsCorrect(null);
        answer.setScore(null);
        answerRepository.save(answer);
        return new AnswerFeedbackDto(snapshot.questionId(), normalized, null, null, null, null);
    }

    @Transactional
    public ExamResultDto submit(Long recordId) {
        Long userId = requireUserId();
        ExamRecord record = requireRecord(recordId, userId);
        if ("IN_PROGRESS".equals(record.getStatus())) {
            grade(record);
        }
        return result(recordId);
    }

    @Transactional(readOnly = true)
    public ExamResultDto result(Long recordId) {
        Long userId = requireUserId();
        ExamRecord record = requireRecord(recordId, userId);
        if (!"SUBMITTED".equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.EXAM_NOT_SUBMITTED);
        }
        List<ExamAnswer> answers = answerRepository.findByRecordIdOrderByIdAsc(recordId);
        int unanswered = 0;
        List<ExamResultQuestionDto> questions = new ArrayList<>();
        for (ExamAnswer answer : answers) {
            QuestionSnapshot snapshot = readSnapshot(answer);
            String myAnswer = answer.getAnswer();
            if (myAnswer == null || myAnswer.isBlank()) {
                unanswered++;
            }
            questions.add(new ExamResultQuestionDto(snapshot.questionId(), snapshot.subject(), snapshot.chapter(),
                    snapshot.type(), snapshot.stem(),
                    snapshot.options().stream().map(item -> new ExamOptionDto(item.label(), item.content())).toList(),
                    snapshot.correctAnswer(), myAnswer, answer.getIsCorrect(), snapshot.score(),
                    answer.getScore() == null ? BigDecimal.ZERO : answer.getScore(), snapshot.analysis()));
        }
        return new ExamResultDto(toRecordDto(record), record.getScore() == null ? BigDecimal.ZERO : record.getScore(),
                record.getTotalScore(), record.getCorrectCount(), record.getWrongCount(), unanswered,
                durationSeconds(record), questions);
    }

    @Transactional(readOnly = true)
    public PageResult<ExamRecordDto> records(int page, int size) {
        Long userId = requireUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 50));
        Page<ExamRecord> result = recordRepository.findByUserIdOrderByIdDesc(userId, pageable);
        return PageResult.of(result.getContent().stream().map(this::toRecordDto).toList(),
                result.getTotalElements(), page, size);
    }

    private void grade(ExamRecord record) {
        List<ExamAnswer> answers = answerRepository.findByRecordIdOrderByIdAsc(record.getId());
        BigDecimal total = BigDecimal.ZERO;
        int correctCount = 0;
        int wrongCount = 0;
        for (ExamAnswer answer : answers) {
            QuestionSnapshot snapshot = readSnapshot(answer);
            String myAnswer = answer.getAnswer();
            boolean answered = myAnswer != null && !myAnswer.isBlank();
            boolean correct = answered && QuestionGrader.isCorrect(snapshot.correctAnswer(), myAnswer);
            BigDecimal earned = correct ? snapshot.score() : BigDecimal.ZERO;
            answer.setIsCorrect(correct);
            answer.setScore(earned);
            answerRepository.save(answer);
            total = total.add(earned);
            if (correct) {
                correctCount++;
            } else if (answered) {
                // 未作答单独在成绩报告中统计，不重复计入答错
                wrongCount++;
                // 练习模式在保存答案时已经入库，避免交卷时重复累计
                if ("EXAM".equals(record.getMode())) {
                    recordWrong(record.getUserId(), snapshot.questionId());
                }
            }
        }
        record.setScore(total);
        record.setCorrectCount(correctCount);
        record.setWrongCount(wrongCount);
        record.setStatus("SUBMITTED");
        LocalDateTime now = now();
        if (record.getDeadlineAt() != null && now.isAfter(record.getDeadlineAt())) {
            record.setSubmittedAt(record.getDeadlineAt());
        } else {
            record.setSubmittedAt(now);
        }
        recordRepository.save(record);
    }

    private ExamSessionDto buildSession(ExamRecord record) {
        List<ExamAnswer> answers = answerRepository.findByRecordIdOrderByIdAsc(record.getId());
        List<ExamQuestionDto> questions = new ArrayList<>();
        List<ExamAnswerStateDto> states = new ArrayList<>();
        boolean practice = "PRACTICE".equals(record.getMode());
        for (ExamAnswer answer : answers) {
            QuestionSnapshot snapshot = readSnapshot(answer);
            questions.add(new ExamQuestionDto(snapshot.questionId(), snapshot.type(), snapshot.stem(),
                    snapshot.options().stream()
                            .map(item -> new ExamOptionDto(item.label(), item.content()))
                            .toList(),
                    snapshot.score(), snapshot.sort()));
            if (practice) {
                boolean judged = answer.getIsCorrect() != null;
                states.add(new ExamAnswerStateDto(snapshot.questionId(), answer.getAnswer(),
                        judged ? answer.getIsCorrect() : null,
                        judged ? answer.getScore() : null,
                        judged ? snapshot.correctAnswer() : null,
                        judged ? snapshot.analysis() : null));
            } else {
                states.add(new ExamAnswerStateDto(snapshot.questionId(), answer.getAnswer(), null, null, null, null));
            }
        }
        questions.sort(Comparator.comparing(ExamQuestionDto::sort,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Long remaining = null;
        if ("EXAM".equals(record.getMode()) && "IN_PROGRESS".equals(record.getStatus())
                && record.getDeadlineAt() != null) {
            remaining = Math.max(0, Duration.between(now(), record.getDeadlineAt()).getSeconds());
        }
        return new ExamSessionDto(record.getId(), record.getTitle(), record.getMode(), record.getSource(),
                record.getStatus(), record.getTotalScore(), record.getStartedAt(), record.getDeadlineAt(),
                remaining, questions, states);
    }

    private void createAnswerSnapshots(ExamRecord record, List<Question> questions, BigDecimal score) {
        Map<Long, List<QuestionOption>> optionMap = loadOptions(questions.stream().map(Question::getId).toList());
        int sort = 1;
        for (Question question : questions) {
            QuestionSnapshot snapshot = snapshotOf(question,
                    optionMap.getOrDefault(question.getId(), List.of()), score, sort++);
            ExamAnswer answer = new ExamAnswer();
            answer.setRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setQuestionSnapshot(toJson(snapshot));
            answerRepository.save(answer);
        }
    }

    private Map<Long, List<QuestionOption>> loadOptions(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findByQuestionIdInOrderByQuestionIdAscSortAscIdAsc(questionIds).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));
    }

    private QuestionSnapshot snapshotOf(Question question, List<QuestionOption> options,
                                        BigDecimal score, int sort) {
        return new QuestionSnapshot(question.getId(), question.getSubject(), question.getChapter(),
                question.getType(), question.getStem(),
                options.stream().map(item -> new QuestionOptionSnapshot(item.getLabel(), item.getContent())).toList(),
                QuestionGrader.correctAnswerOf(options), question.getAnalysis(), score, sort);
    }

    private PracticeStatsDto stats(Long userId) {
        long attemptCount = recordRepository.countByUserIdAndStatus(userId, "SUBMITTED");
        BigDecimal scoreSum = BigDecimal.ZERO;
        BigDecimal totalSum = BigDecimal.ZERO;
        long correctSum = 0;
        long questionSum = 0;
        List<Object[]> rows = recordRepository.summarize(userId);
        if (!rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            scoreSum = toDecimal(row[0]);
            totalSum = toDecimal(row[1]);
            correctSum = ((Number) row[2]).longValue();
            questionSum = ((Number) row[3]).longValue();
        }
        BigDecimal averagePercent = percent(scoreSum, totalSum);
        BigDecimal correctRate = percent(BigDecimal.valueOf(correctSum), BigDecimal.valueOf(questionSum));
        long wrongCount = wrongRepository.countByUserIdAndMastered(userId, false);
        long masteredCount = wrongRepository.countByUserIdAndMastered(userId, true);
        return new PracticeStatsDto((int) attemptCount, averagePercent, correctRate, wrongCount, masteredCount);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private void recordWrong(Long userId, Long questionId) {
        WrongQuestion wrong = wrongRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseGet(WrongQuestion::new);
        boolean created = wrong.getId() == null;
        wrong.setUserId(userId);
        wrong.setQuestionId(questionId);
        wrong.setWrongCount(created ? 1 : (wrong.getWrongCount() == null ? 0 : wrong.getWrongCount()) + 1);
        wrong.setLastWrongAt(now());
        wrong.setMastered(false);
        wrongRepository.save(wrong);
    }

    private void markMastered(Long userId, Long questionId) {
        wrongRepository.findByUserIdAndQuestionId(userId, questionId).ifPresent(wrong -> {
            wrong.setMastered(true);
            wrongRepository.save(wrong);
        });
    }

    private boolean isExamExpired(ExamRecord record, long graceSeconds) {
        return "EXAM".equals(record.getMode()) && record.getDeadlineAt() != null
                && now().isAfter(record.getDeadlineAt().plusSeconds(graceSeconds));
    }

    private ExamRecord requireRecord(Long recordId, Long userId) {
        return recordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String practiceTitle(String source, String subject, String chapter) {
        if ("WRONG".equals(source)) {
            return subject == null ? "错题重做" : subject + " · 错题重做";
        }
        return subject + (chapter == null ? "" : " · " + chapter) + " · 章节练习";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String toJson(QuestionSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "题目快照序列化失败");
        }
    }

    private QuestionSnapshot readSnapshot(ExamAnswer answer) {
        try {
            return objectMapper.readValue(answer.getQuestionSnapshot(), QuestionSnapshot.class);
        } catch (JacksonException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "题目快照解析失败");
        }
    }

    private ExamRecordDto toRecordDto(ExamRecord record) {
        return new ExamRecordDto(record.getId(), record.getPaperId(), record.getTitle(), record.getMode(),
                record.getSource(), record.getSubject(), record.getChapter(), record.getStatus(), record.getScore(),
                record.getTotalScore(), record.getCorrectCount(), record.getWrongCount(), record.getQuestionCount(),
                record.getStartedAt(), record.getSubmittedAt(), durationSeconds(record));
    }

    private Long durationSeconds(ExamRecord record) {
        if (record.getStartedAt() == null || record.getSubmittedAt() == null) {
            return null;
        }
        return Math.max(0, Duration.between(record.getStartedAt(), record.getSubmittedAt()).getSeconds());
    }

    private PaperDto toPaperDto(Paper paper) {
        int questionCount = (int) paperQuestionRepository.countByPaperId(paper.getId());
        return new PaperDto(paper.getId(), paper.getTitle(), paper.getType(), paper.getSubject(),
                paper.getChapter(), paper.getDescription(), paper.getTotalScore(), paper.getDurationMin(),
                paper.getStatus(), paper.getOwnerUserId(), questionCount, paper.getCreatedAt());
    }
}
