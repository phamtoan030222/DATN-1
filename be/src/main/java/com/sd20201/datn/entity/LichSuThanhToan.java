package com.sd20201.datn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "lich_su_thanh_toan")
public class LichSuThanhToan implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "so_tien")
    private BigDecimal soTien;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    @Column(name = "ma_giao_dich")
    private String maGiaoDich;

    @Column(name = "loai_giao_dich")
    private String loaiGiaoDich;

    @ManyToOne
    @JoinColumn(name = "nhan_vien_id")
    private Staff nhanVien;

    @ManyToOne
    @JoinColumn(name = "hoa_don_id", referencedColumnName = "id")
    private Invoice hoaDon;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "trang_thai_thanh_toan")
    private boolean trangThaiThanhToan;

}
