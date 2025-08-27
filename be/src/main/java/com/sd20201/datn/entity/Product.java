package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
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
@Table(name = "product")
public class Product extends PrimaryEntity implements Serializable {

    @ManyToOne
    @JoinColumn(name = "id_screen", referencedColumnName = "id")
    private Screen screen;

    @ManyToOne
    @JoinColumn(name = "id_brand", referencedColumnName = "id")
    private Brand brand;

    @ManyToOne
    @JoinColumn(name = "id_battery", referencedColumnName = "id")
    private Battery battery;

    @ManyToOne
    @JoinColumn(name = "id_operatin_system", referencedColumnName = "id")
    private OperatingSystem operatingSystem;
}
