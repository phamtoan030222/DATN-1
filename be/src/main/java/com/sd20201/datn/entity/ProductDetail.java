package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import com.sd20201.datn.infrastructure.constant.EntityProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "product_detail")
public class ProductDetail extends PrimaryEntity implements Serializable, Cloneable {

    @ManyToOne
    @JoinColumn(name="id_product", referencedColumnName = "id")
    private Product product;

    @OneToOne
    @JoinColumn(name = "id_imei")
    private IMEI iMEI;

    @ManyToOne
    @JoinColumn(name = "id_color", nullable = false)
    private Color color;

    @ManyToOne
    @JoinColumn(name = "id_ram", referencedColumnName = "id")
    private RAM ram;

    @ManyToOne
    @JoinColumn(name = "id_hard_drive", referencedColumnName = "id")
    private HardDrive hardDrive;

    @ManyToOne
    @JoinColumn(name = "id_material", referencedColumnName = "id")
    private Material material;

    @ManyToOne
    @JoinColumn(name = "id_gpu", referencedColumnName = "id")
    private GPU gpu;

    @ManyToOne
    @JoinColumn(name = "id_cpu", referencedColumnName = "id")
    private CPU cpu;

    private BigDecimal price;

    @Column(length = EntityProperties.LENGTH_DESCRIPTION)
    private String description;

    @Override
    public ProductDetail clone() throws CloneNotSupportedException {
        return (ProductDetail) super.clone();
    }
}
