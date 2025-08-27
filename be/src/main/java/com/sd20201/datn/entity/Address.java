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
@Table(name = "address")
public class Address extends PrimaryEntity implements Serializable {

    @ManyToOne
    @JoinColumn(name = "id_customer", referencedColumnName = "id")
    private Customer customer;

    @Column(length = EntityProperties.LENGTH_CONTENT)
    private String provinceCity;

    @Column(length = EntityProperties.LENGTH_CONTENT)
    private String wardCommune;

    @Column(length = EntityProperties.LENGTH_CONTENT)
    private String addressDetail;

}
