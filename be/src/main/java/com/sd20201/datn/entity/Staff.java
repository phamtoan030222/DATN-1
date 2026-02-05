package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "staff")
@ToString
public class Staff extends PrimaryEntity implements Serializable {

    @Column(name = "name") // Khớp cột 'name' trong DB
    private String name;   // Tên biến trong Java là 'name'

    @Column(name = "code")
    private String code;

    @OneToOne
    @JoinColumn(name = "id_account", referencedColumnName = "id", unique = true, nullable = false)
    @ToString.Exclude           // Chặn lỗi log
    @EqualsAndHashCode.Exclude
    private Account account;

    private String citizenIdentifyCard;

    private Long birthday;

    private String hometown;

    private Boolean gender;

    private String email;

    private String phone;

    private String avatarUrl;

    private String provinceCode;
    private String districtCode;
    private String communeCode;

}
