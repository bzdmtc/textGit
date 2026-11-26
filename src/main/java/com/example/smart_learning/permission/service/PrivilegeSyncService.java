package com.example.smart_learning.permission.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.permission.entity.SysPriv;
import com.example.smart_learning.permission.repository.SysPrivRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PrivilegeSyncService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PrivilegeSyncService.class);

    private final SysPrivRepository privRepository;

    public PrivilegeSyncService(SysPrivRepository privRepository) {
        this.privRepository = privRepository;
    }

    @Override
    @Master
    @Transactional
    public void run(ApplicationArguments args) {
        sync();
    }

    @Master
    @Transactional
    public void sync() {
        Set<String> registered = new HashSet<>();
        int created = 0;
        for (PrivilegeRegistry definition : PrivilegeRegistry.all()) {
            String key = definition.module() + "/" + definition.method();
            registered.add(key);
            Optional<SysPriv> existing = privRepository.findByModuleAndMethod(
                    definition.module(), definition.method());
            SysPriv priv;
            if (existing.isPresent()) {
                priv = existing.get();
            } else {
                priv = new SysPriv();
                priv.setModule(definition.module());
                priv.setMethod(definition.method());
                created++;
            }
            priv.setName(definition.displayName());
            priv.setType(definition.type());
            priv.setMenuPath(definition.menuPath());
            priv.setIcon(definition.icon());
            priv.setSort(definition.sort());
            priv.setStatus("ACTIVE");
            privRepository.save(priv);
        }
        List<SysPriv> all = privRepository.findAll();
        int deprecated = 0;
        for (SysPriv priv : all) {
            String key = priv.getModule() + "/" + priv.getMethod();
            if (!registered.contains(key) && !"DEPRECATED".equals(priv.getStatus())) {
                priv.setStatus("DEPRECATED");
                privRepository.save(priv);
                deprecated++;
            }
        }
        log.info("Privilege sync finished: registered={}, created={}, deprecated={}",
                registered.size(), created, deprecated);
    }
}
