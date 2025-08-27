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
@Table(name = "ram")
public class RAM extends PrimaryEntity implements Serializable {

    @Column(length = EntityProperties.LENGTH_CONTENT)
    private String brand;

    @Column(length = EntityProperties.LENGTH_CODE)
    private String type;

    private Integer capacity;

    private Integer busSpeed;

    private Integer slotConFig;

    private Integer maxSupported;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;

}
