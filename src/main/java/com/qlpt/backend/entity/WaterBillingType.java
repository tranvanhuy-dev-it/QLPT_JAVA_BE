package com.qlpt.backend.entity;

public enum WaterBillingType {
    BY_INDEX,           // Tính theo chỉ số đồng hồ nước tiêu thụ
    FIXED_PER_PERSON,   // Tính tiền nước cố định theo đầu người ở
    FIXED_PER_ROOM      // Tính tiền nước cố định theo phòng trọ
}
