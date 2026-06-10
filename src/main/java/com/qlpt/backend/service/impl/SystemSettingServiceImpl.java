package com.qlpt.backend.service.impl;

import com.qlpt.backend.service.SystemSettingService;

import com.qlpt.backend.entity.SystemSetting;
import com.qlpt.backend.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingServiceImpl(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @PostConstruct
    @Override
    public void init() {
        // Automatically check and seed database on application startup
        getSetting();
    }

    @Transactional
    @Override
    public SystemSetting getSetting() {
        return systemSettingRepository.findById(1L).orElseGet(() -> {
            SystemSetting defaultSetting = SystemSetting.builder()
                    .id(1L)
                    .bankName("VietinBank")
                    .bankAccount("102882915218")
                    .accountName("TRAN VAN HUY")
                    .phone("0365943254")
                    .email("admin@gmail.com")
                    .fullName("Trần Văn Huy")
                    .build();
            return systemSettingRepository.save(defaultSetting);
        });
    }

    @Transactional
    @Override
    public SystemSetting updateSetting(SystemSetting newSetting) {
        SystemSetting existing = getSetting();
        existing.setBankName(newSetting.getBankName());
        existing.setBankAccount(newSetting.getBankAccount());
        existing.setAccountName(newSetting.getAccountName());
        existing.setPhone(newSetting.getPhone());
        existing.setEmail(newSetting.getEmail());
        existing.setFullName(newSetting.getFullName());
        return systemSettingRepository.save(existing);
    }
}
