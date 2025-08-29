package com.sd20201.datn.core.admin.discount.voucher.service.impl;

import com.sd20201.datn.core.admin.discount.voucher.motel.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.discount.voucher.motel.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.discount.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.core.admin.discount.voucher.service.AdVoucherService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.utils.Helper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdVoucherServiceImpl implements AdVoucherService {
    private final AdVoucherRepository voucherRepository;
//    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(AdVoucherServiceImpl.class);


    @Override
    public ResponseObject<?> getVouchers(AdVoucherRequest request) {
        return ResponseObject.successForward(PageableObject.of(voucherRepository.getVouchers
                (Helper.createPageable(request), request)), "Lấy danh sách voucher thành công");
    }

    @Override
    public ResponseObject<?> getVoucherById(String id) {
        return voucherRepository.getVoucherById(id).map(res -> ResponseObject.
                successForward(res, "Lấy chi tiết voucher thành công")).orElse(ResponseObject.errorForward("Lấy chi tiết thất bại", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatusVoucher(String id) {
        return voucherRepository.findById(id).map(voucher -> {
            voucher.setStatus(voucher.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
            voucherRepository.save(voucher);
            return ResponseObject.successForward(null, "Cập nhật trang thái thành công");
        }).orElse(ResponseObject.errorForward("Cập nhật thất bại! không tìm thấy voucher", HttpStatus.NOT_FOUND));
    }


    @Override
    public ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) {
        if (voucherRepository.findVoucherByCode(request.getCode()).isPresent()) {
            logger.warn("Duplicate voucher code attempted: {}", request.getCode());
            throw new DuplicateKeyException("Mã voucher đã tồn tại: " + request.getCode());
        }

        Voucher voucher = new Voucher();
        mapRequestToVoucher(request, voucher);
        voucher = voucherRepository.save(voucher);


        logger.info("Voucher created successfully: {}", voucher.getCode());
        return ResponseObject.successForward(voucher, "Thêm thành công voucher");
    }

    @Override
    public ResponseObject<?> update(@PathVariable String id, @Valid AdVoucherCreateUpdateRequest request) {
        Optional<Voucher> optionalVoucher = voucherRepository.findById(id);
        if (optionalVoucher.isEmpty()) {
            logger.warn("Voucher not found for update: ID {}", request.getId());
            return ResponseObject.errorForward("Không tìm thấy voucher để cập nhật", HttpStatus.NOT_FOUND);
        }

        Voucher voucher = optionalVoucher.get();

        // Kiểm tra duplicate code nếu code thay đổi
        if (!voucher.getCode().equals(request.getCode()) && voucherRepository.findVoucherByCode(request.getCode()).isPresent()) {
            logger.warn("Duplicate voucher code attempted during update: {}", request.getCode());
            throw new DuplicateKeyException("Mã voucher mới đã tồn tại: " + request.getCode());
        }

        mapRequestToVoucher(request, voucher);
        voucher = voucherRepository.save(voucher);


        logger.info("Voucher updated successfully: {}", voucher.getCode());
        return ResponseObject.successForward(voucher, "Chỉnh sửa thành công voucher");
    }

    @Override
    public void distributeToCustomers(String voucherId, List<String> userIds) {

    }

    private void mapRequestToVoucher(AdVoucherCreateUpdateRequest request, Voucher voucher) {
        voucher.setCode(request.getCode());
        voucher.setName(request.getName());
        voucher.setTargetType(request.getTargetType());
        voucher.setTypeVoucher(request.getTypeVoucher());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setQuantity(request.getQuantity());
        voucher.setRemainingQuantity(request.getRemainingQuantity());
        voucher.setMaxValue(request.getMaxValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setConditions(request.getConditions());

    }

//    public void distributeToCustomers(String voucherId, List<Long> customerIds) {
//        Voucher voucher = voucherRepository.findById(voucherId).orElseThrow();
//        if (voucher.getTargetType() != TargetType.INDIVIDUAL) {
//            throw new IllegalArgumentException("Chỉ áp dụng cho INDIVIDUAL");
//        }
//        for (Long customerId : customerIds) {
//            Customer customer = customerRepository.findById(customerId).orElseThrow();
//            VoucherUser vu = new VoucherUser();
//            vu.setVoucher(voucher);
//            vu.setUser(customer);
//            vu.setAssignedAt(LocalDateTime.now());
//            voucherUserRepository.save(vu);
//            // Giảm remainingQuantity nếu cần
//            voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
//        }
//        voucherRepository.save(voucher);
//    }

//    private void sendVoucherEmail(String toEmail, Voucher voucher) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(toEmail);
//        message.setSubject("Cập nhật phiếu giảm giá cá nhân");
//        message.setText(String.format("Phiếu giảm giá của bạn đã được tạo/cập nhật: Mã %s, Giảm %d%%, Tối đa %s. " + "Hiệu lực từ %s đến %s. Điều kiện: %s", voucher.getCode(), voucher.getDecreaseUnit(), voucher.getMaximumReduction(), voucher.getStartTime(), voucher.getEndTime(), voucher.getConditionOfUse()));
//        mailSender.send(message);
//        logger.info("Email sent to: {}", toEmail);
//    }


}
