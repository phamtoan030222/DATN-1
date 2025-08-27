package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "staff")
@ToString
public class Staff extends PrimaryEntity implements Serializable {

    @OneToOne
    @JoinColumn(name = "id_account", referencedColumnName = "id", unique = true, nullable = false)
    private Account account;

    private String citizenIdentifyCard;

    private Long birthday;

    private String hometown;

    private Boolean gender;

    private String email;

    private String phone;

    private String avatarUrl;
}
