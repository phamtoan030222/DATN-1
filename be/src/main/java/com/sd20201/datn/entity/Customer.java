package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "customer")
public class Customer extends PrimaryEntity implements Serializable {

    @OneToOne
    @JoinColumn(name = "id_account", unique = true, nullable = false)
    private Account account;

    private String email;

    private Boolean gender;

    @Column(length = 15)
    private String phone;

    private Long birthday;

    private String avatarUrl;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;

}
