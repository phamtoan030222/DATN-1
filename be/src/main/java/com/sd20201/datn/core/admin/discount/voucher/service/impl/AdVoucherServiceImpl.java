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
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.repository.VoucherDetailRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
            return ResponseObject.successForward(null, "Cập nhật trạng thái thành công");
        }).orElse(ResponseObject.errorForward("Cập nhật thất bại! không tìm thấy voucher", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        if (voucherRepository.findVoucherByName(request.getName()).isPresent()) {
            logger.warn("Duplicate voucher name attempted: {}", request.getName());
            throw new DuplicateKeyException("Tên voucher đã tồn tại: " + request.getName());
        }

        if (request.getStartDate() >= request.getEndDate()) {
            logger.warn("Ngày bắt đầu khôgn được lớn hơn ngày kết thúc!!");
            throw new BadRequestException("Ngày bắt đầu khôgn được lớn hơn ngày kết thúc!!");
        }

        Voucher voucher = new Voucher();
        voucher.setCode(Helper.generateCodeVoucher());

        // Điều chỉnh số lượng dựa trên targetType trước khi map
        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            List<Customer> allCustomers = customerRepository.findAll();
            voucher.setQuantity(allCustomers.size());
            voucher.setRemainingQuantity(allCustomers.size());
        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherDetail() == null || request.getVoucherDetail().isEmpty()) {
                throw new IllegalArgumentException("Danh sách khách hàng cá nhân không được để trống cho INDIVIDUAL");
            }
            voucher.setQuantity(request.getVoucherDetail().size());
            voucher.setRemainingQuantity(request.getVoucherDetail().size());
        } else if (request.getTargetType() == TargetType.LIMITED_BY_CONDITION) {
            if (request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0 cho LIMITED_BY_CONDITION");
            }
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());
        }

        mapRequestToVoucher(request, voucher);
        voucher = voucherRepository.save(voucher);

        // Phân phối voucher dựa trên targetType
        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            List<Customer> allCustomers = customerRepository.findAll();
            for (Customer customer : allCustomers) {
                VoucherDetail vd = new VoucherDetail();
                vd.setVoucher(voucher);
                vd.setCustomer(customer);
                voucherDetailRepository.save(vd);
                sendVoucherEmail(customer.getEmail(), voucher);
            }
        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            for (VoucherDetail vd : request.getVoucherDetail()) {
                // Load customer đầy đủ từ repository để lấy email và các thuộc tính khác
                Customer customer = customerRepository.findById(vd.getCustomer().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với ID: " + vd.getCustomer().getId()));
                vd.setCustomer(customer);
                vd.setVoucher(voucher);
                voucherDetailRepository.save(vd);
                sendVoucherEmail(customer.getEmail(), voucher);
            }
        } else if (request.getTargetType() == TargetType.LIMITED_BY_CONDITION) {
            // Không phân phối ngay, chờ claimVoucher
        }

        logger.info("Voucher created successfully: {}", voucher.getCode());
        return ResponseObject.successForward(voucher, "Thêm thành công voucher");
    }

    @Override
    public ResponseObject<?> update(String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {

        Optional<Voucher> optionalVoucher = voucherRepository.findById(id);
        if (optionalVoucher.isEmpty()) {
            logger.warn("Voucher not found for update: ID {}", id);
            return ResponseObject.errorForward("Không tìm thấy voucher để cập nhật", HttpStatus.NOT_FOUND);
        }

        Voucher voucher = optionalVoucher.get();

        // Kiểm tra duplicate code nếu code thay đổi
        if (!voucher.getCode().equals(request.getName()) && voucherRepository.findVoucherByCode(request.getName()).isPresent()) {
            logger.warn("Duplicate voucher code attempted during update: {}", request.getName());
            throw new DuplicateKeyException("Mã voucher mới đã tồn tại: " + request.getName());
        }

        if (request.getStartDate() >= request.getEndDate()) {
            logger.warn("Ngày bắt đầu khôgn được lớn hơn ngày kết thúc!!");
            throw new BadRequestException("Ngày bắt đầu khôgn được lớn hơn ngày kết thúc!!");
        }

        // Kiểm tra duplicate name nếu name thay đổi
        if (!voucher.getName().equals(request.getName()) && voucherRepository.findVoucherByName(request.getName()).isPresent()) {
            logger.warn("Duplicate voucher name attempted during update: {}", request.getName());
            throw new DuplicateKeyException("Tên voucher mới đã tồn tại: " + request.getName());
        }

        mapRequestToVoucher(request, voucher);
        voucher = voucherRepository.save(voucher);

        logger.info("Voucher updated successfully: {}", voucher.getCode());
        return ResponseObject.successForward(voucher, "Chỉnh sửa thành công voucher");
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

    @Override
    public ResponseObject<?> claimVoucher(String voucherId, String customerId, BigDecimal currentOrderValue) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher với ID: " + voucherId));

        if (voucher.getTargetType() != TargetType.LIMITED_BY_CONDITION) {
            throw new IllegalArgumentException("Hàm này chỉ áp dụng cho loại LIMITED_BY_CONDITION");
        }

        if (voucher.getRemainingQuantity() <= 0) {
            return ResponseObject.errorForward("Voucher đã hết số lượng", HttpStatus.BAD_REQUEST);
        }

        if (currentOrderValue == null || currentOrderValue.compareTo(voucher.getConditions()) < 0) {
            return ResponseObject.errorForward("Không đáp ứng điều kiện để nhận voucher", HttpStatus.BAD_REQUEST);
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với ID: " + customerId));

        if (voucherDetailRepository.existsByVoucherAndCustomer(voucher, customer)) {
            return ResponseObject.errorForward("Khách hàng đã nhận voucher này", HttpStatus.BAD_REQUEST);
        }

        VoucherDetail vd = new VoucherDetail();
        vd.setVoucher(voucher);
        vd.setCustomer(customer);
        voucherDetailRepository.save(vd);

        voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
        voucherRepository.save(voucher);

        sendVoucherEmail(customer.getEmail(), voucher);

        logger.info("Voucher {} được nhận bởi khách hàng {}", voucher.getCode(), customerId);
        return ResponseObject.successForward(null, "Nhận voucher thành công");
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
        voucher.setStatus(EntityStatus.ACTIVE);
    }

    private void sendVoucherEmail(String toEmail, Voucher voucher) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getStartDate()), ZoneId.systemDefault());
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getEndDate()), ZoneId.systemDefault());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Thông báo phiếu giảm giá");
            message.setText(String.format(mess(),
                    voucher.getCode(), voucher.getDiscountValue(), voucher.getMaxValue(), start.format(formatter), end.format(formatter), voucher.getConditions()));
            mailSender.send(message);
            logger.info("Email sent to: {}", toEmail);
        } catch (MailException e) {
            logger.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String mess() {
        return "\uD83C\uDF81 Quà tặng đặc biệt dành riêng cho Quý khách từ My Laptop\n" +
               "Kính gửi Quý khách hàng thân thiết,\n" +
               "Trước tiên, My Laptop xin gửi lời cảm ơn chân thành đến Quý khách vì đã luôn tin tưởng và đồng hành cùng chúng tôi trong suốt thời gian qua.\n" +
               "Nhằm tri ân sự ủng hộ đặc biệt của Quý khách, chúng tôi xin trân trọng gửi đến Quý khách chương trình ưu đãi dành riêng cho khách hàng VIP:\n" +
               "✨ Mã giảm giá đặc biệt: %s\n" +
               "\uD83D\uDCB0 Giá trị ưu đãi: Giảm %s (tối đa %s)\n" +
               "\uD83D\uDCC5 Thời gian áp dụng: Từ %s đến %s\n" +
               "\uD83D\uDCCC Điều kiện áp dụng: %s\n" +
               "Đây là ưu đãi đặc biệt chỉ dành cho Quý khách hàng VIP và không áp dụng cho các chương trình khác.\n" +
               "\uD83D\uDC49 Hãy nhanh tay sử dụng mã giảm giá để tận hưởng những sản phẩm/dịch vụ chất lượng nhất từ My Laptop.\n" +
               "Chúng tôi rất mong tiếp tục được phục vụ và mang đến cho Quý khách những trải nghiệm tuyệt vời nhất.\n" +
               "Trân trọng,\n" +
               "My Laptop";
    }
}