package com.sd20201.datn.core.admin.banhang.service.impl;

import com.sd20201.datn.core.admin.banhang.model.response.AvailableVouchersResponse;
import com.sd20201.datn.core.admin.voucher.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    private final AdVoucherRepository voucherRepository;

    public AvailableVouchersResponse getAvailableVouchers(
            String idHD,
            String idKH,
            BigDecimal tongTien
    ) {

        if (tongTien == null) {
            tongTien = BigDecimal.ZERO;
        }

        List<Voucher> vouchers = voucherRepository.findAvailableVouchers(idKH);

        AvailableVouchersResponse response = new AvailableVouchersResponse();

        // ================= LỌC VOUCHER HỢP LỆ =================
        BigDecimal finalTongTien = tongTien;
        BigDecimal finalTongTien1 = tongTien;
        List<AvailableVouchersResponse.PhieuGiamGiaDTO> availableVouchers =
                vouchers.stream()
                        .filter(v -> isVoucherValid(v, finalTongTien))
                        .map(v -> toDTO(v, finalTongTien1))
                        .collect(Collectors.toList());

        response.setAvailableVouchers(availableVouchers);

        // ================= TÌM VOUCHER TỐT NHẤT =================
        AvailableVouchersResponse.PhieuGiamGiaDTO bestVoucher =
                availableVouchers.stream()
                        .max(Comparator.comparing(
                                v -> v.getGiaTriGiamThucTe() != null
                                        ? v.getGiaTriGiamThucTe()
                                        : BigDecimal.ZERO))
                        .orElse(null);

        response.setBestVoucher(bestVoucher);

        // ================= TÌM VOUCHER TỐT HƠN =================
        AvailableVouchersResponse.BetterVoucherInfo betterVoucher =
                findBetterVoucher(vouchers, tongTien, bestVoucher);

        response.setBetterVoucher(betterVoucher);

        return response;
    }

    // =========================================================
    // ==================== HELPER METHODS =====================
    // =========================================================

    private boolean isVoucherValid(Voucher v, BigDecimal tongTien) {
        if (v == null) return false;
        if (v.getRemainingQuantity() == null || v.getRemainingQuantity() <= 0) return false;
        if (v.getConditions() != null && v.getConditions().compareTo(tongTien) > 0) return false;
        return true;
    }

    private AvailableVouchersResponse.PhieuGiamGiaDTO toDTO(
            Voucher v,
            BigDecimal tongTien
    ) {
        AvailableVouchersResponse.PhieuGiamGiaDTO dto =
                new AvailableVouchersResponse.PhieuGiamGiaDTO();

        dto.setId(v.getId());
        dto.setMa(v.getCode());
        dto.setTen(v.getNote());

        dto.setPhanTramGiam(
                v.getTypeVoucher() == TypeVoucher.PERCENTAGE
                        ? v.getDiscountValue()
                        : null
        );

        dto.setGiaTriGiamThucTe(calculateDiscount(v, tongTien));
        dto.setDieuKien(v.getConditions());
        dto.setGiamGia(v.getDiscountValue());
        dto.setSoLuongPhieu(v.getRemainingQuantity());

        dto.setNgayBatDau(v.getStartDate());
        dto.setNgayKetThuc(v.getEndDate());
        dto.setLoaiGiam(v.getTypeVoucher());

        return dto;
    }

    private BigDecimal calculateDiscount(Voucher v, BigDecimal tongTien) {

        if (v == null || tongTien == null) {
            return BigDecimal.ZERO;
        }

        if (v.getTypeVoucher() == TypeVoucher.PERCENTAGE) {
            BigDecimal percent = v.getDiscountValue();
            if (percent == null) return BigDecimal.ZERO;

            BigDecimal discount =
                    tongTien.multiply(percent)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (v.getMaxValue() != null && discount.compareTo(v.getMaxValue()) > 0) {
                return v.getMaxValue();
            }
            return discount;
        }

        // FIXED_AMOUNT
        return v.getDiscountValue() != null
                ? v.getDiscountValue()
                : BigDecimal.ZERO;
    }

    private AvailableVouchersResponse.BetterVoucherInfo findBetterVoucher(
            List<Voucher> vouchers,
            BigDecimal tongTien,
            AvailableVouchersResponse.PhieuGiamGiaDTO bestVoucher
    ) {

        BigDecimal bestValue =
                bestVoucher != null && bestVoucher.getGiaTriGiamThucTe() != null
                        ? bestVoucher.getGiaTriGiamThucTe()
                        : BigDecimal.ZERO;

        for (Voucher v : vouchers) {
            if (v.getConditions() != null && v.getConditions().compareTo(tongTien) > 0) {

                BigDecimal discount = calculateDiscount(v, tongTien);

                if (discount.compareTo(bestValue) > 0) {
                    AvailableVouchersResponse.BetterVoucherInfo info =
                            new AvailableVouchersResponse.BetterVoucherInfo();

                    info.setMa(v.getCode());
                    info.setGiaTriGiamThucTe(discount);
                    info.setAmountNeeded(v.getConditions().subtract(tongTien));

                    return info;
                }
            }
        }
        return null;
    }
}
