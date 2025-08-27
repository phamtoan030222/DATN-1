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
@Table(name = "gpu")
public class GPU extends PrimaryEntity implements Serializable {

    @Column(length = EntityProperties.LENGTH_NAME)
    private String series;

    @Column(length = EntityProperties.LENGTH_CODE)
    private String brand;

    @Column(length = EntityProperties.LENGTH_NAME)
    private String generation;

    private Integer releaseYear;

    @Column(length = EntityProperties.LENGTH_NAME)
    private String color;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;

}
