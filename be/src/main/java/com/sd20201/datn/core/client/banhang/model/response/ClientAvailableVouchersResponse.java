package com.sd20201.datn.core.client.banhang.model.response;

import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ClientAvailableVouchersResponse {

    private List<PhieuGiamGiaDTO> availableVouchers;

    private PhieuGiamGiaDTO bestVoucher;

    private BetterVoucherInfo betterVoucher;

    // ================= DTO =================
    @Getter
    @Setter
    public static class PhieuGiamGiaDTO {

        private String id;
        private String ma;
        private String ten;

        // % giảm (chỉ dùng khi type = PERCENTAGE)
        private BigDecimal phanTramGiam;

        // Giá trị giảm thực tế (đã áp trần)
        private BigDecimal giaTriGiamThucTe;

        // Điều kiện áp dụng (tổng tiền >=)
        private BigDecimal dieuKien;

        // Giá trị giảm gốc (%, hoặc số tiền)
        private BigDecimal giamGia;

        // Số lượng còn lại
        private Integer soLuongPhieu;

        private Long ngayBatDau;
        private Long ngayKetThuc;

        // Kiểu giảm
        private TypeVoucher loaiGiam;
    }

    // ================= Better voucher =================
    @Getter
    @Setter
    public static class BetterVoucherInfo {

        private String ma;

        private BigDecimal giaTriGiamThucTe;

        // Cần mua thêm bao nhiêu để đạt điều kiện
        private BigDecimal amountNeeded;
    }
}
