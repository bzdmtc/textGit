package com.example.smart_learning.university.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.security.SecurityUtils;
import com.example.smart_learning.university.dto.AdmissionStatDto;
import com.example.smart_learning.university.dto.AdmissionStatRequest;
import com.example.smart_learning.university.dto.MajorDto;
import com.example.smart_learning.university.dto.MajorRequest;
import com.example.smart_learning.university.dto.ScoreLineDto;
import com.example.smart_learning.university.dto.ScoreLineRequest;
import com.example.smart_learning.university.dto.UniversityDetailDto;
import com.example.smart_learning.university.dto.UniversityDto;
import com.example.smart_learning.university.dto.UniversityOptionsDto;
import com.example.smart_learning.university.dto.UniversityRequest;
import com.example.smart_learning.university.entity.AdmissionStat;
import com.example.smart_learning.university.entity.ScoreLine;
import com.example.smart_learning.university.entity.University;
import com.example.smart_learning.university.entity.UniversityMajor;
import com.example.smart_learning.university.repository.AdmissionStatRepository;
import com.example.smart_learning.university.repository.ScoreLineRepository;
import com.example.smart_learning.university.repository.UniversityMajorRepository;
import com.example.smart_learning.university.repository.UniversityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Master
public class UniversityAdminService {

    private final UniversityRepository universityRepository;
    private final UniversityMajorRepository majorRepository;
    private final ScoreLineRepository scoreLineRepository;
    private final AdmissionStatRepository admissionStatRepository;
    private final AuditService auditService;

    public UniversityAdminService(UniversityRepository universityRepository,
                                  UniversityMajorRepository majorRepository,
                                  ScoreLineRepository scoreLineRepository,
                                  AdmissionStatRepository admissionStatRepository,
                                  AuditService auditService) {
        this.universityRepository = universityRepository;
        this.majorRepository = majorRepository;
        this.scoreLineRepository = scoreLineRepository;
        this.admissionStatRepository = admissionStatRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<UniversityDto> page(String keyword, String province, String level, String type,
                                          String status, int page, int size) {
        Page<University> result = universityRepository.search(blankToNull(keyword), blankToNull(province),
                blankToNull(level), blankToNull(type), blankToNull(status),
                PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100)));
        List<Long> ids = result.getContent().stream().map(University::getId).toList();
        Map<Long, Long> counts = ids.isEmpty() ? Map.of()
                : majorRepository.countActiveByUniversityIds(ids).stream()
                .collect(Collectors.toMap(UniversityMajorRepository.MajorCount::getUniversityId,
                        UniversityMajorRepository.MajorCount::getTotal));
        List<UniversityDto> items = result.getContent().stream()
                .map(university -> UniversityDto.of(university,
                        counts.getOrDefault(university.getId(), 0L)))
                .toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public UniversityOptionsDto options() {
        return new UniversityOptionsDto(universityRepository.distinctProvinces(),
                universityRepository.distinctLevels(), universityRepository.distinctTypes());
    }

    @Transactional(readOnly = true)
    public UniversityDetailDto detail(Long id) {
        University university = requireUniversity(id);
        List<UniversityMajor> majors = majorRepository.findByUniversityIdOrderBySortAscIdAsc(id);
        Map<Long, String> majorNames = majors.stream()
                .collect(Collectors.toMap(UniversityMajor::getId, UniversityMajor::getName));
        return new UniversityDetailDto(
                UniversityDto.of(university, majors.stream()
                        .filter(major -> "ACTIVE".equals(major.getStatus())).count()),
                majors.stream().map(MajorDto::of).toList(),
                scoreLineRepository.findByUniversityIdOrderByYearDescMajorIdAsc(id).stream()
                        .map(line -> ScoreLineDto.of(line, majorNames.get(line.getMajorId())))
                        .toList(),
                admissionStatRepository.findByUniversityIdOrderByYearDescMajorIdAsc(id).stream()
                        .map(stat -> AdmissionStatDto.of(stat, majorNames.get(stat.getMajorId())))
                        .toList());
    }

