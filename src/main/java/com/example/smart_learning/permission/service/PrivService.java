package com.example.smart_learning.permission.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.permission.dto.PrivDto;
import com.example.smart_learning.permission.repository.SysPrivRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Master
public class PrivService {

    private final SysPrivRepository privRepository;

    public PrivService(SysPrivRepository privRepository) {
        this.privRepository = privRepository;
    }

    @Transactional(readOnly = true)
    public List<PrivDto> list() {
        return privRepository.findAllByOrderByModuleAscSortAscIdAsc().stream()
                .map(PrivDto::of)
                .toList();
    }
}
