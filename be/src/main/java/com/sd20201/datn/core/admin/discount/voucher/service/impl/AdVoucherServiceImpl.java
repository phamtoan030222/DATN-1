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
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdVoucherServiceImpl implements AdVoucherService {

    private final AdVoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;
    private final VoucherDetailRepository voucherDetailRepository;
    private static final Logger logger = LoggerFactory.getLogger(AdVoucherServiceImpl.class);
    private final JavaMailSender mailSender;

    /* ===================== QUERY LIST VOUCHER ===================== */
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

    /* ===================== CREATE / UPDATE ===================== */
    @Override
    public ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        logger.debug("Received create request: {}", request);

        if (voucherRepository.findVoucherByName(request.getName()).isPresent()) {
            throw new DuplicateKeyException("Tên voucher đã tồn tại: " + request.getName());
        }
        validateDateRange(request.getStartDate(), request.getEndDate());

        Voucher voucher = new Voucher();
        voucher.setCode(Helper.generateCodeVoucher());
        mapRequestToVoucher(request, voucher);

        List<String> failedEmails = new ArrayList<>();

        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0 cho ALL_CUSTOMERS");
            }
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());

            // lưu voucher
            voucher = voucherRepository.save(voucher);

            List<Customer> customers = customerRepository.findAll();
            List<VoucherDetail> details = new ArrayList<>(customers.size());
            for (Customer c : customers) {
                VoucherDetail vd = new VoucherDetail();
                vd.setVoucher(voucher);
                vd.setCode(Helper.generateCodeProductDetail());
                vd.setName("Dành cho " + c.getName());
                vd.setCustomer(c);
                vd.setUsageStatus(false);
                vd.setDescription("Assigned to customer " + c.getId());
                details.add(vd);
            }
            voucherDetailRepository.saveAll(details);


        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherUsers() == null || request.getVoucherUsers().isEmpty()) {
                throw new IllegalArgumentException("Danh sách khách hàng cá nhân không được để trống cho Khách Hàng Riêng");
            }

            // load và validate KH
            List<Customer> customers = new ArrayList<>();
            List<String> missingIds = new ArrayList<>();
            request.getVoucherUsers().forEach(vd -> {
                String cid = vd.getCustomer().getId();
                customerRepository.findById(cid).ifPresentOrElse(
                        customers::add, () -> missingIds.add(cid)
                );
            });
            if (!missingIds.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng với IDs: " + String.join(", ", missingIds));
            }

            int customerCount = customers.size();
            voucher.setQuantity(customerCount);
            voucher.setRemainingQuantity(customerCount);

            // 1) lưu voucher
            voucher = voucherRepository.save(voucher);
            // 2) lưu chi tiết gán
            List<VoucherDetail> details = new ArrayList<>(customerCount);
            for (Customer c : customers) {
                VoucherDetail vd = new VoucherDetail();
                vd.setVoucher(voucher);
                vd.setCode(Helper.generateCodeProductDetail());
                vd.setName("Dành cho " + c.getName());
                vd.setCustomer(c);
                vd.setUsageStatus(false);
                vd.setDescription("Assigned to customer " + c.getId());
                details.add(vd);
            }
            voucherDetailRepository.saveAll(details);

            // 3) gửi mail
            for (Customer c : customers) {
                try {
                    sendVoucherEmail(c, voucher);
                } catch (MailException e) {
                    logger.error("❌ Failed to send email to {}: {}", c.getEmail(), e.getMessage());
                    failedEmails.add(c.getEmail());
                }
            }

        } else {
            throw new IllegalArgumentException("TargetType không hợp lệ!");
        }

        String message = "Thêm thành công voucher";
        if (!failedEmails.isEmpty()) {
            message += ". Tuy nhiên, gửi email thất bại cho: " + String.join(", ", failedEmails);
        }
        return ResponseObject.successForward(voucher, message);
    }

    @Override
    public ResponseObject<?> update(String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        Optional<Voucher> opt = voucherRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseObject.errorForward("Không tìm thấy voucher để cập nhật", HttpStatus.NOT_FOUND);
        }
        Voucher voucher = opt.get();

        if (!voucher.getName().equals(request.getName()) &&
            voucherRepository.findVoucherByName(request.getName()).isPresent()) {
            throw new DuplicateKeyException("Tên voucher mới đã tồn tại: " + request.getName());
        }
        validateDateRange(request.getStartDate(), request.getEndDate());

        List<String> failedEmails = new ArrayList<>();
        mapRequestToVoucher(request, voucher);

        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0 cho ALL_CUSTOMERS");
            }
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());

            // Nếu trước đó là INDIVIDUAL thì xoá assign cũ
            if (voucher.getTargetType() == TargetType.INDIVIDUAL) {
                voucherDetailRepository.deleteByVoucher(voucher);
            }

            voucher = voucherRepository.save(voucher);

        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherUsers() == null || request.getVoucherUsers().isEmpty()) {
                throw new IllegalArgumentException("Danh sách khách hàng cá nhân không được để trống cho Khách Hàng Riêng");
            }

            // Xoá assign cũ, gán mới
            voucherDetailRepository.deleteByVoucher(voucher);

            List<Customer> customers = new ArrayList<>();
            List<String> missingIds = new ArrayList<>();
            request.getVoucherUsers().forEach(vd -> {
                String cid = vd.getCustomer().getId();
                customerRepository.findById(cid).ifPresentOrElse(
                        customers::add, () -> missingIds.add(cid)
                );
            });
            if (!missingIds.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng với IDs: " + String.join(", ", missingIds));
            }

            int customerCount = customers.size();
            voucher.setQuantity(customerCount);
            voucher.setRemainingQuantity(customerCount);

            voucher = voucherRepository.save(voucher);

            List<VoucherDetail> details = new ArrayList<>(customerCount);
            for (Customer c : customers) {
                VoucherDetail vd = new VoucherDetail();
                vd.setVoucher(voucher);
                vd.setCustomer(c);
                vd.setUsageStatus(false);
                vd.setDescription("Assigned to customer " + c.getId());
                details.add(vd);
            }
            voucherDetailRepository.saveAll(details);

            for (Customer c : customers) {
                try {
                    sendVoucherEmail(c, voucher);
                } catch (MailException e) {
                    logger.error("❌ Failed to send email to {}: {}", c.getEmail(), e.getMessage());
                    failedEmails.add(c.getEmail());
                }
            }

        } else {
            throw new IllegalArgumentException("TargetType không hợp lệ!");
        }

        String message = "Chỉnh sửa thành công voucher";
        if (!failedEmails.isEmpty()) {
            message += ". Tuy nhiên, gửi email thất bại cho: " + String.join(", ", failedEmails);
        }
        return ResponseObject.successForward(voucher, message);
    }

    /* ===================== DELETE ===================== */
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

    /* ===================== MAPS (giữ nguyên) ===================== */
    @Override
    public Map<Voucher, List<Customer>> getCustomersByVoucher(boolean onlyActive) {
        List<Object[]> rows = voucherRepository.getVouchersWithCustomers(onlyActive);
        Map<Voucher, List<Customer>> map = new HashMap<>();
        for (Object[] r : rows) {
            Voucher v = (Voucher) r[0];
            Customer c = (Customer) r[1];
            if (c != null) map.computeIfAbsent(v, k -> new ArrayList<>()).add(c);
        }
        return map;
    }

    @Override
    public Map<Customer, List<Voucher>> getVouchersByCustomer(boolean onlyActive) {
        List<Object[]> rows = voucherRepository.getVouchersWithCustomers(onlyActive);
        Map<Customer, List<Voucher>> map = new HashMap<>();
        for (Object[] r : rows) {
            Voucher v = (Voucher) r[0];
            Customer c = (Customer) r[1];
            if (c != null) map.computeIfAbsent(c, k -> new ArrayList<>()).add(v);
        }
        return map;
    }

    /* ===================== NEW: Paged customers of a voucher ===================== */
    // Thêm vào AdVoucherService interface:
    // ResponseObject<?> getCustomersOfVoucher(String voucherId, boolean onlyUsed, org.springframework.data.domain.Pageable pageable);

    @Override
    public ResponseObject<?> getCustomersOfVoucher(String voucherId, boolean onlyUsed, Pageable pageable) {
        Optional<Voucher> opt = voucherRepository.findById(voucherId);
        if (opt.isEmpty()) {
            return ResponseObject.errorForward("Không tìm thấy voucher", HttpStatus.NOT_FOUND);
        }
        Voucher voucher = opt.get();

        // Sửa kiểu biến từ Page<Customer> thành Page<?> hoặc Page<AdCustomerResponse>
        Page<?> page = onlyUsed
                ? voucherRepository.findUsedCustomersByVoucherCode(voucher.getCode(), pageable)
                : voucherRepository.findAssignedCustomersByVoucherId(voucherId, pageable);

        return ResponseObject.successForward(
                PageableObject.of(page),
                "Lấy danh sách khách hàng theo voucher thành công"
        );
    }

    /* ===================== HELPERS ===================== */
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

        long now = DateTimeUtil.getCurrentTimeMillisecondsStamp();
        voucher.setStatus((request.getStartDate() <= now && now <= request.getEndDate())
                ? EntityStatus.ACTIVE
                : EntityStatus.INACTIVE);
    }

    private void validateDateRange(Long start, Long end) throws BadRequestException {
        if (start == null || end == null) {
            throw new BadRequestException("StartDate hoặc EndDate không được để trống!!");
        }
        if (start >= end) {
            throw new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc!!");
        }
        if (end < DateTimeUtil.getCurrentTimeMillisecondsStamp()) {
            throw new BadRequestException("Ngày kết thúc không được nhỏ hơn hiện tại!!");
        }
    }

    // gửi mail dựa trên Customer đã có sẵn
    private void sendVoucherEmail(Customer customer, Voucher voucher) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getStartDate()), ZoneId.systemDefault());
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getEndDate()), ZoneId.systemDefault());

            String discount = voucher.getTypeVoucher() == TypeVoucher.PERCENTAGE
                    ? voucher.getDiscountValue() + "%"
                    : voucher.getDiscountValue() + " VND";

            String htmlTemplate = """
                        <html>
                          <body style="font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px;">
                            <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                              <div style="background: #ff6600; padding: 20px; text-align: center; color: white;">
                                <h1 style="margin: 0;">🎁 Ưu Đãi Đặc Biệt Dành Cho Bạn</h1>
                              </div>
                              <div style="padding: 20px; color: #333;">
                                <p>Xin chào, {6}</p>
                                <p>Chúng tôi gửi tặng bạn một <b>phiếu giảm giá đặc biệt</b>. Hãy sử dụng ngay để nhận ưu đãi hấp dẫn!</p>
                                <table style="width: 100%; border-collapse: collapse; margin-top: 20px;">
                                  <tr style="background: #f2f2f2;">
                                    <td style="padding: 10px; font-weight: bold;">Mã Voucher</td>
                                    <td style="padding: 10px; color: #ff6600; font-size: 18px; font-weight: bold;">{0}</td>
                                  </tr>
                                  <tr>
                                    <td style="padding: 10px; font-weight: bold;">Giá trị giảm</td>
                                    <td style="padding: 10px;">{1}</td>
                                  </tr>
                                  <tr style="background: #f2f2f2;">
                                    <td style="padding: 10px; font-weight: bold;">Giảm tối đa</td>
                                    <td style="padding: 10px;">{2} VND</td>
                                  </tr>
                                  <tr>
                                    <td style="padding: 10px; font-weight: bold;">Thời gian áp dụng</td>
                                    <td style="padding: 10px;">Từ {3} đến {4}</td>
                                  </tr>
                                  <tr style="background: #f2f2f2;">
                                    <td style="padding: 10px; font-weight: bold;">Điều kiện</td>
                                    <td style="padding: 10px;">Đơn hàng từ {5} VND</td>
                                  </tr>
                                </table>
                                <div style="text-align: center; margin-top: 30px;">
                                  <a href="https://your-shop.com"
                                     style="background: #ff6600; color: white; padding: 12px 24px; border-radius: 5px; text-decoration: none; font-weight: bold;">
                                    Mua sắm ngay
                                  </a>
                                </div>
                              </div>
                              <div style="background: #eee; text-align: center; padding: 15px; font-size: 12px; color: #777;">
                                © 2025 YourShop. Mọi quyền được bảo lưu.
                              </div>
                            </div>
                          </body>
                        </html>
                    """;

            String htmlBody = MessageFormat.format(
                    htmlTemplate,
                    voucher.getCode(),
                    discount,
                    com.sd20201.datn.utils.DateTimeUtil.formatMoney(voucher.getMaxValue()),
                    start.format(formatter),
                    end.format(formatter),
                    com.sd20201.datn.utils.DateTimeUtil.formatMoney(voucher.getConditions()),
                    customer.getName()
            );

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.toString());
            helper.setTo(customer.getEmail());
            helper.setSubject("🎁 Ưu đãi dành riêng cho bạn - Mã giảm giá " + voucher.getCode());
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            logger.info("✅ Email voucher sent to: {}", customer.getEmail());

        } catch (Exception e) {
            logger.error("❌ Error sending voucher email: {}", e.getMessage(), e);
        }
    }

}