    @Transactional
    public UniversityDto createUniversity(UniversityRequest request) {
        String name = request.name().trim();
        if (universityRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.CONFLICT, "院校名称已存在");
        }
        University university = new University();
        university.setOwnerUserId(SecurityUtils.currentUserId());
        apply(university, request);
        university = universityRepository.save(university);
        auditService.record("UNIVERSITY_CREATE", "UNIVERSITY", university.getId(), null,
                Map.of("name", university.getName(), "province", university.getProvince()));
        return UniversityDto.of(university, 0);
    }

    @Transactional
    public UniversityDto updateUniversity(Long id, UniversityRequest request) {
        University university = requireUniversity(id);
        String name = request.name().trim();
        if (universityRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "院校名称已存在");
        }
        String before = university.getName();
        apply(university, request);
        university = universityRepository.save(university);
        auditService.record("UNIVERSITY_UPDATE", "UNIVERSITY", id, before, university.getName());
        return UniversityDto.of(university,
                majorRepository.countByUniversityIdAndStatus(id, "ACTIVE"));
    }

    @Transactional
    public UniversityDto setUniversityStatus(Long id, String status) {
        University university = requireUniversity(id);
        String before = university.getStatus();
        university.setStatus(status);
        university = universityRepository.save(university);
        auditService.record("UNIVERSITY_STATUS", "UNIVERSITY", id, before, status);
        return UniversityDto.of(university,
                majorRepository.countByUniversityIdAndStatus(id, "ACTIVE"));
    }

    @Transactional
    public MajorDto createMajor(Long universityId, MajorRequest request) {
        University university = requireUniversity(universityId);
        String name = request.name().trim();
        String degreeType = request.degreeType().trim();
        if (majorRepository.existsByUniversityIdAndNameAndDegreeType(universityId, name, degreeType)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该院校下已存在同名专业");
        }
        UniversityMajor major = new UniversityMajor();
        major.setUniversityId(universityId);
        apply(major, request);
        major = majorRepository.save(major);
        auditService.record("UNIVERSITY_MAJOR_CREATE", "UNIVERSITY_MAJOR", major.getId(), null,
                Map.of("university", university.getName(), "name", major.getName()));
        return MajorDto.of(major);
    }

    @Transactional
    public MajorDto updateMajor(Long universityId, Long majorId, MajorRequest request) {
        UniversityMajor major = requireMajor(universityId, majorId);
        String name = request.name().trim();
        String degreeType = request.degreeType().trim();
        if (majorRepository.existsByUniversityIdAndNameAndDegreeTypeAndIdNot(universityId, name, degreeType,
                majorId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该院校下已存在同名专业");
        }
        String before = major.getName();
        apply(major, request);
        major = majorRepository.save(major);
        auditService.record("UNIVERSITY_MAJOR_UPDATE", "UNIVERSITY_MAJOR", majorId, before, major.getName());
        return MajorDto.of(major);
    }

    @Transactional
    public void disableMajor(Long universityId, Long majorId) {
        UniversityMajor major = requireMajor(universityId, majorId);
        major.setStatus("DISABLED");
        majorRepository.save(major);
        auditService.record("UNIVERSITY_MAJOR_DISABLE", "UNIVERSITY_MAJOR", majorId, major.getName(), null);
    }

    @Transactional
    public ScoreLineDto createScoreLine(Long universityId, ScoreLineRequest request) {
        requireUniversity(universityId);
        UniversityMajor major = requireMajor(universityId, request.majorId());
        if (scoreLineRepository.existsByMajorIdAndYear(major.getId(), request.year())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该专业当年分数线已存在");
        }
        ScoreLine line = new ScoreLine();
        line.setUniversityId(universityId);
        line.setMajorId(major.getId());
        line.setDegreeType(major.getDegreeType());
        apply(line, request);
        line = scoreLineRepository.save(line);
        auditService.record("SCORE_LINE_CREATE", "SCORE_LINE", line.getId(), null,
                Map.of("major", major.getName(), "year", line.getYear(), "total", line.getTotal()));
        return ScoreLineDto.of(line, major.getName());
    }

    @Transactional
    public ScoreLineDto updateScoreLine(Long universityId, Long lineId, ScoreLineRequest request) {
        ScoreLine line = requireScoreLine(universityId, lineId);
        UniversityMajor major = requireMajor(universityId, request.majorId());
        if (scoreLineRepository.existsByMajorIdAndYearAndIdNot(major.getId(), request.year(), lineId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该专业当年分数线已存在");
        }
        Integer before = line.getTotal();
        line.setMajorId(major.getId());
        line.setDegreeType(major.getDegreeType());
        apply(line, request);
        line = scoreLineRepository.save(line);
        auditService.record("SCORE_LINE_UPDATE", "SCORE_LINE", lineId, before, line.getTotal());
        return ScoreLineDto.of(line, major.getName());
    }

    @Transactional
    public void deleteScoreLine(Long universityId, Long lineId) {
        ScoreLine line = requireScoreLine(universityId, lineId);
        scoreLineRepository.delete(line);
        auditService.record("SCORE_LINE_DELETE", "SCORE_LINE", lineId,
                Map.of("year", line.getYear(), "total", line.getTotal()), null);
    }

    @Transactional
    public AdmissionStatDto createAdmissionStat(Long universityId, AdmissionStatRequest request) {
        requireUniversity(universityId);
        UniversityMajor major = requireMajor(universityId, request.majorId());
        if (admissionStatRepository.existsByMajorIdAndYear(major.getId(), request.year())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该专业当年报录比已存在");
        }
        AdmissionStat stat = new AdmissionStat();
        stat.setUniversityId(universityId);
        stat.setMajorId(major.getId());
        apply(stat, request);
        stat = admissionStatRepository.save(stat);
        auditService.record("ADMISSION_STAT_CREATE", "ADMISSION_STAT", stat.getId(), null,
                Map.of("major", major.getName(), "year", stat.getYear(), "ratio", stat.getRatio()));
        return AdmissionStatDto.of(stat, major.getName());
    }

    @Transactional
    public AdmissionStatDto updateAdmissionStat(Long universityId, Long statId, AdmissionStatRequest request) {
        AdmissionStat stat = requireAdmissionStat(universityId, statId);
        UniversityMajor major = requireMajor(universityId, request.majorId());
        if (admissionStatRepository.existsByMajorIdAndYearAndIdNot(major.getId(), request.year(), statId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该专业当年报录比已存在");
        }
        Object before = Map.of("year", stat.getYear(), "ratio", stat.getRatio());
        stat.setMajorId(major.getId());
        apply(stat, request);
        stat = admissionStatRepository.save(stat);
        auditService.record("ADMISSION_STAT_UPDATE", "ADMISSION_STAT", statId, before,
                Map.of("year", stat.getYear(), "ratio", stat.getRatio()));
        return AdmissionStatDto.of(stat, major.getName());
    }

    @Transactional
    public void deleteAdmissionStat(Long universityId, Long statId) {
        AdmissionStat stat = requireAdmissionStat(universityId, statId);
        admissionStatRepository.delete(stat);
        auditService.record("ADMISSION_STAT_DELETE", "ADMISSION_STAT", statId,
                Map.of("year", stat.getYear(), "ratio", stat.getRatio()), null);
    }

    private void apply(University university, UniversityRequest request) {
        university.setName(request.name().trim());
        university.setProvince(request.province().trim());
        university.setCity(blankToNull(request.city()));
        university.setLevel(blankToNull(request.level()));
        university.setType(blankToNull(request.type()));
        university.setLogoUrl(blankToNull(request.logoUrl()));
        university.setWebsite(blankToNull(request.website()));
        university.setIntroduction(blankToNull(request.introduction()));
        university.setFeatured(Boolean.TRUE.equals(request.featured()));
        university.setSort(request.sort() == null ? 0 : request.sort());
    }

    private void apply(UniversityMajor major, MajorRequest request) {
        major.setName(request.name().trim());
        major.setCode(blankToNull(request.code()));
        major.setDegreeType(request.degreeType().trim());
        major.setDuration(blankToNull(request.duration()));
        major.setTuition(request.tuition());
        major.setExamSubjects(blankToNull(request.examSubjects()));
        major.setIntroduction(blankToNull(request.introduction()));
        major.setSort(request.sort() == null ? 0 : request.sort());
    }

    private void apply(ScoreLine line, ScoreLineRequest request) {
        line.setYear(request.year());
        line.setPolitical(request.political());
        line.setEnglish(request.english());
        line.setMath(request.math());
        line.setMajorScore(request.majorScore());
        line.setTotal(request.total());
    }

    private void apply(AdmissionStat stat, AdmissionStatRequest request) {
        stat.setYear(request.year());
        stat.setApplicants(request.applicants());
        stat.setAdmitted(request.admitted());
        stat.setMinScore(request.minScore());
        stat.setRatio(request.admitted() == 0 ? null
                : BigDecimal.valueOf(request.applicants())
                .divide(BigDecimal.valueOf(request.admitted()), 2, RoundingMode.HALF_UP));
    }

    private University requireUniversity(Long id) {
        return universityRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "院校不存在"));
    }

    private UniversityMajor requireMajor(Long universityId, Long majorId) {
        return majorRepository.findByIdAndUniversityId(majorId, universityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "专业不存在"));
    }

    private ScoreLine requireScoreLine(Long universityId, Long lineId) {
        return scoreLineRepository.findByIdAndUniversityId(lineId, universityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分数线记录不存在"));
    }

    private AdmissionStat requireAdmissionStat(Long universityId, Long statId) {
        return admissionStatRepository.findByIdAndUniversityId(statId, universityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报录比记录不存在"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
