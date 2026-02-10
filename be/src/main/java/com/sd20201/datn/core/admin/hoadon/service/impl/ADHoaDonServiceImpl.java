package com.sd20201.datn.core.admin.hoadon.service.impl;

import com.sd20201.datn.core.admin.shift.repository.AdShiftHandoverRepository;
import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.core.admin.banhang.repository.ADBanHangIMEIRepository;
import com.sd20201.datn.core.admin.hoadon.model.request.ADChangeStatusRequest;
import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonDetailRequest;
import com.sd20201.datn.core.admin.hoadon.model.request.ADHoaDonSearchRequest;
import com.sd20201.datn.core.admin.hoadon.model.response.ADHoaDonChiTietResponseDetail;
import com.sd20201.datn.core.admin.hoadon.model.response.HoaDonPageResponse;
import com.sd20201.datn.core.admin.hoadon.repository.ADHoaDonChiTietRepository;
import com.sd20201.datn.core.admin.hoadon.repository.ADInvoiceRepository;
import com.sd20201.datn.core.admin.hoadon.service.ADHoaDonService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.*;
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.ImeiStatus;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import com.sd20201.datn.repository.*;
import com.sd20201.datn.utils.EmailService;
import com.sd20201.datn.utils.Helper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ADHoaDonServiceImpl implements ADHoaDonService {

    private final ADInvoiceRepository adHoaDonRepository;
    private final ADHoaDonChiTietRepository adHoaDonChiTietRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiRepository;
    private final StaffRepository staffRepository;
    private final ADBanHangIMEIRepository imeiRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherDetailRepository voucherDetailRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final CustomerRepository customerRepository;
    private final AdShiftHandoverRepository shiftHandoverRepository;

    @Override
    @Transactional
    public ResponseObject<?> capNhatTrangThaiHoaDon(ADChangeStatusRequest request) {
        try {
            log.info("Bắt đầu cập nhật trạng thái hóa đơn: {}", request.getMaHoaDon());

            // 1. Lấy hóa đơn theo mã
            Invoice hoaDon = adHoaDonRepository.findByMa(request.getMaHoaDon())
                    .orElseThrow(() ->
                            new RuntimeException("Không tìm thấy hóa đơn: " + request.getMaHoaDon())
                    );

            EntityTrangThaiHoaDon trangThaiCu = hoaDon.getEntityTrangThaiHoaDon();
            EntityTrangThaiHoaDon trangThaiMoi = request.getStatusTrangThaiHoaDon();

            log.info("Trạng thái cũ: {}, Trạng thái mới: {}", trangThaiCu, trangThaiMoi);

            // 2. Kiểm tra luồng trạng thái hợp lệ
            kiemTraChuyenTrangThai(trangThaiCu, trangThaiMoi);

            Staff nhanVien;
            // Nếu request có gửi id nhân viên → lấy theo ID
            if (request.getIdNhanVien() != null) {
                nhanVien = staffRepository.findById(request.getIdNhanVien())
                        .orElseThrow(() ->
                                new RuntimeException("Không tìm thấy nhân viên ID = "
                                        + request.getIdNhanVien())
                        );
            } else {
                // Nếu không gửi → lấy nhân viên đang đăng nhập
                nhanVien = getCurrentStaff();
            }

            if (hoaDon.getShiftHandover() == null && nhanVien != null && nhanVien.getAccount() != null) {
                try {
                    String accountId = nhanVien.getAccount().getId();
                    // Tìm ca đang mở (ACTIVE) của nhân viên này
                    Optional<ShiftHandover> caDangMo = shiftHandoverRepository.findOpenShiftByAccountId(accountId);

                    if (caDangMo.isPresent()) {
                        hoaDon.setShiftHandover(caDangMo.get());
                        log.info("Đã tự động gán hóa đơn {} vào ca làm việc {}", hoaDon.getCode(), caDangMo.get().getId());
                    }
                } catch (Exception e) {
                    log.warn("Không thể gán ca làm việc cho hóa đơn: {}", e.getMessage());
                }
            }

            // 4. Cập nhật trạng thái hóa đơn
            hoaDon.setEntityTrangThaiHoaDon(trangThaiMoi);
            hoaDon.setLastModifiedDate(System.currentTimeMillis());

            // Cập nhật paymentDate nếu chuyển sang trạng thái HOÀN THÀNH
            if (trangThaiMoi == EntityTrangThaiHoaDon.HOAN_THANH && hoaDon.getPaymentDate() == null) {
                hoaDon.setPaymentDate(System.currentTimeMillis());
            }

            // Reset paymentDate nếu hủy đơn hàng đã thanh toán
            if (trangThaiMoi == EntityTrangThaiHoaDon.DA_HUY && hoaDon.getPaymentDate() != null) {
                hoaDon.setPaymentDate(null);
            }

            Invoice hoaDonDaCapNhat = adHoaDonRepository.save(hoaDon);

            // 5. Lưu lịch sử trạng thái vào bảng lịch sử
            LichSuTrangThaiHoaDon lichSu = luuLichSuTrangThaiHoaDon(
                    hoaDonDaCapNhat,
                    trangThaiMoi,
                    request.getNote(),
                    nhanVien
            );

            // 6. XỬ LÝ NGHIỆP VỤ THEO TRẠNG THÁI
            switch (trangThaiMoi) {
                case CHO_XAC_NHAN:
                case DA_XAC_NHAN:
                    khoaIMEIKhiChoXacNhan(hoaDonDaCapNhat);
                    break;

                case CHO_GIAO:
                    danhDauIMEIDaBan(hoaDonDaCapNhat);
                    luuLichSuThanhToan(hoaDonDaCapNhat, nhanVien);
                    danhDauVoucherDaSuDung(hoaDonDaCapNhat);
                    break;

                case DANG_GIAO:
                    // Không phát sinh nghiệp vụ mới
                    break;

                case HOAN_THANH:
                    // Kết thúc vòng đời hóa đơn
                    danhDauIMEIDaBan(hoaDonDaCapNhat);
                    break;

                case DA_HUY:
                    traIMEIVeKho(hoaDonDaCapNhat);
                    hoanTraVoucher(hoaDonDaCapNhat);
                    hoanTienNeuCan(hoaDonDaCapNhat, nhanVien);
                    break;

                default:
                    break;
            }

            // 7. Gửi email thông báo (bất đồng bộ)
            sendStatusUpdateEmailAsync(hoaDonDaCapNhat, trangThaiMoi);

            log.info("Cập nhật trạng thái thành công cho hóa đơn: {}", request.getMaHoaDon());

            // 8. Trả về thông tin chi tiết bao gồm thời gian từ lịch sử
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("maHoaDon", hoaDonDaCapNhat.getCode());
            responseData.put("trangThaiCu", trangThaiCu);
            responseData.put("trangThaiMoi", trangThaiMoi);
            responseData.put("ngayCapNhat", System.currentTimeMillis());
            responseData.put("thoiGianCapNhat", lichSu.getThoiGian());
            responseData.put("nhanVien", nhanVien.getName());
            responseData.put("paymentDate", hoaDonDaCapNhat.getPaymentDate());
            responseData.put("ghiChu", lichSu.getNote());

            return new ResponseObject<>(responseData, HttpStatus.OK, "Cập nhật trạng thái thành công");

        } catch (RuntimeException e) {
            log.error("Lỗi cập nhật trạng thái hóa đơn: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không xác định khi cập nhật trạng thái: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage(), e);
        }
    }

    private void doiTrangThaiImel(Invoice hoaDonDaCapNhat) {
    }

    /**
     * Lưu lịch sử thay đổi trạng thái hóa đơn
     */
    private LichSuTrangThaiHoaDon luuLichSuTrangThaiHoaDon(
            Invoice hoaDon,
            EntityTrangThaiHoaDon trangThai,
            String ghiChu,
            Staff nhanVien) {

        LichSuTrangThaiHoaDon lichSu = new LichSuTrangThaiHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(trangThai);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setNote(ghiChu != null ? ghiChu : "Cập nhật từ hệ thống quản trị");
        lichSu.setNhanVien(nhanVien);

        LichSuTrangThaiHoaDon saved = lichSuTrangThaiRepository.save(lichSu);
        log.info("Đã lưu lịch sử trạng thái: {} cho hóa đơn {}", trangThai, hoaDon.getCode());

        return saved;
    }

    /**
     * Lấy danh sách lịch sử trạng thái của hóa đơn
     */
    public List<LichSuTrangThaiHoaDon> getLichSuTrangThaiHoaDon(String maHoaDon) {
        Invoice hoaDon = adHoaDonRepository.findByMa(maHoaDon)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + maHoaDon));

        return lichSuTrangThaiRepository.findByHoaDonOrderByThoiGianDesc(hoaDon);
    }

    /**
     * Lấy thời gian của một trạng thái cụ thể từ lịch sử
     */
    public LocalDateTime getThoiGianTrangThai(String maHoaDon, EntityTrangThaiHoaDon trangThai) {
        Invoice hoaDon = adHoaDonRepository.findByMa(maHoaDon)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + maHoaDon));

        return lichSuTrangThaiRepository
                .findFirstByHoaDonAndTrangThaiOrderByThoiGianDesc(hoaDon, trangThai)
                .map(LichSuTrangThaiHoaDon::getThoiGian)
                .orElse(null);
    }

    /**
     * Lấy thông tin timeline của hóa đơn
     */
    public Map<String, Object> getTimelineHoaDon(String maHoaDon) {
        Invoice hoaDon = adHoaDonRepository.findByMa(maHoaDon)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + maHoaDon));

        List<LichSuTrangThaiHoaDon> lichSu = lichSuTrangThaiRepository
                .findByHoaDonOrderByThoiGianAsc(hoaDon);

        Map<String, Object> timeline = new LinkedHashMap<>();
        for (LichSuTrangThaiHoaDon item : lichSu) {
            Map<String, Object> step = new HashMap<>();
            step.put("trangThai", item.getTrangThai());
            step.put("tenTrangThai", getStatusText(item.getTrangThai()));
            step.put("thoiGian", item.getThoiGian());
            step.put("ghiChu", item.getNote());
            step.put("nhanVien", item.getNhanVien() != null ? item.getNhanVien().getName() : "Hệ thống");

            timeline.put(item.getTrangThai().name(), step);
        }

        return timeline;
    }

    /**
     * Kiểm tra hóa đơn có được phép chuyển sang trạng thái mới hay không
     */
    private void kiemTraChuyenTrangThai(
            EntityTrangThaiHoaDon trangThaiCu,
            EntityTrangThaiHoaDon trangThaiMoi) {

        if (trangThaiCu == trangThaiMoi) {
            throw new RuntimeException("Hóa đơn đã ở trạng thái này");
        }

        if (trangThaiCu == EntityTrangThaiHoaDon.DA_HUY) {
            throw new RuntimeException("Hóa đơn đã bị hủy, không thể cập nhật");
        }

        if (trangThaiCu == EntityTrangThaiHoaDon.HOAN_THANH) {
            throw new RuntimeException("Hóa đơn đã hoàn thành, không thể cập nhật");
        }

        // Kiểm tra luồng hợp lệ
        Map<EntityTrangThaiHoaDon, List<EntityTrangThaiHoaDon>> validTransitions = new HashMap<>();
        validTransitions.put(EntityTrangThaiHoaDon.CHO_XAC_NHAN,
                Arrays.asList(EntityTrangThaiHoaDon.DA_XAC_NHAN, EntityTrangThaiHoaDon.DA_HUY));
        validTransitions.put(EntityTrangThaiHoaDon.DA_XAC_NHAN,
                Arrays.asList(EntityTrangThaiHoaDon.CHO_GIAO, EntityTrangThaiHoaDon.DA_HUY));
        validTransitions.put(EntityTrangThaiHoaDon.CHO_GIAO,
                Arrays.asList(EntityTrangThaiHoaDon.DANG_GIAO, EntityTrangThaiHoaDon.DA_HUY));
        validTransitions.put(EntityTrangThaiHoaDon.DANG_GIAO,
                Arrays.asList(EntityTrangThaiHoaDon.HOAN_THANH));

        List<EntityTrangThaiHoaDon> allowedTransitions = validTransitions.get(trangThaiCu);
        if (allowedTransitions == null || !allowedTransitions.contains(trangThaiMoi)) {
            throw new RuntimeException(String.format(
                    "Không thể chuyển từ trạng thái %s sang %s",
                    getStatusText(trangThaiCu), getStatusText(trangThaiMoi)
            ));
        }
    }

    /**
     * Lấy nhân viên hiện tại từ Security Context
     */
    private Staff getCurrentStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Không xác định được nhân viên thực hiện");
        }

        String username = authentication.getName();
        return staffRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên: " + username));
    }

    /**
     * Khóa IMEI khi hóa đơn chờ xác nhận / đã xác nhận
     */
    private void khoaIMEIKhiChoXacNhan(Invoice hoaDon) {
        List<InvoiceDetail> danhSachChiTiet =
                adHoaDonChiTietRepository.findByInvoiceId(hoaDon.getId());

        int imeiCount = 0;
        for (InvoiceDetail chiTiet : danhSachChiTiet) {
            if (chiTiet.getImeis() != null && !chiTiet.getImeis().isEmpty()) {
                for (IMEI imei : chiTiet.getImeis()) {
                    imei.setImeiStatus(ImeiStatus.RESERVED);
                    imei.setLockedAt(System.currentTimeMillis());
                }
                imeiRepository.saveAll(chiTiet.getImeis());
                imeiCount += chiTiet.getImeis().size();
            }
        }
        log.info("Đã khóa {} IMEI cho hóa đơn {}", imeiCount, hoaDon.getCode());
    }

    /**
     * Đánh dấu IMEI đã bán khi chuyển sang chờ giao
     */
    private void danhDauIMEIDaBan(Invoice hoaDon) {
        List<InvoiceDetail> danhSachChiTiet =
                adHoaDonChiTietRepository.findByInvoiceId(hoaDon.getId());

        int imeiCount = 0;
        for (InvoiceDetail chiTiet : danhSachChiTiet) {
            if (chiTiet.getImeis() != null && !chiTiet.getImeis().isEmpty()) {
                for (IMEI imei : chiTiet.getImeis()) {
                    imei.setImeiStatus(ImeiStatus.SOLD);
                    imei.setSoldAt(System.currentTimeMillis());
                }
                imeiRepository.saveAll(chiTiet.getImeis());
                imeiCount += chiTiet.getImeis().size();
            }
        }
        log.info("Đã đánh dấu {} IMEI là đã bán cho hóa đơn {}", imeiCount, hoaDon.getCode());
    }

    /**
     * Trả toàn bộ IMEI về trạng thái AVAILABLE khi hủy hóa đơn
     */
    private void traIMEIVeKho(Invoice hoaDon) {
        List<InvoiceDetail> danhSachChiTiet =
                adHoaDonChiTietRepository.findByInvoiceId(hoaDon.getId());

        int imeiCount = 0;
        for (InvoiceDetail chiTiet : danhSachChiTiet) {
            if (chiTiet.getImeis() != null && !chiTiet.getImeis().isEmpty()) {
                for (IMEI imei : chiTiet.getImeis()) {
                    imei.setImeiStatus(ImeiStatus.AVAILABLE);
                    imei.setInvoiceDetail(null);
                    imei.setInvoiceHolding(null);
                    imei.setLockedAt(null);
                }
                imeiRepository.saveAll(chiTiet.getImeis());
                imeiCount += chiTiet.getImeis().size();
            }
        }
        log.info("Đã trả {} IMEI về kho cho hóa đơn {}", imeiCount, hoaDon.getCode());
    }

    /**
     * Đánh dấu voucher đã được sử dụng khi chuyển sang CHỜ GIAO
     */
    private void danhDauVoucherDaSuDung(Invoice hoaDon) {
        Voucher voucher = hoaDon.getVoucher();
        if (voucher == null) {
            return;
        }

        // Tìm voucher detail cho hóa đơn này
        VoucherDetail voucherDetail = voucherDetailRepository.findByInvoiceId(hoaDon.getId());
        if (voucherDetail != null && !Boolean.TRUE.equals(voucherDetail.getUsageStatus())) {
            // Đánh dấu đã sử dụng
            voucherDetail.markAsUsed(hoaDon.getId());
            voucherDetailRepository.save(voucherDetail);

            // Giảm số lượng remaining quantity của voucher
            if (voucher.getRemainingQuantity() != null && voucher.getRemainingQuantity() > 0) {
                voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
                voucherRepository.save(voucher);
            }

            log.info("Đã đánh dấu voucher {} đã sử dụng cho hóa đơn {}", voucher.getCode(), hoaDon.getCode());
        }
    }

    /**
     * Hoàn trả voucher khi hủy hóa đơn
     */
    private void hoanTraVoucher(Invoice hoaDon) {
        Voucher voucher = hoaDon.getVoucher();
        if (voucher == null) {
            return;
        }

        // Tìm voucher detail của hóa đơn
        VoucherDetail voucherDetail = voucherDetailRepository.findByInvoiceId(hoaDon.getId());
        if (voucherDetail != null && Boolean.TRUE.equals(voucherDetail.getUsageStatus())) {
            // Reset trạng thái voucher detail
            voucherDetail.setUsageStatus(false);
            voucherDetail.setUsedDate(null);
            voucherDetail.setInvoiceId(null);
            voucherDetailRepository.save(voucherDetail);

            // Tăng lại remaining quantity của voucher
            if (voucher.getRemainingQuantity() != null) {
                voucher.setRemainingQuantity(voucher.getRemainingQuantity() + 1);
                voucherRepository.save(voucher);
            }

            // Nếu là voucher cá nhân, cập nhật lại customer
            if (voucher.getTargetType() == TargetType.INDIVIDUAL) {
                Customer customer = hoaDon.getCustomer();
                if (customer != null && voucherDetail.getCustomer() == null) {
                    voucherDetail.setCustomer(customer);
                    voucherDetailRepository.save(voucherDetail);
                }
            }

            log.info("Đã hoàn trả voucher {} cho hóa đơn {}", voucher.getCode(), hoaDon.getCode());
        }
    }

    /**
     * Lưu lịch sử thanh toán khi chuyển sang chờ giao
     */
    private void luuLichSuThanhToan(Invoice hoaDon, Staff nhanVien) {
        if (hoaDon.getPaymentDate() != null) {
            return; // Đã thanh toán rồi
        }

        LichSuThanhToan thanhToan = new LichSuThanhToan();
        thanhToan.setHoaDon(hoaDon);
        thanhToan.setSoTien(hoaDon.getTotalAmountAfterDecrease() != null ?
                hoaDon.getTotalAmountAfterDecrease() : hoaDon.getTotalAmount());
        thanhToan.setThoiGian(LocalDateTime.now());
        thanhToan.setLoaiGiaoDich("THANH_TOAN");
        thanhToan.setMaGiaoDich("PAY-" + hoaDon.getCode() + "-" + System.currentTimeMillis());
        thanhToan.setNhanVien(nhanVien);
        thanhToan.setGhiChu("Thanh toán khi xác nhận đơn hàng");

        lichSuThanhToanRepository.save(thanhToan);
        hoaDon.setPaymentDate(System.currentTimeMillis());
        adHoaDonRepository.save(hoaDon);

        log.info("Đã lưu lịch sử thanh toán cho hóa đơn {}", hoaDon.getCode());
    }

    /**
     * Hoàn tiền khi hủy hóa đơn đã thanh toán
     */
    private void hoanTienNeuCan(Invoice hoaDon, Staff nhanVien) {
        if (hoaDon.getPaymentDate() == null) {
            return; // Chưa thanh toán thì không hoàn
        }

        LichSuThanhToan hoanTien = new LichSuThanhToan();
        hoanTien.setHoaDon(hoaDon);
        hoanTien.setSoTien(hoaDon.getTotalAmountAfterDecrease() != null ?
                hoaDon.getTotalAmountAfterDecrease().negate() :
                hoaDon.getTotalAmount().negate());
        hoanTien.setThoiGian(LocalDateTime.now());
        hoanTien.setLoaiGiaoDich("HOAN_TIEN");
        hoanTien.setMaGiaoDich("REFUND-" + hoaDon.getCode() + "-" + System.currentTimeMillis());
        hoanTien.setNhanVien(nhanVien);
        hoanTien.setGhiChu("Hoàn tiền do hủy hóa đơn");

        lichSuThanhToanRepository.save(hoanTien);
        hoaDon.setPaymentDate(null); // Reset payment date
        adHoaDonRepository.save(hoaDon);

        log.info("Đã hoàn tiền cho hóa đơn {}", hoaDon.getCode());
    }

    /**
     * Gửi email thông báo thay đổi trạng thái (bất đồng bộ)
     */
    @Async
    public void sendStatusUpdateEmailAsync(Invoice invoice, EntityTrangThaiHoaDon newStatus) {
        try {
            Customer customer = invoice.getCustomer();
            if (customer == null || customer.getEmail() == null) {
                log.warn("Không có email khách hàng, bỏ qua gửi email cho hóa đơn {}", invoice.getCode());
                return;
            }

            String email = customer.getEmail();
            String subject = getEmailSubject(newStatus);
            String content = buildEmailContent(invoice, newStatus);

            EmailService.sendEmail(email, subject, content);
            log.info("Đã gửi email thông báo đến: {} cho hóa đơn {}", email, invoice.getCode());

        } catch (Exception e) {
            log.error("Lỗi khi gửi email cho hóa đơn {}: {}", invoice.getCode(), e.getMessage(), e);
        }
    }

    private String getEmailSubject(EntityTrangThaiHoaDon status) {
        switch (status) {
            case DA_XAC_NHAN:
                return "Xác nhận đơn hàng tại MyLaptop";
            case DANG_GIAO:
                return "Đơn hàng của bạn đang được giao";
            case HOAN_THANH:
                return "Đơn hàng đã giao thành công";
            case DA_HUY:
                return "Thông báo hủy đơn hàng";
            case CHO_GIAO:
                return "Đơn hàng đã sẵn sàng để giao";
            default:
                return "Cập nhật trạng thái đơn hàng tại MyLaptop";
        }
    }

    private String buildEmailContent(Invoice invoice, EntityTrangThaiHoaDon newStatus) {
        String trackingUrl = "http://localhost:6788/hoa-don/" + invoice.getCode();
        String statusText = getStatusText(newStatus);
        String customerName = invoice.getCustomer() != null ?
                invoice.getCustomer().getName() :
                invoice.getNameReceiver();

        return """
        <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden; background: #f9f9f9;">
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px 20px; text-align: center; color: white;">
                <h2 style="margin: 0; font-size: 28px;">MyLaptop Store</h2>
                <p style="margin: 8px 0 0; font-size: 16px; opacity: 0.9;">Thông báo cập nhật đơn hàng</p>
            </div>
            
            <div style="padding: 30px; background: white;">
                <h3 style="color: #333; margin-top: 0; font-size: 20px;">${statusText}</h3>
                <p style="color: #555; font-size: 16px; line-height: 1.6;">
                    Xin chào <b>${customerName}</b>,<br>
                    Đơn hàng <b style="color: #667eea;">${invoiceCode}</b> của bạn đã được cập nhật trạng thái.
                </p>
                
                <div style="background: #f8f9fa; border-left: 4px solid #667eea; padding: 15px; margin: 20px 0; border-radius: 4px;">
                    <p style="margin: 0; color: #333;">
                        <strong>Mã đơn hàng:</strong> ${invoiceCode}<br>
                        <strong>Trạng thái mới:</strong> ${statusText}<br>
                        <strong>Ngày cập nhật:</strong> ${currentDate}
                    </p>
                </div>
                
                <div style="text-align: center; margin: 30px 0;">
                    <a href="${trackingUrl}" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                       color: white; text-decoration: none; padding: 14px 28px; border-radius: 8px; 
                       font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);">
                        Theo dõi đơn hàng
                    </a>
                </div>
                
                <div style="border-top: 1px solid #eee; padding-top: 20px; margin-top: 20px;">
                    <p style="color: #777; font-size: 14px; text-align: center;">
                        Cảm ơn bạn đã mua hàng tại MyLaptop!<br>
                        Mọi thắc mắc vui lòng liên hệ: 1800-xxxx (Miễn phí)
                    </p>
                </div>
            </div>
            
            <div style="background: #f5f5f5; padding: 20px; text-align: center; border-top: 1px solid #e0e0e0;">
                <p style="margin: 0; color: #666; font-size: 12px;">
                    © 2024 MyLaptop Store. All rights reserved.<br>
                    Địa chỉ: 123 Nguyễn Văn Linh, Quận 7, TP.HCM
                </p>
            </div>
        </div>
        """
                .replace("${statusText}", statusText)
                .replace("${customerName}", customerName != null ? customerName : "Quý khách")
                .replace("${invoiceCode}", invoice.getCode())
                .replace("${currentDate}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .replace("${trackingUrl}", trackingUrl);
    }

    private String getStatusText(EntityTrangThaiHoaDon status) {
        switch (status) {
            case CHO_XAC_NHAN: return "Chờ xác nhận";
            case DA_XAC_NHAN: return "Đã xác nhận";
            case CHO_GIAO: return "Chờ giao hàng";
            case DANG_GIAO: return "Đang giao hàng";
            case HOAN_THANH: return "Hoàn thành";
            case DA_HUY: return "Đã hủy";
            default: return "Đang xử lý";
        }
    }

    // Các phương thức khác giữ nguyên
    @Override
    public ResponseObject<?> getAllHoaDon(ADHoaDonSearchRequest request) {
        try {
            Pageable pageable = Helper.createPageable(request, "created_date");
            HoaDonPageResponse result = adHoaDonRepository.getAllHoaDonResponse(request, pageable);

            return new ResponseObject<>(
                    result,
                    HttpStatus.OK,
                    "Lấy danh sách hóa đơn thành công"
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách hóa đơn: {}", e.getMessage(), e);
            return new ResponseObject<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy danh sách hóa đơn: " + e.getMessage()
            );
        }
    }

    @Override
    public ResponseObject<?> getAllHoaDonCT(ADHoaDonDetailRequest request) {
        try {
            Pageable pageable = Helper.createPageable(request, "created_date");



            Page<ADHoaDonChiTietResponseDetail> page = adHoaDonChiTietRepository.getHoaDonChiTiet(request.getMaHoaDon(), pageable);
            return new ResponseObject<>(
                    page,
                    HttpStatus.OK,
                    "Lấy danh sách chi tiết hóa đơn thành công"
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết hóa đơn: {}", e.getMessage(), e);
            return new ResponseObject<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy chi tiết hóa đơn: " + e.getMessage()
            );
        }
    }
}