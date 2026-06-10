package com.qlpt.backend.service;

import com.qlpt.backend.entity.SystemSetting;
import com.qlpt.backend.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface SystemSettingService {
    public void init();
    public SystemSetting getSetting();
    public SystemSetting updateSetting(SystemSetting newSetting);
}
