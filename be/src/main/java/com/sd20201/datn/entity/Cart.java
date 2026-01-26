package com.sd20201.datn.entity;

import com.sd20201.datn.entity.base.PrimaryEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.w3c.dom.stylesheets.LinkStyle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Cart extends PrimaryEntity {

    @ManyToOne
    @JoinColumn(name = "id_customer")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "id_staff")
    private Staff staff;

    @OneToMany(mappedBy = "cart")
    private List<CartItem> items = new ArrayList<>();

    private BigDecimal totalAmount = BigDecimal.ZERO;

    private LocalDateTime lastUpdatedAt;

    private String sessionId;
}
