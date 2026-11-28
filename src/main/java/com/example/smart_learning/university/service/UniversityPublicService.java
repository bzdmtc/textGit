package com.example.smart_learning.university.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.university.dto.AdmissionStatDto;
import com.example.smart_learning.university.dto.MajorDto;
import com.example.smart_learning.university.dto.ScoreLineDto;
import com.example.smart_learning.university.dto.UniversityDetailDto;
import com.example.smart_learning.university.dto.UniversityDto;
import com.example.smart_learning.university.dto.UniversityOptionsDto;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 院校库读取固定走主库：后台调整院校/分数线后前台立即可见，
 * 避免开发环境从库手动同步导致的长时间延迟；后续可在此基础上加 Cache-Aside。
 */
@Service
@Master
public class UniversityPublicService {

    private final UniversityRepository universityRepository;
    private final UniversityMajorRepository majorRepository;
    private final ScoreLineRepository scoreLineRepository;
    private final AdmissionStatRepository admissionStatRepository;

    public UniversityPublicService(UniversityRepository universityRepository,
                                   UniversityMajorRepository majorRepository,
                                   ScoreLineRepository scoreLineRepository,
                                   AdmissionStatRepository admissionStatRepository) {
        this.universityRepository = universityRepository;
        this.majorRepository = majorRepository;
        this.scoreLineRepository = scoreLineRepository;
        this.admissionStatRepository = admissionStatRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<UniversityDto> page(String keyword, String province, String level, String type,
                                          int page, int size) {
        Page<University> result = universityRepository.search(blankToNull(keyword), blankToNull(province),
                blankToNull(level), blankToNull(type), "ACTIVE",
                PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 50)));
        Map<Long, Long> counts = majorCounts(result.getContent());
        List<UniversityDto> items = result.getContent().stream()
                .map(university -> UniversityDto.of(university,
                        counts.getOrDefault(university.getId(), 0L)))
                .toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public List<UniversityDto> featured(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 8);
        List<University> universities = universityRepository
                .findTop8ByStatusAndFeaturedTrueOrderBySortAscIdAsc("ACTIVE");
        Map<Long, Long> counts = majorCounts(universities);
        return universities.stream().limit(safeLimit)
                .map(university -> UniversityDto.of(university,
                        counts.getOrDefault(university.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public UniversityOptionsDto options() {
        return new UniversityOptionsDto(universityRepository.distinctProvinces(),
                universityRepository.distinctLevels(), universityRepository.distinctTypes());
    }

    @Transactional(readOnly = true)
    public UniversityDetailDto detail(Long id) {
        University university = universityRepository.findById(id)
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "院校不存在"));
        List<UniversityMajor> majors = majorRepository
                .findByUniversityIdAndStatusOrderBySortAscIdAsc(id, "ACTIVE");
        Map<Long, String> majorNames = majors.stream()
                .collect(Collectors.toMap(UniversityMajor::getId, UniversityMajor::getName));
        List<ScoreLine> lines = scoreLineRepository.findByUniversityIdOrderByYearDescMajorIdAsc(id);
        List<AdmissionStat> stats = admissionStatRepository.findByUniversityIdOrderByYearDescMajorIdAsc(id);

        return new UniversityDetailDto(
                UniversityDto.of(university, majors.size()),
                majors.stream().map(MajorDto::of).toList(),
                lines.stream()
                        .map(line -> ScoreLineDto.of(line, majorNames.get(line.getMajorId())))
                        .filter(line -> line.majorName() != null)
                        .toList(),
                stats.stream()
                        .map(stat -> AdmissionStatDto.of(stat, majorNames.get(stat.getMajorId())))
                        .filter(stat -> stat.majorName() != null)
                        .toList());
    }

    private Map<Long, Long> majorCounts(List<University> universities) {
        if (universities.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = universities.stream().map(University::getId).toList();
        return majorRepository.countActiveByUniversityIds(ids).stream()
                .collect(Collectors.toMap(UniversityMajorRepository.MajorCount::getUniversityId,
                        UniversityMajorRepository.MajorCount::getTotal,
                        (left, right) -> left));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
