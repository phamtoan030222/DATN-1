package com.sd20201.datn.infrastructure.listener;

import com.sd20201.datn.entity.Material;
import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import jakarta.persistence.PrePersist;

import java.security.SecureRandom;
import java.util.UUID;

public class CreatePrimaryEntityListener {

    @PrePersist
    private void onCreate(PrimaryEntity entity) {
        entity.setId(UUID.randomUUID().toString());
        entity.setStatus(EntityStatus.ACTIVE);
        entity.setCode(setCodePrimaryEntity(entity));
    }


    private String setCodePrimaryEntity(PrimaryEntity entity) {
        switch (entity.getClass().getSimpleName()) {
            case "Product" -> {
                return "PD" + generateCode();
            }
            case "ProductDetail" -> {
                return "PDDT" + generateCode();
            }
            case "Brand" -> {
                return "BR" + generateCode();
            }
            case "Battery" -> {
                return "BT" + generateCode();
            }
            case "Screen" -> {
                return "SC" + generateCode();
            }
            case "OperatingSystem" -> {
                return "OS" + generateCode();
            }
            case "CPU" -> {
                return "CPU" + generateCode();
            }
            case "GPU" -> {
                return "GPU" + generateCode();
            }
            case "Material" -> {
                return "MT" + generateCode();
            }
            case "RAM" -> {
                return "RAM" + generateCode();
            }
            case "HardDrive" -> {
                return "HD" + generateCode();
            }
        }

        return entity.getCode();
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
