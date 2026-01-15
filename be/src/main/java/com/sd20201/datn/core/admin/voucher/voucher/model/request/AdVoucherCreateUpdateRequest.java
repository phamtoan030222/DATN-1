package com.sd20201.datn.core.admin.voucher.voucher.model.request;

import com.sd20201.datn.entity.VoucherDetail;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdVoucherCreateUpdateRequest {


    @NotBlank(message = "Tên voucher không được để trống")
    @Size(max = 255, message = "Tên voucher không được quá 255 ký tự")
    private String name;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private Long startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private Long endDate;

    @NotNull(message = "Loại phiếu giảm giá không được để trống")
    @Enumerated(EnumType.ORDINAL)
    private TypeVoucher typeVoucher;

    @NotNull(message = "Giá trị không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá trị giảm tối đa không được âm")
    private BigDecimal maxValue;

    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity;

    @Min(value = 0, message = "Số lượng còn lại không được âm")
    private Integer remainingQuantity;

    @DecimalMin(value = "0.0", message = "Điều kiện nhận phải ≥ 0")
    private BigDecimal conditions;

    @NotNull(message = "Mục đích không được để trống")
    @Enumerated(EnumType.ORDINAL)
    private TargetType targetType;


    private String note;

    private Set<VoucherDetail> voucherUsers;
}
