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

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "warranty")
public class Warranty extends PrimaryEntity implements Serializable {

    @OneToOne
    @JoinColumn(name = "id_imei_sold", referencedColumnName = "id")
    private IMEISold iMEISold;

    private Long startTime;

    private Long endTime;

}
