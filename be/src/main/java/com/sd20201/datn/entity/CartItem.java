package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart_item")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CartItem extends PrimaryEntity {

    @ManyToOne
    @JoinColumn(name = "id_cart")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "id_product_detail")
    private ProductDetail productDetail;

    @OneToMany
    @JoinColumn(name = "id_cart_item")
    private List<IMEI> imeis = new ArrayList<>();

    private BigDecimal price; //Gia tai thoi diem them vao gio

    private BigDecimal totalAmount;

    public void calculateTotal() {
        this.totalAmount = this.price.multiply(BigDecimal.valueOf(this.imeis.size()));
    }
}
