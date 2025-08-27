package com.sd20201.datn.infrastructure.listener;

import com.sd20201.datn.entity.base.AuditEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.Calendar;

public class AuditEntityListener {

    @PrePersist
    private void onCreate(AuditEntity entity) {
//        entity.setCreatedBy();
        entity.setCreatedDate(getCurrentTime());
//        entity.setLastModifiedBy();
        entity.setLastModifiedDate(getCurrentTime());
    }

    @PreUpdate
    private void onUpdate(AuditEntity entity) {
//        entity.setLastModifiedBy();
        entity.setLastModifiedDate(getCurrentTime());
    }

    private long getCurrentTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

}
