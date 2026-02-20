package com.sd20201.datn.core.admin.banhang.service.impl;


import com.sd20201.datn.core.admin.shift.repository.AdShiftHandoverRepository;
import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.core.admin.banhang.model.request.*;
import com.sd20201.datn.core.admin.banhang.model.request.ChonPhieuGiamGiaRequest;
import com.sd20201.datn.core.admin.banhang.model.response.*;
import com.sd20201.datn.core.admin.banhang.repository.*;
import com.sd20201.datn.core.admin.banhang.service.ADBanHangService;
import com.sd20201.datn.core.admin.customer.repository.AdCustomerRepository;
import com.sd20201.datn.core.admin.hoadon.repository.ADHoaDonChiTietRepository;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductDetailRepository;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.admin.voucher.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.*;
import com.sd20201.datn.infrastructure.constant.*;
import com.sd20201.datn.infrastructure.exception.BusinessException;
import com.sd20201.datn.repository.*;
import com.sd20201.datn.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ADBanHangServiceImpl implements ADBanHangService {

    private final ADTaoHoaDonRepository adTaoHoaDonRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final ADTaoHoaDonChiTietRepository adTaoHoaDonChiTietRepository;
    private final ADPDProductDetailRepository adSanPhamRepository;
    private final ADBanHangIMEIRepository imeiRepository;
    private final ADStaffRepository adNhanVienRepository;
    public final AdVoucherRepository adVoucherRepository;
    public final ADPDProductDetailRepository adPDProductDetailRepository;
    public final LichSuThanhToanRepository adLichSuThanhToanRepository;
    public final ADBanHangSanPhamChiTiet productDetailRepository;

    public final ADBHVoucherDetailRepository adbhvoucher;
    public final AdCustomerRepository khachHangRepository;

    public final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    public final AdVoucherRepository phieuGiamGiaRepository;
    public final ADHoaDonChiTietRepository adHoaDonChiTietRepository;
    private final AdShiftHandoverRepository adShiftHandoverRepository;

    private final ADBHScreenRepository screenRepository;
    private final ADBHBrandRepository brandRepository;
    private final ADBHOperatingSystemRepository operatingSystemRepository;
    private final ADBHBatteryRepository batteryRepository;
    private final ADBHRAMRepository ramRepository;
    private final ADBHMaterialRepository materialRepository;
    private final ADBHCPURepository cpuRepository;
    private final ADBHGPURepository gpuRepository;
    private final ADBHHardDriveRepository hardDriveRepository;
    private final ADBHColorRepository colorRepository;


    @Override
    public List<ListHoaDon> getHoaDon() {
        return adTaoHoaDonRepository.getAll();
    }

    @Override
    public ResponseObject<?> themMoiKhachHang(ADThemMoiKhachHangRequest request) {

        List<Customer> existing = khachHangRepository.findByExactPhone(request.getSdt());

        if (!existing.isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.CONFLICT,
                    "Khách hàng đã tồn tại trong hệ thống"
            );
        }

        Customer them = new Customer();
        them.setName(request.getTen());
        them.setPhone(request.getSdt());

        khachHangRepository.save(them);

        return new ResponseObject<>(
                them.getId(),
                HttpStatus.CREATED,
                "Thêm mới khách hàng thành công"
        );
    }


    @Override
    public ResponseObject<?> getProductDetails(ADPDProductDetailRequest request) {
        Long currentTime = System.currentTimeMillis();
        return ResponseObject.successForward(
                PageableObject.of(
                        productDetailRepository.getProductDetails(
                                Helper.createPageable(request),
                                request,
                                currentTime)),
                "OKE"
        );
    }

    @Override
    @Transactional
    public ResponseObject<?> createHoaDon(ADNhanVienRequest adNhanVienRequest) {
        try {
            Invoice hoaDon = new Invoice();
            hoaDon.setTotalAmount(BigDecimal.ZERO);
            hoaDon.setTotalAmountAfterDecrease(BigDecimal.ZERO);

            Staff nhanVien = adNhanVienRepository
                    .findById(adNhanVienRequest.getIdNV())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với id = " + adNhanVienRequest.getIdNV()));

            hoaDon.setCode(adNhanVienRequest.getMa());
            hoaDon.setStaff(nhanVien);
            hoaDon.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
            hoaDon.setTypeInvoice(TypeInvoice.TAI_QUAY);
            hoaDon.setCreatedDate(System.currentTimeMillis());
            hoaDon.setPaymentDate(null); // Chưa thanh toán

            Invoice savedInvoice = adTaoHoaDonRepository.save(hoaDon);

            // Tạo lịch sử trạng thái
            createStatusHistory(savedInvoice, EntityTrangThaiHoaDon.CHO_XAC_NHAN,
                    "Đơn hàng đã được tạo và đang chờ xử lý.", nhanVien);

            return new ResponseObject<>(savedInvoice, HttpStatus.CREATED, "Tạo hóa đơn thành công");

        } catch (Exception e) {
            log.error("Error creating invoice: ", e);
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo hóa đơn: " + e.getMessage());
        }
    }
    @Override
    public List<ADGioHangResponse> getListGioHang(String id) {
        try {
            Long currentTime = System.currentTimeMillis();
            return adTaoHoaDonChiTietRepository.getAllGioHang(id,currentTime);
        } catch (Exception e) {
            log.error("Error getting cart for invoice {}: ", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public ResponseObject<?> thanhToanThanhCong(ADThanhToanRequest id) throws BadRequestException {
        System.out.println("=== BẮT ĐẦU XỬ LÝ THANH TOÁN ===");
        System.out.println("ID hóa đơn: " + id.getIdHD());
        System.out.println("ID nhân viên: " + id.getIdNV());

        // ========== VALIDATE INPUT ==========
        if (id.getIdHD() == null || id.getIdHD().trim().isEmpty()) {
            throw new BadRequestException("ID hóa đơn không được để trống");
        }

        if (id.getIdNV() == null || id.getIdNV().trim().isEmpty()) {
            throw new BadRequestException("ID nhân viên không được để trống");
        }

        // Kiểm tra hóa đơn tồn tại
        Invoice hoaDon = adTaoHoaDonRepository.findById(id.getIdHD())
                .orElseThrow(() -> new BadRequestException("Hóa đơn không tồn tại với ID: " + id.getIdHD()));

        System.out.println("Trạng thái hóa đơn hiện tại: " + hoaDon.getEntityTrangThaiHoaDon());
        System.out.println("Loại hóa đơn: " + hoaDon.getTypeInvoice());
        System.out.println("Ngày tạo hóa đơn: " + hoaDon.getCreatedDate());
        System.out.println("Ngày thanh toán hiện tại: " + hoaDon.getPaymentDate());

        // ========== XỬ LÝ VOUCHER ==========
        processVoucher(id, hoaDon);

        // ========== KIỂM TRA SẢN PHẨM ==========
        validateProducts(id);

        // ========== XỬ LÝ THANH TOÁN ==========
        try {
            // Lấy thông tin nhân viên
            Staff nhanVien = adNhanVienRepository.findById(id.getIdNV())
                    .orElseThrow(() -> new BadRequestException("Nhân viên không tồn tại"));

            if (hoaDon.getShiftHandover() == null && nhanVien.getAccount() != null) {
                try {
                    String accountId = nhanVien.getAccount().getId();
                    // Tìm ca đang mở (ACTIVE) của nhân viên này
                    ShiftHandover openShift = adShiftHandoverRepository.findOpenShiftByAccountId(accountId).orElse(null);

                    if (openShift != null) {
                        hoaDon.setShiftHandover(openShift);
                        // System.out.println("DEBUG: Đã gán hóa đơn vào ca: " + openShift.getId());
                    }
                } catch (Exception e) {
                    log.error("Lỗi gán ca làm việc: ", e);
                }
            }
            // Xác định phương thức thanh toán
            TypePayment phuongThucThanhToan = determinePaymentMethod(id.getPhuongThucThanhToan());

            // Cập nhật thông tin hóa đơn
            updateInvoiceInfo(hoaDon, id, phuongThucThanhToan);

            // Xử lý theo loại hóa đơn
            if (hoaDon.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
                return processDeliveryInvoice(hoaDon, id, nhanVien, phuongThucThanhToan);
            } else {
                return processCounterInvoice(hoaDon, id, nhanVien, phuongThucThanhToan);
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi xử lý thanh toán: " + e.getMessage());
        }
    }

    private void processVoucher(ADThanhToanRequest id, Invoice hoaDon) throws BadRequestException {
        if (id.getIdPGG() != null && !id.getIdPGG().trim().isEmpty()) {
            try {
                Voucher phieuGiamGia1 = adVoucherRepository.findById(id.getIdPGG())
                        .orElseThrow(() -> new BadRequestException("Voucher không tồn tại"));

                if (phieuGiamGia1.getRemainingQuantity() <= 0) {
                    throw new BadRequestException("Voucher đã hết số lượng");
                }

                // Tạo request để kiểm tra voucher
                ChonPhieuGiamGiaRequest chonPhieuGiamGiaRequest = new ChonPhieuGiamGiaRequest();
                chonPhieuGiamGiaRequest.setTongTien(id.getTienHang());

                if (hoaDon.getCustomer() != null && hoaDon.getCustomer().getId() != null) {
                    chonPhieuGiamGiaRequest.setIdKH(hoaDon.getCustomer().getId());
                }

                // Kiểm tra xem có voucher tốt hơn không (chỉ khi check = 1)
                if (id.getCheck() != null && id.getCheck() == 1) {
                    try {
                        List<Voucher> list = danhSachPhieuGiamGia1(chonPhieuGiamGiaRequest);

                        if (!list.isEmpty() && list.get(0) != null && list.get(0).getGiaTriGiamThucTe() != null
                                && phieuGiamGia1.getGiaTriGiamThucTe() != null
                                && list.get(0).getGiaTriGiamThucTe().compareTo(phieuGiamGia1.getGiaTriGiamThucTe()) > 0) {
                            throw new BadRequestException("Đã có 1 phiếu giảm giá tốt hơn");
                        }
                    } catch (Exception e) {
                        System.out.println("Lỗi khi kiểm tra voucher tốt hơn: " + e.getMessage());
                    }
                }

            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("Lỗi khi xử lý voucher: " + e.getMessage());
                id.setIdPGG(null);
                id.setCheck(0);
            }
        }
    }

    private void validateProducts(ADThanhToanRequest id) throws BadRequestException {
        try {
            List<String> idHDCTS = adTaoHoaDonChiTietRepository.getHoaDonChiTiet(id.getIdHD());

            if (idHDCTS == null || idHDCTS.isEmpty()) {
                throw new BadRequestException("Hóa đơn không có sản phẩm nào");
            }

            for (String idHDCT : idHDCTS) {
                if (idHDCT == null || idHDCT.trim().isEmpty()) {
                    continue;
                }

                InvoiceDetail hoaDonChiTiet = adTaoHoaDonChiTietRepository.findById(idHDCT)
                        .orElseThrow(() -> new BadRequestException("Chi tiết hóa đơn không tồn tại"));

                String idSPCT = adTaoHoaDonChiTietRepository.getSanPhamChiTiet(idHDCT);
                if (idSPCT == null || idSPCT.trim().isEmpty()) {
                    throw new BadRequestException("Không tìm thấy sản phẩm cho chi tiết hóa đơn");
                }

                ProductDetail sanPhamChiTiet = adPDProductDetailRepository.findById(idSPCT)
                        .orElseThrow(() -> new BadRequestException("Sản phẩm không tồn tại"));
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi kiểm tra sản phẩm: " + e.getMessage());
        }
    }

    // Helper methods
    private TypePayment determinePaymentMethod(String paymentMethod) {
        if (paymentMethod == null) {
            return TypePayment.TIEN_MAT;
        }

        switch (paymentMethod) {
            case "0":
                return TypePayment.TIEN_MAT;
            case "1":
                return TypePayment.CHUYEN_KHOAN;
            default:
                return TypePayment.TIEN_MAT_CHUYEN_KHOAN;
        }
    }

    private void updateInvoiceInfo(Invoice invoice, ADThanhToanRequest request, TypePayment paymentMethod) {
        System.out.println("=== CẬP NHẬT THÔNG TIN HÓA ĐƠN ===");
        System.out.println("Loại hóa đơn: " + invoice.getTypeInvoice());

        // Chỉ cập nhật thông tin nhận hàng nếu là giao hàng
        if (invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
            System.out.println("Đây là hóa đơn GIAO HÀNG, cập nhật thông tin nhận hàng");

            if (request.getTen() != null && !request.getTen().trim().isEmpty()) {
                invoice.setNameReceiver(request.getTen());
                System.out.println("Đã cập nhật tên người nhận: " + request.getTen());
            }
            if (request.getSdt() != null && !request.getSdt().trim().isEmpty()) {
                invoice.setPhoneReceiver(request.getSdt());
                System.out.println("Đã cập nhật SĐT người nhận: " + request.getSdt());
            }
            if (request.getDiaChi() != null && !request.getDiaChi().trim().isEmpty()) {
                invoice.setAddressReceiver(request.getDiaChi());
                System.out.println("Đã cập nhật địa chỉ: " + request.getDiaChi());
            }
        } else {
            System.out.println("Đây là hóa đơn TẠI QUẦY, không cập nhật thông tin giao hàng");
        }

        // Cập nhật thông tin tài chính
        invoice.setShippingFee(request.getTienShip() != null ? request.getTienShip() : BigDecimal.ZERO);
        invoice.setTotalAmount(request.getTienHang() != null ? request.getTienHang() : BigDecimal.ZERO);
        invoice.setTotalAmountAfterDecrease(request.getTongTien() != null ? request.getTongTien() : BigDecimal.ZERO);

        System.out.println("Tổng tiền: " + invoice.getTotalAmount());
        System.out.println("Tổng tiền sau giảm: " + invoice.getTotalAmountAfterDecrease());
        System.out.println("Phí vận chuyển: " + invoice.getShippingFee());
    }

    private ResponseObject<?> processDeliveryInvoice(Invoice invoice, ADThanhToanRequest request,
                                                     Staff staff, TypePayment paymentMethod) {
        System.out.println("=== XỬ LÝ HÓA ĐƠN GIAO HÀNG ===");

        try {
            // Đặt trạng thái là ĐÃ XÁC NHẬN cho hóa đơn giao hàng
            invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.DA_XAC_NHAN);

            // Set thời gian thanh toán
            Long paymentTime = System.currentTimeMillis();
            invoice.setPaymentDate(paymentTime);
            System.out.println("Đã set ngày thanh toán: " + paymentTime);

            // Xử lý voucher nếu có
            processVoucherIfExists(invoice, request);

            // Lưu hóa đơn
            Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);

            // Tạo lịch sử TRẠNG THÁI
            createStatusHistory(savedInvoice, EntityTrangThaiHoaDon.DA_XAC_NHAN,
                    "Hóa đơn giao hàng đã được xác nhận và chờ giao cho đơn vị vận chuyển.", staff);

            // Tạo lịch sử THANH TOÁN (luôn tạo dù là tiền mặt hay chuyển khoản)
            createPaymentHistory(savedInvoice, request, staff, paymentMethod);

            // Cập nhật trạng thái IMEI
            updateImeiStatusForInvoice(savedInvoice, ImeiStatus.SOLD);

            return buildSuccessResponse(savedInvoice, "Thanh toán hóa đơn giao hàng thành công");

        } catch (Exception e) {
            log.error("Error processing delivery invoice: ", e);
            throw new BusinessException("Lỗi khi xử lý hóa đơn giao hàng: " + e.getMessage());
        }
    }

    private ResponseObject<?> processCounterInvoice(Invoice invoice, ADThanhToanRequest request,
                                                    Staff staff, TypePayment paymentMethod) {
        System.out.println("=== XỬ LÝ HÓA ĐƠN TẠI QUẦY ===");

        try {
            // Đặt trạng thái là HOÀN THÀNH cho hóa đơn tại quầy
            invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.HOAN_THANH);

            // Set thời gian thanh toán
            Long paymentTime = System.currentTimeMillis();
            invoice.setPaymentDate(paymentTime);
            System.out.println("Đã set ngày thanh toán: " + paymentTime);

            // Xử lý voucher nếu có
            processVoucherIfExists(invoice, request);

            // Lưu hóa đơn
            Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);

            // Tạo lịch sử TRẠNG THÁI
            createStatusHistory(savedInvoice, EntityTrangThaiHoaDon.HOAN_THANH,
                    "Hóa đơn đã được thanh toán thành công tại quầy.", staff);

            // Tạo lịch sử THANH TOÁN
            createPaymentHistory(savedInvoice, request, staff, paymentMethod);

            // Cập nhật trạng thái IMEI
            updateImeiStatusForInvoice(savedInvoice, ImeiStatus.SOLD);

            return buildSuccessResponse(savedInvoice, "Thanh toán hóa đơn tại quầy thành công");

        } catch (Exception e) {
            log.error("Error processing counter invoice: ", e);
            throw new BusinessException("Lỗi khi xử lý hóa đơn tại quầy: " + e.getMessage());
        }
    }

    private void updateImeiStatusForInvoice(Invoice invoice, ImeiStatus status) {
        try {
            List<InvoiceDetail> invoiceDetails = adHoaDonChiTietRepository.findByInvoiceId(invoice.getId());

            for (InvoiceDetail detail : invoiceDetails) {
                if (detail.getImeis() != null && !detail.getImeis().isEmpty()) {
                    for (IMEI imei : detail.getImeis()) {
                        imei.setImeiStatus(status);
                        if (status == ImeiStatus.SOLD) {
                            imei.setSoldAt(System.currentTimeMillis());
                        }
                    }
                    imeiRepository.saveAll(detail.getImeis());
                }
            }
            System.out.println("Đã cập nhật trạng thái IMEI sang: " + status);
        } catch (Exception e) {
            log.error("Error updating IMEI status: ", e);
        }
    }

    private void processVoucherIfExists(Invoice invoice, ADThanhToanRequest request) {
        if (request.getIdPGG() != null && !request.getIdPGG().trim().isEmpty()) {
            try {
                System.out.println("=== XỬ LÝ VOUCHER ===");
                System.out.println("Voucher ID: " + request.getIdPGG());

                Voucher voucher = adVoucherRepository.findById(request.getIdPGG())
                        .orElse(null);

                if (voucher != null && voucher.getRemainingQuantity() > 0) {
                    // Trừ số lượng tồn kho voucher chung
                    voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
                    adVoucherRepository.save(voucher);
                    System.out.println("Đã trừ số lượng voucher, còn lại: " + voucher.getRemainingQuantity());

                    // Gán voucher vào hóa đơn
                    invoice.setVoucher(voucher);

                    // Cập nhật trạng thái Voucher Detail
                    if (invoice.getCustomer() != null) {
                        String customerId = invoice.getCustomer().getId();
                        String voucherId = voucher.getId();

                        Optional<VoucherDetail> voucherDetailOpt =
                                adbhvoucher.findByVoucherIdAndCustomerId(voucherId, customerId);

                        if (voucherDetailOpt.isPresent()) {
                            VoucherDetail detail = voucherDetailOpt.get();
                            detail.markAsUsed(invoice.getId());
                            adbhvoucher.save(detail);
                            System.out.println("Đã cập nhật trạng thái đã sử dụng cho khách hàng: " + customerId);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi khi xử lý voucher: ", e);
            }
        }
    }

    private void createStatusHistory(Invoice invoice, EntityTrangThaiHoaDon status,
                                     String note, Staff staff) {
        try {
            LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
            history.setHoaDon(invoice);
            history.setTrangThai(status);
            history.setNote(note);
            history.setThoiGian(LocalDateTime.now());
            history.setNhanVien(staff);

            lichSuTrangThaiHoaDonRepository.save(history);
            System.out.println("Đã tạo lịch sử trạng thái: " + status + " - " + note);
        } catch (Exception e) {
            log.error("Error creating status history: ", e);
        }
    }

    private void createPaymentHistory(Invoice invoice, ADThanhToanRequest request,
                                      Staff staff, TypePayment paymentMethod) {
        try {
            System.out.println("=== TẠO LỊCH SỬ THANH TOÁN ===");

            LichSuThanhToan paymentHistory = new LichSuThanhToan();
            paymentHistory.setHoaDon(invoice);
            paymentHistory.setSoTien(request.getTongTien() != null ? request.getTongTien() : BigDecimal.ZERO);
            paymentHistory.setLoaiGiaoDich(paymentMethod != null ? paymentMethod.toString() : TypePayment.TIEN_MAT.toString());
            paymentHistory.setThoiGian(LocalDateTime.now());
            paymentHistory.setNhanVien(staff);
            paymentHistory.setMaGiaoDich("GD_" + invoice.getCode() + "_" + System.currentTimeMillis());
            paymentHistory.setGhiChu("Thanh toán hóa đơn #" + invoice.getCode());
            paymentHistory.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);

            adLichSuThanhToanRepository.save(paymentHistory);
            System.out.println("Đã tạo lịch sử thanh toán với mã: " + paymentHistory.getMaGiaoDich());
            System.out.println("Số tiền: " + paymentHistory.getSoTien());
            System.out.println("Loại giao dịch: " + paymentHistory.getLoaiGiaoDich());
            System.out.println("Thời gian: " + paymentHistory.getThoiGian());

        } catch (Exception e) {
            System.out.println("Lỗi khi tạo lịch sử thanh toán: " + e.getMessage());
            e.printStackTrace();
            // Không throw exception vì đây không phải là lỗi nghiêm trọng
        }
    }

    private ResponseObject<?> buildSuccessResponse(Invoice invoice, String message) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("id", invoice.getId());
        responseData.put("maHoaDon", invoice.getCode());
        responseData.put("trangThai", invoice.getEntityTrangThaiHoaDon().toString());
        responseData.put("ngayTao", invoice.getCreatedDate());
        responseData.put("ngayThanhToan", invoice.getPaymentDate());
        responseData.put("tongTien", invoice.getTotalAmountAfterDecrease());
        responseData.put("loaiHoaDon", invoice.getTypeInvoice().toString());

        System.out.println("=== THÔNG TIN HÓA ĐƠN SAU KHI XỬ LÝ ===");
        System.out.println("Mã hóa đơn: " + invoice.getCode());
        System.out.println("Trạng thái: " + invoice.getEntityTrangThaiHoaDon());
        System.out.println("Ngày tạo: " + invoice.getCreatedDate());
        System.out.println("Ngày thanh toán: " + invoice.getPaymentDate());
        System.out.println("Loại hóa đơn: " + invoice.getTypeInvoice());
        System.out.println("Tổng tiền: " + invoice.getTotalAmountAfterDecrease());

        return new ResponseObject<>(responseData, HttpStatus.OK, message);
    }

    // Các phương thức khác giữ nguyên...

    @Override
    public ResponseObject<?> giaoHang(String request) {
        System.out.println("=== CHUYỂN ĐỔI LOẠI HÓA ĐƠN ===");
        System.out.println("ID hóa đơn: " + request);

        Invoice hoaDon = adTaoHoaDonRepository.findById(request)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn"));

        System.out.println("Loại hóa đơn hiện tại: " + hoaDon.getTypeInvoice());

        // Chuyển đổi loại hóa đơn
        if (hoaDon.getTypeInvoice() == TypeInvoice.TAI_QUAY) {
            hoaDon.setTypeInvoice(TypeInvoice.GIAO_HANG);
            System.out.println("Đã chuyển sang GIAO HÀNG");
        } else {
            hoaDon.setTypeInvoice(TypeInvoice.TAI_QUAY);
            System.out.println("Đã chuyển sang TẠI QUẦY");
        }

        adTaoHoaDonRepository.save(hoaDon);

        return new ResponseObject<>(hoaDon, HttpStatus.OK,
                "Đã chuyển loại hóa đơn thành " + hoaDon.getTypeInvoice());
    }


    @Override
    public ResponseObject<?> listKhachHang(ListKhachHangRequest listKhachHangRequest) {
        try {
            Pageable pageable = Helper.createPageable(listKhachHangRequest, "createdDate");
            Page<ADChonKhachHangResponse> page = adTaoHoaDonChiTietRepository.getAllList(listKhachHangRequest, pageable);
            return new ResponseObject<>(PageableObject.of(page), HttpStatus.OK, "Lấy danh sách khách hàng thành công");
        } catch (Exception e) {
            log.error("Error getting customer list: ", e);
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi lấy danh sách khách hàng");
        }
    }

    @Override
    public ADChonKhachHangResponse getKhachHang(String id) {
        try {
            return adTaoHoaDonChiTietRepository.getKhachHang(id);
        } catch (Exception e) {
            log.error("Error getting customer with id {}: ", id, e);
            return null;
        }
    }


    @Override
    public List<ADPhuongThucThanhToanRespones> getPhuongThucThanhToan(String id) {
        try {
            return adTaoHoaDonChiTietRepository.getPhuongThucThanhToan(id);
        } catch (Exception e) {
            log.error("Error getting payment methods for invoice {}: ", id, e);
            return Collections.emptyList();
        }
    }


    @Override
    @Transactional
    public ResponseObject<?> createThemSanPham(ADThemSanPhamRequest request) {

        System.out.println(request.getInvoiceId());
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new BusinessException("Khong tim thay hoa don"));

        ProductDetail productDetail = productDetailRepository
                .findById(request.getProductDetailId())
                .orElseThrow(() -> new BusinessException("Khong tim thay san pham"));

        List<IMEI> imeis = imeiRepository.findAllById(request.getImeiIds());

        if(imeis.isEmpty()) {
            throw new BusinessException("Khong tim thay san pham");
        }

        if (imeis.size() != request.getImeiIds().size()) {
            throw new BusinessException("Khong tim thay san pham");
        }

        boolean invalidImei = imeis.stream().anyMatch(
                imei -> imei.getImeiStatus() != ImeiStatus.AVAILABLE ||
                        !imei.getProductDetail().getId().equals(productDetail.getId())
        );

        if (invalidImei) {
            throw new BusinessException("Imei da duoc dat da ban hoac khong co");
        }

        InvoiceDetail invoiceDetail = new InvoiceDetail();
        invoiceDetail.setInvoice(invoice);
        invoiceDetail.setProductDetail(productDetail);
        invoiceDetail.setPrice(productDetail.getPrice());
        invoiceDetail.setQuantity(imeis.size());

        BigDecimal totalAmount = productDetail.getPrice()
                .multiply(BigDecimal.valueOf(imeis.size()));

        invoiceDetail.setTotalAmount(totalAmount);
        invoiceDetailRepository.save(invoiceDetail);

        for (IMEI imei : imeis) {
            imei.setInvoiceDetail(invoiceDetail);
            imei.setImeiStatus(ImeiStatus.RESERVED);
        }

        imeiRepository.saveAll(imeis);


        return new ResponseObject<>(null, HttpStatus.OK, "thêm sản phẩm thanh công");


    }

    @Override
    public void themKhachHang(ADThemKhachHangRequest id) {

        Customer khachHang = khachHangRepository.findById(id.getIdKH()).get();

        Invoice hoaDon = adTaoHoaDonRepository.findById(id.getIdHD()).get();

        hoaDon.setCustomer(khachHang);

        adTaoHoaDonRepository.save(hoaDon);

    }



    private ResponseObject<?> processDeliveryInvoice(Invoice invoice, ADThanhToanRequest request, Staff staff) {
        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.DA_XAC_NHAN);

        // Xử lý voucher
        processVoucherIfExists(invoice, request);

        adTaoHoaDonRepository.save(invoice);

        // Tạo lịch sử trạng thái
        createStatusHistory(invoice, EntityTrangThaiHoaDon.DA_XAC_NHAN,
                "Đơn hàng đã được xác nhận và chờ giao cho đơn vị vận chuyển.");

        // Tạo lịch sử thanh toán nếu là chuyển khoản
        if (request.getPhuongThucThanhToan() != null && request.getPhuongThucThanhToan().equals("1")) {
            createPaymentHistory(invoice, request, staff);
        }

        return new ResponseObject<>(null, HttpStatus.CREATED, "Xác nhận giao hàng thành công");
    }

    private ResponseObject<?> processCounterInvoice(Invoice invoice, ADThanhToanRequest request, Staff staff) {
        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.HOAN_THANH);

        // Xử lý voucher
        processVoucherIfExists(invoice, request);

        adTaoHoaDonRepository.save(invoice);

        // Tạo lịch sử trạng thái
        createStatusHistory(invoice, EntityTrangThaiHoaDon.HOAN_THANH,
                "Đơn hàng đã được khách hàng thanh toán thành công.");

        // Tạo lịch sử thanh toán
        createPaymentHistory(invoice, request, staff);

        return new ResponseObject<>(null, HttpStatus.CREATED, "Thanh toán thành công");
    }

    private void createStatusHistory(Invoice invoice, EntityTrangThaiHoaDon status, String note) {
        LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
        history.setThoiGian(LocalDateTime.now());
        history.setNote(note);
        history.setHoaDon(invoice);
        history.setTrangThai(status);
        lichSuTrangThaiHoaDonRepository.save(history);
    }

    private void createPaymentHistory(Invoice invoice, ADThanhToanRequest request, Staff staff) {
        try {
            LichSuThanhToan paymentHistory = new LichSuThanhToan();
            paymentHistory.setHoaDon(invoice);
            paymentHistory.setSoTien(request.getTongTien() != null ? request.getTongTien() : BigDecimal.ZERO);
            paymentHistory.setLoaiGiaoDich(request.getPhuongThucThanhToan());
            paymentHistory.setThoiGian(LocalDateTime.now());
            paymentHistory.setNhanVien(staff);
            paymentHistory.setMaGiaoDich(UUID.randomUUID().toString());
            adLichSuThanhToanRepository.save(paymentHistory);
        } catch (Exception e) {
            System.out.println("Lỗi khi tạo lịch sử thanh toán: " + e.getMessage());
            // Không throw exception vì đây không phải là lỗi nghiêm trọng
        }
    }

    @Override
    public List<Voucher> danhSachPhieuGiamGia1(ChonPhieuGiamGiaRequest request) throws BadRequestException {
        BigDecimal tongTien = request.getTongTien();

        // Validate tổng tiền
        if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Tổng tiền không hợp lệ");
        }

        // Lấy danh sách voucher phù hợp
        List<Voucher> phieuGiamGias = adTaoHoaDonRepository.getPhieuGiamGia(request.getIdKH(), tongTien);

        if (phieuGiamGias == null) {
            phieuGiamGias = new ArrayList<>();
        }

        System.out.println("Số lượng voucher tìm thấy: " + phieuGiamGias.size());

        if (!phieuGiamGias.isEmpty()) {
            phieuGiamGias.forEach(voucher -> {
                // Kiểm tra voucher còn hiệu lực
                Long currentTime = System.currentTimeMillis();
                if (voucher.getStartDate() != null && voucher.getStartDate() > currentTime) {
                    voucher.setGiaTriGiamThucTe(BigDecimal.ZERO); // Chưa đến thời gian bắt đầu
                    return;
                }

                if (voucher.getEndDate() != null && voucher.getEndDate() < currentTime) {
                    voucher.setGiaTriGiamThucTe(BigDecimal.ZERO); // Đã hết hạn
                    return;
                }

                // Kiểm tra số lượng còn lại
                if (voucher.getRemainingQuantity() != null && voucher.getRemainingQuantity() <= 0) {
                    voucher.setGiaTriGiamThucTe(BigDecimal.ZERO); // Hết số lượng
                    return;
                }

                // Tính toán giá trị giảm thực tế dựa trên typeVoucher
                if (voucher.getTypeVoucher() != null && voucher.getDiscountValue() != null) {
                    BigDecimal giaTriGiamThucTe = BigDecimal.ZERO;

                    if (voucher.getTypeVoucher() == TypeVoucher.PERCENTAGE) {
                        // Giảm theo phần trăm
                        BigDecimal phanTramGiam = voucher.getDiscountValue();
                        giaTriGiamThucTe = tongTien.multiply(phanTramGiam.divide(new BigDecimal(100)));

                        // Áp dụng giới hạn tối đa nếu có
                        if (voucher.getMaxValue() != null && giaTriGiamThucTe.compareTo(voucher.getMaxValue()) > 0) {
                            giaTriGiamThucTe = voucher.getMaxValue();
                        }
                    } else if (voucher.getTypeVoucher() == TypeVoucher.FIXED_AMOUNT) {
                        // Giảm theo số tiền cố định
                        giaTriGiamThucTe = voucher.getDiscountValue();

                        // Đảm bảo không giảm nhiều hơn tổng tiền
                        if (giaTriGiamThucTe.compareTo(tongTien) > 0) {
                            giaTriGiamThucTe = tongTien;
                        }
                    }

                    voucher.setGiaTriGiamThucTe(giaTriGiamThucTe);
                } else {
                    voucher.setGiaTriGiamThucTe(BigDecimal.ZERO);
                }
            });
        }

        // Lọc chỉ những voucher có giá trị giảm > 0
        phieuGiamGias = phieuGiamGias.stream()
                .filter(voucher -> voucher.getGiaTriGiamThucTe() != null
                        && voucher.getGiaTriGiamThucTe().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Voucher::getGiaTriGiamThucTe, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return phieuGiamGias;
    }



    private VoucherResponseDTO calculateActualDiscount(Voucher voucher, BigDecimal totalAmount) {
        try {
            BigDecimal actualDiscount = BigDecimal.ZERO;
            BigDecimal maxDiscount = voucher.getMaxValue() != null ? voucher.getMaxValue() : BigDecimal.ZERO;

            if (voucher.getTypeVoucher() == TypeVoucher.PERCENTAGE) {
                BigDecimal percentage = voucher.getDiscountValue();
                if (percentage != null) {
                    actualDiscount = totalAmount.multiply(percentage.divide(BigDecimal.valueOf(100)));

                    // Áp dụng giới hạn tối đa
                    if (maxDiscount.compareTo(BigDecimal.ZERO) > 0 &&
                            actualDiscount.compareTo(maxDiscount) > 0) {
                        actualDiscount = maxDiscount;
                    }
                }
            } else if (voucher.getTypeVoucher() == TypeVoucher.FIXED_AMOUNT) {
                actualDiscount = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : BigDecimal.ZERO;

                if (actualDiscount.compareTo(totalAmount) > 0) {
                    actualDiscount = totalAmount;
                }
            }

            // Kiểm tra điều kiện áp dụng
            if (voucher.getConditions() != null && totalAmount.compareTo(voucher.getConditions()) < 0) {
                return null;
            }

            // Kiểm tra thời gian hiệu lực
            Long now = System.currentTimeMillis();
            if (voucher.getStartDate() != null && now < voucher.getStartDate()) {
                return null;
            }
            if (voucher.getEndDate() != null && now > voucher.getEndDate()) {
                return null;
            }

            // Kiểm tra số lượng còn lại
            if (voucher.getRemainingQuantity() != null && voucher.getRemainingQuantity() <= 0) {
                return null;
            }

            return new VoucherResponseDTO(
                    voucher.getId(),
                    voucher.getCode(),
                    voucher.getName(),
                    voucher.getTypeVoucher(),
                    voucher.getDiscountValue(),
                    maxDiscount,
                    voucher.getConditions(),
                    actualDiscount,
                    voucher.getStartDate(),
                    voucher.getEndDate(),
                    voucher.getRemainingQuantity()
            );

        } catch (Exception e) {
            log.error("Error calculating discount for voucher {}: ", voucher.getId(), e);
            return null;
        }
    }

    private String formatCurrency(Double amount) {
        if (amount == null) return "0 ₫";
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    @Override
    public VoucherSuggestionResponse goiYVoucher(
            VoucherSuggestionRequest req
    ) {
        BigDecimal tongTien = req.getTongTien();
        Long now = System.currentTimeMillis();

        // 1️⃣ Lấy toàn bộ voucher có thể dùng
        List<Voucher> vouchers = new ArrayList<>();

        vouchers.addAll(
                adbhvoucher.findVoucherHopLe(
                        TargetType.ALL_CUSTOMERS, now
                )
        );

        if (req.getCustomerId() != null) {
            vouchers.addAll(
                    adbhvoucher.findVoucherRiengCuaKH(
                            req.getCustomerId()
                    )
            );
        }

        // 2️⃣ Voucher áp dụng ngay
        List<ApplicableVoucherResponse> apDung = vouchers.stream()
                .filter(v -> tongTien.compareTo(v.getConditions()) >= 0)
                .map(v -> toApplicable(v, tongTien))
                .sorted(
                        Comparator.comparing(
                                ApplicableVoucherResponse::getGiamGiaThucTe
                        ).reversed()
                )
                .toList();

        BigDecimal giamTotNhat = apDung.isEmpty()
                ? BigDecimal.ZERO
                : apDung.get(0).getGiamGiaThucTe();

        // 3️⃣ Voucher tốt hơn nếu mua thêm
        List<BetterVoucherResponse> totHon = vouchers.stream()
                .filter(v -> v.getConditions().compareTo(tongTien) > 0)
                .map(v -> toBetter(v, tongTien, giamTotNhat))
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparing(
                                BetterVoucherResponse::getHieuQua
                        ).reversed()
                )
                .toList();

        VoucherSuggestionResponse res = new VoucherSuggestionResponse();
        res.setVoucherApDung(apDung);
        res.setVoucherTotHon(totHon);

        return res;
    }

    // ================= HELPER =================

    private ApplicableVoucherResponse toApplicable(
            Voucher v,
            BigDecimal tongTien
    ) {
        BigDecimal giam = tinhGiam(v, tongTien);

        return new ApplicableVoucherResponse(
                v.getId(),
                v.getCode(),
                v.getTypeVoucher(),
                v.getDiscountValue(),
                v.getMaxValue(),
                v.getConditions(),
                giam,
                tongTien.subtract(giam)
        );
    }

    private BetterVoucherResponse toBetter(
            Voucher v,
            BigDecimal tongTien,
            BigDecimal giamHienTai
    ) {
        BigDecimal canMuaThem =
                v.getConditions().subtract(tongTien);

        BigDecimal giamMoi =
                tinhGiam(v, v.getConditions());

        BigDecimal giamThem =
                giamMoi.subtract(giamHienTai);

        if (giamThem.compareTo(BigDecimal.ZERO) <= 0)
            return null;

        BigDecimal hieuQua =
                giamThem.divide(
                        canMuaThem,
                        4,
                        RoundingMode.HALF_UP
                ).multiply(BigDecimal.valueOf(100));

        return new BetterVoucherResponse(
                v.getId(),
                v.getCode(),
                v.getConditions(),
                canMuaThem,
                giamMoi,
                giamThem,
                hieuQua
        );
    }

    private BigDecimal tinhGiam(
            Voucher v,
            BigDecimal tongTien
    ) {
        BigDecimal giam =
                v.getTypeVoucher() == TypeVoucher.PERCENTAGE
                        ? tongTien.multiply(v.getDiscountValue())
                        .divide(BigDecimal.valueOf(100))
                        : v.getDiscountValue();

        if (v.getMaxValue() != null) {
            giam = giam.min(v.getMaxValue());
        }

        return giam.min(tongTien);
    }



    @Override
    public ResponseObject<?> huy(ADHuyRequest request) {

        Invoice hoaDon = adTaoHoaDonRepository.findById(request.getIdHD()).get();

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
        hoaDon.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.DA_HUY);


        adTaoHoaDonRepository.save(hoaDon);

        LichSuTrangThaiHoaDon lichSuTrangThaiHoaDon = new LichSuTrangThaiHoaDon();
        lichSuTrangThaiHoaDon.setHoaDon(hoaDon);
        lichSuTrangThaiHoaDon.setTrangThai(EntityTrangThaiHoaDon.DA_HUY);
        lichSuTrangThaiHoaDon.setThoiGian(LocalDateTime.now());

        lichSuTrangThaiHoaDonRepository.save(lichSuTrangThaiHoaDon);

        return new ResponseObject<>(null, HttpStatus.CREATED, "Hủy hóa đơn thành công");
    }

    @Override
    public ResponseObject<?> getScreens() {
        return ResponseObject.successForward(
                screenRepository.getScreenComboboxResponse(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getBrands() {
        return ResponseObject.successForward(
                brandRepository.getBrandComboboxResponse(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getBatteries() {
        return ResponseObject.successForward(
                batteryRepository.getBatteryComboboxResponse(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getOperatingSystems() {
        return ResponseObject.successForward(
                operatingSystemRepository.getOperatingSystemComboboxResponse(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getColors() {
        return ResponseObject.successForward(
                colorRepository.getColors(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getRAMs() {
        return ResponseObject.successForward(
                ramRepository.getRAMs(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getCPUs() {
        return ResponseObject.successForward(
                cpuRepository.getCPUs(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getHardDrivers() {
        return ResponseObject.successForward(
                hardDriveRepository.getHardDrives(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getGPUs() {
        return ResponseObject.successForward(
                gpuRepository.getGPUs(),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getMaterials() {
        return ResponseObject.successForward(
                materialRepository.getMaterials(),
                "OKE"
        );
    }
}