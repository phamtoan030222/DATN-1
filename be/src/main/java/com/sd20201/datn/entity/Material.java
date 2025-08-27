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
@Table(name = "material")
public class Material extends PrimaryEntity implements Serializable {

    // Chất liệu nắp trên của laptop
    @Column(length = EntityProperties.LENGTH_CODE)
    private String topCaseMaterial;

    // Chất liệu đáy laptop
    @Column(length = EntityProperties.LENGTH_CODE)
    private String bottomCaseMaterial;

    // Chất liệu bàn phím
    @Column(length = EntityProperties.LENGTH_CODE)
    private String keyboardMaterial;

    // Mô tả thêm nếu cần
    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;

}
