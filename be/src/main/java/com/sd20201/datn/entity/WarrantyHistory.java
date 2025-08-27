package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "warranty_history")
public class WarrantyHistory extends PrimaryEntity implements Serializable {

    @ManyToOne
    @JoinColumn(name = "id_warranty", referencedColumnName = "id")
    private Warranty warranty;

    private Long warrantyDate;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String descriptionError;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String howToFix;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;
}
