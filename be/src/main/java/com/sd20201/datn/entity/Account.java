package com.sd20201.datn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "account")
@ToString
public class Account extends PrimaryEntity implements Serializable {

    private RoleConstant roleConstant;

    private String username;

    private String password;

    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
    @ToString.Exclude           // 1. Chặn lỗi khi log ra console
    @EqualsAndHashCode.Exclude  // 2. Chặn lỗi khi so sánh đối tượng
    @JsonIgnore
    private Staff staff;

    public Staff getStaff()
    { return staff; }


}
