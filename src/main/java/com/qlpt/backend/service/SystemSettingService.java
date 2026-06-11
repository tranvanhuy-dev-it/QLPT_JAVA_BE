package com.qlpt.backend.service;

import com.qlpt.backend.entity.SystemSetting;

public interface SystemSettingService {
    public void init();

    public SystemSetting getSetting();

    public SystemSetting updateSetting(SystemSetting newSetting);
}
