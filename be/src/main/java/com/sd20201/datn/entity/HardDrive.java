package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "hard_drive")
public class HardDrive extends PrimaryEntity implements Serializable {

    @Column(length = EntityProperties.LENGTH_NAME)
    private String brand;

    @Column(length = EntityProperties.LENGTH_NAME)
    private String type;

    @Column(length = EntityProperties.LENGTH_NAME)
    private String typeConnect;

    private Integer capacity;

    private Integer readSpeed;

    private Integer writeSpeed;

    private Integer cacheMemory;

    private Float physicalSize;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;

}
