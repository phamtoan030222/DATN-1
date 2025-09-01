package com.sd20201.datn.core.admin.discount.voucher.service.impl;

import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.discount.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.discount.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.core.admin.discount.voucher.service.AdVoucherService;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.entity.VoucherDetail;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.repository.VoucherDetailRepository;
import com.sd20201.datn.utils.DateTimeUtil;
import com.sd20201.datn.utils.Helper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdVoucherServiceImpl implements AdVoucherService {
    private final AdVoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;
    private final VoucherDetailRepository voucherDetailRepository;
    private static final Logger logger = LoggerFactory.getLogger(AdVoucherServiceImpl.class);
    private final JavaMailSenderImpl mailSender;

    @Override
    public ResponseObject<?> getVouchers(AdVoucherRequest request) {
        return ResponseObject.successForward(
                PageableObject.of(voucherRepository.getVouchers(Helper.createPageable(request), request)),
                "Lấy danh sách voucher thành công"
        );
    }

    @Override
    public ResponseObject<?> getVoucherById(String id) {
        return voucherRepository.getVoucherById(id)
                .map(res -> ResponseObject.successForward(res, "Lấy chi tiết voucher thành công"))
                .orElse(ResponseObject.errorForward("Lấy chi tiết thất bại", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatusVoucher(String id) {
        return voucherRepository.findById(id).map(voucher -> {
            voucher.setStatus(voucher.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
            voucherRepository.save(voucher);
            return ResponseObject.successForward(null, "Cập nhật trạng thái thành công");
        }).orElse(ResponseObject.errorForward("Cập nhật thất bại! không tìm thấy voucher", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        logger.debug("Received create request: {}", request);

        if (voucherRepository.findVoucherByName(request.getName()).isPresent()) {
            throw new DuplicateKeyException("Tên voucher đã tồn tại: " + request.getName());
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("StartDate hoặc EndDate không được để trống!!");
        }

        if (request.getStartDate() >= request.getEndDate()) {
            throw new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc!!");
        }

        if (request.getEndDate() < DateTimeUtil.getCurrentTimeMillisecondsStamp()) {
            throw new BadRequestException("Ngày kết thúc không được nhỏ hơn hiện tại!!");
        }

        Voucher voucher = new Voucher();
        voucher.setCode(Helper.generateCodeVoucher());
        mapRequestToVoucher(request, voucher);

        List<String> failedEmails = new ArrayList<>();

        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            if (request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0 cho ALL_CUSTOMERS");
            }
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());
        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherUsers() == null || request.getVoucherUsers().isEmpty()) {
                throw new IllegalArgumentException("Danh sách khách hàng cá nhân không được để trống cho Khách Hàng Riêng");
            }

            List<VoucherDetail> voucherUsers = request.getVoucherUsers().stream().toList();
            List<Customer> customers = new ArrayList<>();
            List<String> missingIds = new ArrayList<>();

            for (VoucherDetail vd : voucherUsers) {
                Optional<Customer> optionalCustomer = customerRepository.findById(vd.getCustomer().getId());
                if (optionalCustomer.isPresent()) {
                    customers.add(optionalCustomer.get());
                } else {
                    missingIds.add(vd.getCustomer().getId());
                }
            }

            if (!missingIds.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng với IDs: " + String.join(", ", missingIds));
            }

            int customerCount = voucherUsers.size();
            voucher.setQuantity(customerCount);
            voucher.setRemainingQuantity(customerCount);

            for (int i = 0; i < customerCount; i++) {
                VoucherDetail vd = voucherUsers.get(i);
                Customer customer = customers.get(i);
                vd.setCustomer(customer);
                vd.setVoucher(voucher);
                voucherDetailRepository.save(vd);

                try {
                    sendVoucherEmail(customer.getEmail(), voucher);
                } catch (MailException e) {
                    logger.error("❌ Failed to send email to {}: {}", customer.getEmail(), e.getMessage());
                    failedEmails.add(customer.getEmail());
                }
            }
        } else {
            throw new IllegalArgumentException("TargetType không hợp lệ!");
        }

        voucher = voucherRepository.save(voucher);

        String message = "Thêm thành công voucher";
        if (!failedEmails.isEmpty()) {
            message += ". Tuy nhiên, gửi email thất bại cho các khách hàng: " + String.join(", ", failedEmails);
        }

        return ResponseObject.successForward(voucher, message);
    }

    @Override
    public ResponseObject<?> update(String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        Optional<Voucher> optionalVoucher = voucherRepository.findById(id);
        if (optionalVoucher.isEmpty()) {
            return ResponseObject.errorForward("Không tìm thấy voucher để cập nhật", HttpStatus.NOT_FOUND);
        }

        Voucher voucher = optionalVoucher.get();

        if (!voucher.getName().equals(request.getName()) && voucherRepository.findVoucherByName(request.getName()).isPresent()) {
            throw new DuplicateKeyException("Tên voucher mới đã tồn tại: " + request.getName());
        }

        if (request.getStartDate() >= request.getEndDate()) {
            throw new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc!!");
        }

        if (request.getEndDate() < DateTimeUtil.getCurrentTimeMillisecondsStamp()) {
            throw new BadRequestException("Ngày kết thúc không được nhỏ hơn hiện tại!!");
        }

        List<String> failedEmails = new ArrayList<>();

        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            if (request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0 cho ALL_CUSTOMERS");
            }
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());
            if (voucher.getTargetType() == TargetType.INDIVIDUAL) {
                voucherDetailRepository.deleteByVoucher(voucher);
            }
        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherUsers() == null || request.getVoucherUsers().isEmpty()) {
                throw new IllegalArgumentException("Danh sách khách hàng cá nhân không được để trống cho Khách Hàng Riêng");
            }
            voucherDetailRepository.deleteByVoucher(voucher);

            List<VoucherDetail> voucherUsers = request.getVoucherUsers().stream().toList();
            List<Customer> customers = new ArrayList<>();
            List<String> missingIds = new ArrayList<>();

            for (VoucherDetail vd : voucherUsers) {
                Optional<Customer> optionalCustomer = customerRepository.findById(vd.getCustomer().getId());
                if (optionalCustomer.isPresent()) {
                    customers.add(optionalCustomer.get());
                } else {
                    missingIds.add(vd.getCustomer().getId());
                }
            }

            if (!missingIds.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng với IDs: " + String.join(", ", missingIds));
            }

            int customerCount = voucherUsers.size();
            voucher.setQuantity(customerCount);
            voucher.setRemainingQuantity(customerCount);

            for (int i = 0; i < customerCount; i++) {
                VoucherDetail vd = voucherUsers.get(i);
                Customer customer = customers.get(i);
                vd.setCustomer(customer);
                vd.setVoucher(voucher);
                voucherDetailRepository.save(vd);

                try {
                    sendVoucherEmail(customer.getEmail(), voucher);
                } catch (MailException e) {
                    logger.error("❌ Failed to send email to {}: {}", customer.getEmail(), e.getMessage());
                    failedEmails.add(customer.getEmail());
                }
            }
        } else {
            throw new IllegalArgumentException("TargetType không hợp lệ!");
        }

        mapRequestToVoucher(request, voucher);
        voucher = voucherRepository.save(voucher);

        String message = "Chỉnh sửa thành công voucher";
        if (!failedEmails.isEmpty()) {
            message += ". Tuy nhiên, gửi email thất bại cho các khách hàng: " + String.join(", ", failedEmails);
        }

        return ResponseObject.successForward(voucher, message);
    }

    @Override
    public ResponseObject<?> deleteById(String id) {
        voucherRepository.deleteById(id);
        return ResponseObject.successForward(null, "Xóa thành công");
    }

    @Override
    public ResponseObject<?> deleteAllByIds(List<String> ids) {
        voucherRepository.deleteAllById(ids);
        return ResponseObject.successForward(null, "Xóa thành công");
    }

    private void mapRequestToVoucher(AdVoucherCreateUpdateRequest request, Voucher voucher) {
        voucher.setName(request.getName());
        voucher.setTargetType(request.getTargetType());
        voucher.setTypeVoucher(request.getTypeVoucher());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxValue(request.getMaxValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setConditions(request.getConditions());
        voucher.setNote(request.getNote());
        if (request.getStartDate() <= DateTimeUtil.getCurrentTimeMillisecondsStamp()
            && DateTimeUtil.getCurrentTimeMillisecondsStamp() <= request.getEndDate()) {
            voucher.setStatus(EntityStatus.ACTIVE);
        } else {
            voucher.setStatus(EntityStatus.INACTIVE);
        }
    }

    private void sendVoucherEmail(String toEmail, Voucher voucher) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getStartDate()), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getEndDate()), ZoneId.systemDefault());

        String gt = voucher.getTypeVoucher() == TypeVoucher.PERCENTAGE
                ? voucher.getDiscountValue() + "%"
                : voucher.getDiscountValue() + " VND";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("🎁 Thông báo phiếu giảm giá");
        message.setText(String.format(mess(),
                voucher.getCode(),
                gt,
                DateTimeUtil.formatMoney(voucher.getMaxValue()) + " VND",
                start.format(formatter),
                end.format(formatter),
                DateTimeUtil.formatMoney(voucher.getConditions()) + " VND"));
        mailSender.send(message);
        logger.info("✅ Email sent to: {}", toEmail);
    }

    private String mess() {
        return """
                🎁 Quà tặng đặc biệt dành riêng cho Quý khách từ My Laptop

                Kính gửi Quý khách hàng thân thiết,
                Trước tiên, My Laptop xin gửi lời cảm ơn chân thành đến Quý khách vì đã luôn tin tưởng và đồng hành cùng chúng tôi.

                ✨ Mã giảm giá đặc biệt: %s
                💰 Giá trị ưu đãi: Giảm %s (tối đa %s)
                📅 Thời gian áp dụng: Từ %s đến %s
                📌 Điều kiện áp dụng: %s

                👉 Hãy nhanh tay sử dụng mã giảm giá để tận hưởng những sản phẩm/dịch vụ chất lượng nhất từ My Laptop.
                Trân trọng,
                My Laptop
                """;
    }
}