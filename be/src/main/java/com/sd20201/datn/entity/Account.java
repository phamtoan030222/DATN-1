package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import jakarta.persistence.Entity;
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
@Table(name = "account")
@ToString
public class Account extends PrimaryEntity implements Serializable {

    private RoleConstant roleConstant;

    private String username;

    private String password;


}
