package com.sd20201.datn.core.client.voucher.service.impl;

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
public class ClientVoucherServiceImpl implements AdVoucherService {

    private final AdVoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;
    private final AdVoucherDetailRepository voucherDetailRepository;
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
    public ResponseObject<?> changeStatusVoucherToStart(String id) {
        Long now = System.currentTimeMillis();
        return voucherRepository.findById(id).map(v -> {
            v.setStartDate(now);
            voucherRepository.save(v);
            return ResponseObject.successForward(null, "Kích hoạt voucher thành công");
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
            voucher.setStatus(voucher.getStatus() == EntityStatus.ACTIVE ? EntityStatus.INACTIVE : EntityStatus.ACTIVE);
            voucherRepository.save(voucher);
            return ResponseObject.successForward(null, "Cập nhật trạng thái thành công");
        }).orElse(ResponseObject.errorForward("Cập nhật thất bại! không tìm thấy voucher", HttpStatus.NOT_FOUND));
    }

    /* ===================== CREATE / UPDATE ===================== */

    @Override
    public ResponseObject<?> create(@Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        // 1. Validate cơ bản
        if (voucherRepository.findVoucherByName(request.getName()).isPresent()) {
            throw new DuplicateKeyException("Tên voucher đã tồn tại: " + request.getName());
        }

        Helper.validateVoucherInput(request);

        // 2. Map dữ liệu
        Voucher voucher = new Voucher();
        voucher.setCode(Helper.generateCodeVoucher());
        Helper.mapRequestToVoucher(request, voucher);

        List<String> failedEmails = new ArrayList<>();

        // 3. Xử lý theo loại đối tượng
        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0 cho Voucher Công Khai");
            }
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());

            // Chỉ lưu Voucher cha, không tạo detail cho tất cả khách
            voucher = voucherRepository.save(voucher);

        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherUsers() == null || request.getVoucherUsers().isEmpty()) {
                throw new IllegalArgumentException("Danh sách khách hàng không được để trống");
            }

            // Load và validate danh sách khách hàng
            List<Customer> customers = new ArrayList<>();
            List<String> missingIds = new ArrayList<>();
            request.getVoucherUsers().forEach(vd -> {
                String cid = vd.getCustomer().getId();
                customerRepository.findById(cid).ifPresentOrElse(customers::add, () -> missingIds.add(cid));
            });

            if (!missingIds.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng với IDs: " + String.join(", ", missingIds));
            }

            int count = customers.size();
            voucher.setQuantity(count);
            voucher.setRemainingQuantity(count);

            // Lưu voucher trước khi gán detail
            voucher = voucherRepository.save(voucher);

            // Tạo detail và gửi mail
            List<VoucherDetail> details = new ArrayList<>();
            for (Customer c : customers) {
                details.add(createVoucherDetail(voucher, c));
            }
            voucherDetailRepository.saveAll(details);

            for (Customer c : customers) {
                sendVoucherEmail(c, voucher, failedEmails);
            }

        } else {
            throw new IllegalArgumentException("Loại đối tượng (TargetType) không hợp lệ!");
        }

        String message = "Thêm mới voucher thành công";
        if (!failedEmails.isEmpty()) message += ". Gửi mail thất bại cho: " + String.join(", ", failedEmails);

        return ResponseObject.successForward(null, message);
    }

    @Override
    public ResponseObject<?> update(String id, @Valid AdVoucherCreateUpdateRequest request) throws BadRequestException {
        Voucher voucher = voucherRepository.findById(id)
                .orElse(null);

        if (voucher == null) {
            return ResponseObject.errorForward("Không tìm thấy voucher để cập nhật", HttpStatus.NOT_FOUND);
        }

        TargetType oldTargetType = voucher.getTargetType();

        // Kiểm tra xem nội dung quan trọng có thay đổi không (để quyết định gửi mail cho khách cũ)
        boolean isContentChanged = Helper.isVoucherContentChanged(voucher, request);

        // Validate
        if (!voucher.getName().equals(request.getName()) &&
            voucherRepository.findVoucherByName(request.getName()).isPresent()) {
            throw new DuplicateKeyException("Tên voucher mới đã tồn tại: " + request.getName());
        }
        Helper.validateVoucherInput(request);

        // Update thông tin voucher
        Helper.mapRequestToVoucher(request, voucher);

        // Chặn chuyển đổi từ Cá nhân -> Công khai
        if (oldTargetType == TargetType.INDIVIDUAL && request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            throw new BadRequestException("Không thể thay đổi Voucher từ 'Cá nhân' sang 'Công khai'!");
        }

        List<String> failedEmails = new ArrayList<>();

        if (request.getTargetType() == TargetType.ALL_CUSTOMERS) {
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0 cho Voucher Công Khai");
            }
            voucher.setQuantity(request.getQuantity());
            voucher.setRemainingQuantity(request.getQuantity());

            if (oldTargetType == TargetType.INDIVIDUAL) {
                voucherDetailRepository.deleteByVoucher(voucher);
            }
            voucherRepository.save(voucher);

        } else if (request.getTargetType() == TargetType.INDIVIDUAL) {
            if (request.getVoucherUsers() == null || request.getVoucherUsers().isEmpty()) {
                throw new IllegalArgumentException("Danh sách khách hàng không được để trống");
            }

            // Logic xử lý không xoá khách cũ
            List<VoucherDetail> currentDetails = voucherDetailRepository.findAllByVoucher(voucher);
            Set<String> currentCustomerIds = currentDetails.stream()
                    .map(d -> d.getCustomer().getId())
                    .collect(Collectors.toSet());

            Set<String> requestCustomerIds = request.getVoucherUsers().stream()
                    .map(vd -> vd.getCustomer().getId())
                    .collect(Collectors.toSet());

            if (!requestCustomerIds.containsAll(currentCustomerIds)) {
                throw new BadRequestException("Không thể hủy khách hàng đã gán voucher, chỉ được phép thêm mới!");
            }

            List<String> newCustomerIds = requestCustomerIds.stream()
                    .filter(idReq -> !currentCustomerIds.contains(idReq))
                    .toList();

            List<Customer> customersToSendEmail = new ArrayList<>();

            // Xử lý khách hàng mới
            if (!newCustomerIds.isEmpty()) {
                List<Customer> newCustomers = customerRepository.findAllById(newCustomerIds);
                if (newCustomers.size() < newCustomerIds.size()) {
                    throw new BadRequestException("Có ID khách hàng không tồn tại");
                }

                int addedCount = newCustomers.size();
                voucher.setQuantity(voucher.getQuantity() + addedCount);
                voucher.setRemainingQuantity(voucher.getRemainingQuantity() + addedCount);

                List<VoucherDetail> newDetails = new ArrayList<>();
                for (Customer c : newCustomers) {
                    newDetails.add(createVoucherDetail(voucher, c));
                }
                voucherDetailRepository.saveAll(newDetails);

                customersToSendEmail.addAll(newCustomers); // Luôn gửi mail cho khách mới
            }

            voucherRepository.save(voucher);

            // Nếu nội dung thay đổi, gửi thêm cho toàn bộ khách cũ
            if (isContentChanged) {
                Set<Customer> oldCustomers = currentDetails.stream()
                        .map(VoucherDetail::getCustomer)
                        .collect(Collectors.toSet());
                customersToSendEmail.addAll(oldCustomers);
            }

            // Gửi mail (Distinct để tránh gửi trùng nếu logic trên có lỗi)
            for (Customer c : customersToSendEmail.stream().distinct().toList()) {
                sendVoucherEmail(c, voucher, failedEmails);
            }
        }

        String message = "Cập nhật voucher thành công";
        if (!failedEmails.isEmpty()) message += ". Gửi mail thất bại cho: " + String.join(", ", failedEmails);

        return ResponseObject.successForward(null, message);
    }

    // --- PRIVATE UTILS METHODS ---

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

    private void sendVoucherEmail(Customer customer, Voucher voucher, List<String> failedEmails) {
        try {
            String htmlBody = Helper.createVoucherEmailBody(voucher, customer);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.toString());
            helper.setTo(customer.getEmail());
            helper.setSubject("🎁 Ưu đãi dành riêng cho bạn - Mã giảm giá " + voucher.getCode());
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("✅ Email sent to: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", customer.getEmail(), e.getMessage());
            if (failedEmails != null) failedEmails.add(customer.getEmail());
        }
    }

    /* ===================== DELETE & MAPS (Giữ nguyên) ===================== */
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
        if (voucher == null) {
            return ResponseObject.errorForward("Không tìm thấy voucher", HttpStatus.NOT_FOUND);
        }

        Page<?> page = onlyUsed
                ? voucherRepository.findUsedCustomersByVoucherCode(voucher.getCode(), pageable)
                : voucherRepository.findAssignedCustomersByVoucherId(voucherId, pageable);

        return ResponseObject.successForward(
                PageableObject.of(page),
                "Lấy danh sách khách hàng thành công"
        );
    }


}