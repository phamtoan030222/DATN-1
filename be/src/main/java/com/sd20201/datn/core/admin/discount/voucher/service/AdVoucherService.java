package com.sd20201.datn.core.admin.discount.voucher.service;

import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AdVoucherService {

    /* Danh sách + chi tiết voucher */
    ResponseObject<?> getVouchers(AdVoucherRequest request);
    ResponseObject<?> getVoucherById(String id);

    /* Đổi trạng thái ACTIVE/INACTIVE */
    ResponseObject<?> changeStatusVoucher(String id);

    /* Tạo / cập nhật / xóa */
    ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException;
    ResponseObject<?> update(String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException;
    ResponseObject<?> deleteById(String id);
    ResponseObject<?> deleteAllByIds(List<String> ids);

    /* Bản đồ quan hệ Voucher <-> Customer (không phân trang) */
    Map<Voucher, List<Customer>> getCustomersByVoucher(boolean onlyActive);
    Map<Customer, List<Voucher>> getVouchersByCustomer(boolean onlyActive);

    /* === MỚI: Lấy khách hàng theo voucher có PHÂN TRANG ===
       - onlyUsed = true: khách đã sử dụng (usageStatus = true) theo voucherCode
       - onlyUsed = false: khách được gán theo voucherId
       Trả về Page<...> bọc trong ResponseObject (dùng PageableObject.of(...) ở service impl)
    */
    ResponseObject<?> getCustomersOfVoucher(String voucherId, boolean onlyUsed, Pageable pageable);
}
