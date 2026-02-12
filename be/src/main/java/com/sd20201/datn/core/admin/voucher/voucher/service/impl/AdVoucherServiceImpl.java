package com.sd20201.datn.core.admin.voucher.voucher.service.impl;

import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherRequest;
import com.sd20201.datn.core.admin.voucher.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.core.admin.voucher.voucher.service.AdVoucherService;
import com.sd20201.datn.core.admin.voucher.voucherudetail.repository.AdVoucherDetailRepository;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.entity.VoucherDetail;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.utils.Helper;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdVoucherServiceImpl implements AdVoucherService {

    private final AdVoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;
    private final AdVoucherDetailRepository voucherDetailRepository;
    private final JavaMailSender mailSender;

    // ... (Query methods giữ nguyên)
    @Override
    public ResponseObject<?> getVouchers(AdVoucherRequest request) {
        return ResponseObject.successForward(PageableObject.of(voucherRepository.getVouchers(Helper.createPageable(request), request)), "Lấy danh sách voucher thành công");
    }

    @Override
    public ResponseObject<?> getVoucherById(String id) {
        return voucherRepository.getVoucherById(id).map(res -> ResponseObject.successForward(res, "Lấy chi tiết voucher thành công")).orElse(ResponseObject.errorForward("Lấy chi tiết thất bại", HttpStatus.NOT_FOUND));
    }

    // --- STATUS CHANGE (Đã sửa lỗi gửi mail) ---
    @Override
    public ResponseObject<?> changeStatusVoucherToStart(String id) {
        Long now = System.currentTimeMillis();
        return voucherRepository.findById(id).map(v -> {
            v.setStartDate(now);
            v.setStatus(EntityStatus.ACTIVE);
            voucherRepository.save(v);

            List<String> failedEmails = new ArrayList<>();
            int count = 0;
            try {
                // Logic gửi mail cho khách hàng đã được gán voucher này
                List<VoucherDetail> details = voucherDetailRepository.findAllByVoucher(v);
                for (VoucherDetail vd : details) {
                    Customer c = vd.getCustomer();
                    if (c != null && c.getEmail() != null && !c.getEmail().isBlank()) {
                        sendVoucherStartNowEmail(c, v, failedEmails);
                        count++;
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi gửi mail start: {}", e.getMessage());
            }

            String msg = "Kích hoạt voucher thành công, đã gửi mail cho " + count + " khách.";
            return ResponseObject.successForward(null, msg);
        }).orElse(ResponseObject.errorForward("Không tìm thấy voucher", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatusVoucherToEnd(String id) {
        Long now = System.currentTimeMillis();
        return voucherRepository.findById(id).map(v -> {
            v.setEndDate(now);
            v.setRemainingQuantity(0);
            voucherRepository.save(v);
            return ResponseObject.successForward(null, "Kết thúc voucher thành công");
        }).orElse(ResponseObject.errorForward("Không tìm thấy voucher", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> changeStatusVoucher(String id) {
        return voucherRepository.findById(id).map(voucher -> {
            EntityStatus newStatus = voucher.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE;
            voucher.setStatus(newStatus);
            voucherRepository.save(voucher);

            List<String> failedEmails = new ArrayList<>();
            int notifiedCount = 0;
            try {
                List<VoucherDetail> details = voucherDetailRepository.findAllByVoucher(voucher);
                for (VoucherDetail vd : details) {
                    Customer c = vd.getCustomer();
                    if (c != null && c.getEmail() != null && !c.getEmail().isBlank()) {
                        if (newStatus == EntityStatus.INACTIVE) {
                            sendVoucherPausedEmail(c, voucher, failedEmails);
                            notifiedCount++;
                        } else if (newStatus == EntityStatus.ACTIVE) {
                            sendVoucherResumedEmail(c, voucher, failedEmails);
                            notifiedCount++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Gửi email thất bại: {}", e.getMessage());
            }

            String msg = "Cập nhật thành công";
            if (newStatus == EntityStatus.INACTIVE) msg += ", đã báo tạm dừng cho " + notifiedCount + " khách";
            else msg += ", đã báo tiếp tục cho " + notifiedCount + " khách";

            return ResponseObject.successForward(null, msg);
        }).orElse(ResponseObject.errorForward("Không tìm thấy voucher", HttpStatus.NOT_FOUND));
    }

    // --- CRUD (Có Validate) ---
    @Override
    public ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        // 1. VALIDATE MỚI (Length & Value)
        Helper.validateVoucherInput(request);

        if (voucherRepository.findVoucherByName(request.getName()).isPresent())
            throw new DuplicateKeyException("Tên trùng: " + request.getName());
        Voucher voucher = new Voucher();
        voucher.setCode(Helper.generateCodeVoucher());
        Helper.mapRequestToVoucher(request, voucher);

        List<String> failedEmails = new ArrayList<>();
        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            if (request.getQuantity() == null || request.getQuantity() <= 0)
                throw new IllegalArgumentException("Số lượng phải > 0");
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());
            voucher = voucherRepository.save(voucher);
        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherUsers() == null || request.getVoucherUsers().isEmpty())
                throw new IllegalArgumentException("DS khách hàng trống");
            List<Customer> customers = new ArrayList<>();
            request.getVoucherUsers().forEach(vd -> {
                String cid = vd.getCustomer().getId();
                customerRepository.findById(cid).ifPresent(customers::add);
            });
            int count = customers.size();
            voucher.setQuantity(count);
            voucher.setRemainingQuantity(count);
            voucher = voucherRepository.save(voucher);
            List<VoucherDetail> details = new ArrayList<>();
            for (Customer c : customers) details.add(createVoucherDetail(voucher, c));
            voucherDetailRepository.saveAll(details);
            for (Customer c : customers) sendVoucherStartNowEmail(c, voucher, failedEmails);
        } else {
            throw new IllegalArgumentException("TargetType lỗi");
        }

        return ResponseObject.successForward(null, "Thêm mới thành công");
    }

    @Override
    public ResponseObject<?> update(String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        // 1. VALIDATE MỚI
        Helper.validateVoucherInput(request);

        Voucher voucher = voucherRepository.findById(id).orElse(null);
        if (voucher == null) return ResponseObject.errorForward("Không tìm thấy", HttpStatus.NOT_FOUND);
        TargetType oldTargetType = voucher.getTargetType();
        boolean isContentChanged = Helper.isVoucherContentChanged(voucher, request);
        if (!voucher.getName().equals(request.getName()) && voucherRepository.findVoucherByName(request.getName()).isPresent())
            throw new DuplicateKeyException("Tên trùng");
        Helper.mapRequestToVoucher(request, voucher);
        if (oldTargetType == TargetType.INDIVIDUAL && request.getTargetType() == TargetType.ALL_CUSTOMERS)
            throw new BadRequestException("Không thể đổi Cá nhân -> Công khai");

        List<String> failedEmails = new ArrayList<>();
        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            if (request.getQuantity() == null || request.getQuantity() <= 0)
                throw new IllegalArgumentException("Số lượng > 0");
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());
            if (oldTargetType == TargetType.INDIVIDUAL) voucherDetailRepository.deleteByVoucher(voucher);
            voucherRepository.save(voucher);
        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherUsers() == null || request.getVoucherUsers().isEmpty())
                throw new IllegalArgumentException("DS khách trống");
            List<VoucherDetail> currentDetails = voucherDetailRepository.findAllByVoucher(voucher);
            Set<String> currentCustomerIds = currentDetails.stream().map(d -> d.getCustomer().getId()).collect(Collectors.toSet());
            Set<String> requestCustomerIds = request.getVoucherUsers().stream().map(vd -> vd.getCustomer().getId()).collect(Collectors.toSet());
            if (!requestCustomerIds.containsAll(currentCustomerIds))
                throw new BadRequestException("Không được xóa khách cũ");
            List<String> newCustomerIds = requestCustomerIds.stream().filter(idReq -> !currentCustomerIds.contains(idReq)).toList();
            List<Customer> customersToSendEmail = new ArrayList<>();
            if (!newCustomerIds.isEmpty()) {
                List<Customer> newCustomers = customerRepository.findAllById(newCustomerIds);
                int addedCount = newCustomers.size();
                voucher.setQuantity(voucher.getQuantity() + addedCount);
                voucher.setRemainingQuantity(voucher.getRemainingQuantity() + addedCount);
                List<VoucherDetail> newDetails = new ArrayList<>();
                for (Customer c : newCustomers) newDetails.add(createVoucherDetail(voucher, c));
                voucherDetailRepository.saveAll(newDetails);
                customersToSendEmail.addAll(newCustomers);
            }
            voucherRepository.save(voucher);
            if (isContentChanged) {
                Set<Customer> oldCustomers = currentDetails.stream().map(VoucherDetail::getCustomer).collect(Collectors.toSet());
                customersToSendEmail.addAll(oldCustomers);
            }
            for (Customer c : customersToSendEmail.stream().distinct().toList())
                sendVoucherStartNowEmail(c, voucher, failedEmails);
        }
        return ResponseObject.successForward(null, "Cập nhật thành công");
    }

    // --- Utils & Send Mail ---
    private VoucherDetail createVoucherDetail(Voucher voucher, Customer c) {
        VoucherDetail vd = new VoucherDetail();
        vd.setVoucher(voucher);
        vd.setCode(Helper.generateCodeVoucherDetail());
        vd.setName("Dành cho " + c.getName());
        vd.setCustomer(c);
        vd.setUsageStatus(false);
        vd.setDescription("Assigned to customer " + c.getId());
        return vd;
    }

    private void sendVoucherPausedEmail(Customer customer, Voucher voucher, List<String> failedEmails) {
        try {
            String htmlBody = Helper.createPausedEmailBody(voucher, customer);
            sendHtmlEmail(customer.getEmail(), "⏸️ Tạm hoãn Voucher: " + voucher.getCode(), htmlBody, failedEmails);
        } catch (Exception e) {
            log.error("Error pause email: {}", e.getMessage());
        }
    }

    private void sendVoucherResumedEmail(Customer customer, Voucher voucher, List<String> failedEmails) {
        try {
            String htmlBody = Helper.createResumedEmailBody(voucher, customer);
            sendHtmlEmail(customer.getEmail(), "✅ Voucher hoạt động trở lại: " + voucher.getCode(), htmlBody, failedEmails);
        } catch (Exception e) {
            log.error("Error resume email: {}", e.getMessage());
        }
    }

    private void sendVoucherStartNowEmail(Customer customer, Voucher voucher, List<String> failedEmails) {
        try {
            String htmlBody = Helper.createVoucherEmailBody(voucher, customer);
            sendHtmlEmail(customer.getEmail(), "🎁 Voucher dành cho bạn: " + voucher.getCode(), htmlBody, failedEmails);
        } catch (Exception e) {
            log.error("Error start email: {}", e.getMessage());
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody, List<String> failedEmails) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.toString());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("✅ Email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage());
            if (failedEmails != null) failedEmails.add(toEmail);
        }
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

    @Override
    public ResponseObject<?> getCustomersOfVoucher(String voucherId, boolean onlyUsed, Pageable pageable) {
        Voucher voucher = voucherRepository.findById(voucherId).orElse(null);
        if (voucher == null) return ResponseObject.errorForward("Không tìm thấy voucher", HttpStatus.NOT_FOUND);
        Page<?> page = onlyUsed ? voucherRepository.findUsedCustomersByVoucherCode(voucher.getCode(), pageable) : voucherRepository.findAssignedCustomersByVoucherId(voucherId, pageable);
        return ResponseObject.successForward(PageableObject.of(page), "Lấy danh sách khách hàng thành công");
    }
}