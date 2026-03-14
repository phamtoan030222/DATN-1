package com.sd20201.datn.entity;

import jakarta.persistence.*;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ward_shipping_mapping")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardShippingMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "new_province_name")
    private String newProvinceName;  // "Thành phố Hà Nội"

    @Column(name = "new_ward_name")
    private String newWardName;      // "Phường Ô Chợ Dừa"

    @Column(name = "ghtk_province")
    private String ghtkProvince;     // "Hà Nội"

    @Column(name = "ghtk_district")
    private String ghtkDistrict;     // "Quận Đống Đa"
}
