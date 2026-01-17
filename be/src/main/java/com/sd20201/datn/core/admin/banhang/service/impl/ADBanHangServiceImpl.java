package com.sd20201.datn.core.admin.banhang.service.impl;

import com.sd20201.datn.core.admin.banhang.model.request.*;
import com.sd20201.datn.core.admin.banhang.model.response.ADChonKhachHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADGioHangResponse;
import com.sd20201.datn.core.admin.banhang.model.response.ADPhuongThucThanhToanRespones;
import com.sd20201.datn.core.admin.banhang.model.response.ListHoaDon;
import com.sd20201.datn.core.admin.banhang.repository.ADBanHangIMEIRepository;
import com.sd20201.datn.core.admin.banhang.repository.ADTaoHoaDonChiTietRepository;
import com.sd20201.datn.core.admin.banhang.repository.ADTaoHoaDonRepository;
import com.sd20201.datn.core.admin.banhang.service.ADBanHangService;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductDetailRepository;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.admin.voucher.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.*;
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import com.sd20201.datn.infrastructure.constant.TypePayment;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import com.sd20201.datn.repository.LichSuThanhToanResposiotry;
import com.sd20201.datn.repository.LichSuTrangThaiHoaDonRepository;
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
    public final LichSuThanhToanResposiotry adLichSuThanhToanRepository;

    @Override
    public List<ListHoaDon> getHoaDon() {
        return adTaoHoaDonRepository.getAll();
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

            hoaDon.setStaff(nhanVien);
            hoaDon.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
            hoaDon.setTypeInvoice(TypeInvoice.OFFLINE);
            hoaDon.setCreatedDate(System.currentTimeMillis());

            adTaoHoaDonRepository.save(hoaDon);

            // Tạo lịch sử trạng thái
            LichSuTrangThaiHoaDon lichSuTrangThaiHoaDon = new LichSuTrangThaiHoaDon();
            lichSuTrangThaiHoaDon.setThoiGian(LocalDateTime.now());
            lichSuTrangThaiHoaDon.setNote("Đơn hàng đã được tạo và đang chờ xử lý.");
            lichSuTrangThaiHoaDon.setHoaDon(hoaDon);
            lichSuTrangThaiHoaDon.setTrangThai(EntityTrangThaiHoaDon.CHO_XAC_NHAN);

            lichSuTrangThaiHoaDonRepository.save(lichSuTrangThaiHoaDon);

            return new ResponseObject<>(hoaDon, HttpStatus.CREATED, "Tạo hóa đơn thành công");

        } catch (Exception e) {
            log.error("Error creating invoice: ", e);
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo hóa đơn: " + e.getMessage());
        }
    }

    @Override
    public List<ADGioHangResponse> getListGioHang(String id) {
        try {
            return adTaoHoaDonChiTietRepository.getAllGioHang(id);
        } catch (Exception e) {
            log.error("Error getting cart for invoice {}: ", id, e);
            return Collections.emptyList();
        }
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
    public ResponseObject<?> danhSachPhieuGiamGia(ChonPhieuGiamGiaRequest request) {
        try {
            BigDecimal tongTien = request.getTongTien() != null
                    ? request.getTongTien()
                    : BigDecimal.ZERO;

            if (tongTien.compareTo(BigDecimal.ZERO) <= 0) {
                return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Tổng tiền không được để trống hoặc âm");
            }

            // Lấy danh sách voucher phù hợp
            List<Voucher> vouchers = adTaoHoaDonRepository.getAvailableVouchersNative(
                    request.getIdKH(),
                    tongTien,
                    System.currentTimeMillis()
            );

            if (vouchers == null || vouchers.isEmpty()) {
                return new ResponseObject<>(Collections.emptyList(), HttpStatus.OK,
                        "Không có voucher nào khả dụng");
            }

            // Tính toán giá trị giảm thực tế cho từng voucher
            List<VoucherResponseDTO> voucherResponses = vouchers.stream()
                    .map(voucher -> calculateActualDiscount(voucher, tongTien))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(VoucherResponseDTO::getActualDiscount).reversed())
                    .collect(Collectors.toList());

            return new ResponseObject<>(voucherResponses, HttpStatus.OK,
                    "Lấy danh sách voucher thành công");

        } catch (Exception e) {
            log.error("Error getting vouchers: ", e);
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy danh sách voucher: " + e.getMessage());
        }
    }

    @Override
    public ResponseObject<?> danhSachPhieuGiamGiaKoDuDieuKien(ChonPhieuGiamGiaRequest request) {
        try {
            BigDecimal tongTien = request.getTongTien() != null
                    ? request.getTongTien()
                    : BigDecimal.ZERO;

            // Lấy tất cả voucher có thể áp dụng (không kiểm tra điều kiện)
            List<Voucher> allVouchers = adTaoHoaDonRepository.getAllVouchersNative(
                    request.getIdKH(),
                    System.currentTimeMillis()
            );

            if (allVouchers == null || allVouchers.isEmpty()) {
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("applicableVouchers", Collections.emptyList());
                emptyResult.put("inapplicableVouchers", Collections.emptyList());
                return new ResponseObject<>(emptyResult, HttpStatus.OK,
                        "Không có voucher nào khả dụng");
            }

            // Phân loại voucher
            List<VoucherResponseDTO> applicableVouchers = new ArrayList<>();
            List<VoucherResponseDTO> inapplicableVouchers = new ArrayList<>();

            for (Voucher voucher : allVouchers) {
                VoucherResponseDTO response = calculateActualDiscount(voucher, tongTien);
                if (response != null) {
                    if (voucher.getConditions() == null ||
                            tongTien.compareTo(voucher.getConditions()) >= 0) {
                        applicableVouchers.add(response);
                    } else {
                        // Voucher không đủ điều kiện
                        BigDecimal amountNeeded = voucher.getConditions().subtract(tongTien);
                        if (amountNeeded.compareTo(BigDecimal.ZERO) > 0) {
                            response.setAdditionalMessage(
                                    "Cần thêm " + formatCurrency(amountNeeded.doubleValue()) + " để áp dụng voucher"
                            );
                        }
                        inapplicableVouchers.add(response);
                    }
                }
            }

            // Sắp xếp voucher có thể áp dụng theo giá trị giảm
            applicableVouchers.sort(Comparator.comparing(VoucherResponseDTO::getActualDiscount).reversed());

            Map<String, Object> result = new HashMap<>();
            result.put("applicableVouchers", applicableVouchers);
            result.put("inapplicableVouchers", inapplicableVouchers);

            // Tìm voucher tốt nhất
            if (!applicableVouchers.isEmpty()) {
                VoucherResponseDTO bestVoucher = applicableVouchers.get(0);
                result.put("bestVoucher", bestVoucher);

                // Tìm voucher tốt hơn trong danh sách không đủ điều kiện
                Optional<VoucherResponseDTO> betterVoucher = inapplicableVouchers.stream()
                        .filter(v -> v.getActualDiscount().compareTo(bestVoucher.getActualDiscount()) > 0)
                        .max(Comparator.comparing(VoucherResponseDTO::getActualDiscount));

                if (betterVoucher.isPresent()) {
                    result.put("betterVoucher", betterVoucher.get());
                }
            }

            return new ResponseObject<>(result, HttpStatus.OK,
                    "Lấy danh sách voucher thành công");

        } catch (Exception e) {
            log.error("Error getting vouchers without condition: ", e);
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy danh sách voucher: " + e.getMessage());
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
        try {
            // Kiểm tra xem sản phẩm đã có trong hóa đơn chưa
            List<String> idHDCT = adTaoHoaDonChiTietRepository.checkGioHang(request);

            if (idHDCT == null || idHDCT.isEmpty()) {
                return addNewProductToInvoice(request);
            } else {
                return updateExistingProductInInvoice(request, idHDCT.get(0));
            }

        } catch (RuntimeException e) {
            log.error("Error adding product to cart: ", e);
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error adding product to cart: ", e);
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi không xác định khi thêm sản phẩm");
        }
    }

    @Override
    public ResponseObject<?> thanhToanThanhCong(ADThanhToanRequest id) throws BadRequestException {

        System.out.println("  idHD length: " + (id.getIdHD() != null ? id.getIdHD().length() : "null"));
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

        // ========== XỬ LÝ VOUCHER ==========
        if (id.getIdPGG() != null && !id.getIdPGG().trim().isEmpty()) {
            try {
                Voucher phieuGiamGia1 = adVoucherRepository.findById(id.getIdPGG())
                        .orElseThrow(() -> new BadRequestException("Voucher không tồn tại"));

                if (phieuGiamGia1.getRemainingQuantity() <= 0) {
                    return new ResponseObject<>(null, HttpStatus.OK, "Voucher đã hết số lượng");
                }

                // Tạo request để kiểm tra voucher
                ChonPhieuGiamGiaRequest chonPhieuGiamGiaRequest = new ChonPhieuGiamGiaRequest();
                chonPhieuGiamGiaRequest.setTongTien(id.getTienHang());

                // Chỉ set idKH nếu customer tồn tại và có id
                if (hoaDon.getCustomer() != null && hoaDon.getCustomer().getId() != null) {
                    chonPhieuGiamGiaRequest.setIdKH(hoaDon.getCustomer().getId());
                }

                // Kiểm tra xem có voucher tốt hơn không (chỉ khi check = 1)
                if (id.getCheck() != null && id.getCheck() == 1) {
                    try {
                        List<Voucher> list = danhSachPhieuGiamGia1(chonPhieuGiamGiaRequest);

                        // CHỈ so sánh nếu cả hai đều có giá trị
                        if (!list.isEmpty() && list.get(0) != null && list.get(0).getGiaTriGiamThucTe() != null
                                && phieuGiamGia1.getGiaTriGiamThucTe() != null
                                && list.get(0).getGiaTriGiamThucTe().compareTo(phieuGiamGia1.getGiaTriGiamThucTe()) > 0) {
                            return new ResponseObject<>(null, HttpStatus.OK, "Đã có 1 phiếu giảm giá tốt hơn");
                        }
                    } catch (Exception e) {
                        // Nếu có lỗi khi kiểm tra voucher tốt hơn, bỏ qua và tiếp tục
                        System.out.println("Lỗi khi kiểm tra voucher tốt hơn: " + e.getMessage());
                    }
                }

            } catch (BadRequestException e) {
                throw e; // Re-throw BadRequestException
            } catch (Exception e) {
                // Nếu có lỗi khác với voucher, coi như không có voucher
                System.out.println("Lỗi khi xử lý voucher: " + e.getMessage());
                // Reset idPGG để không sử dụng voucher này
                id.setIdPGG(null);
                id.setCheck(0);
            }
        }

        // ========== KIỂM TRA SẢN PHẨM ==========
        try {
            List<String> idHDCTS = adTaoHoaDonChiTietRepository.getHoaDonChiTiet(id.getIdHD());

            if (idHDCTS == null || idHDCTS.isEmpty()) {
                throw new BadRequestException("Hóa đơn không có sản phẩm nào");
            }

            for (String idHDCT : idHDCTS) {
                if (idHDCT == null || idHDCT.trim().isEmpty()) {
                    continue; // Bỏ qua nếu ID null
                }

                InvoiceDetail hoaDonChiTiet = adTaoHoaDonChiTietRepository.findById(idHDCT)
                        .orElseThrow(() -> new BadRequestException("Chi tiết hóa đơn không tồn tại"));

                String idSPCT = adTaoHoaDonChiTietRepository.getSanPhamChiTiet(idHDCT);
                if (idSPCT == null || idSPCT.trim().isEmpty()) {
                    throw new BadRequestException("Không tìm thấy sản phẩm cho chi tiết hóa đơn");
                }

                // Kiểm tra sản phẩm tồn tại
                ProductDetail sanPhamChiTiet = adPDProductDetailRepository.findById(idSPCT)
                        .orElseThrow(() -> new BadRequestException("Sản phẩm không tồn tại"));

                // TODO: Xử lý kiểm tra số lượng tồn kho ở đây
                // Hiện tại ProductDetail không có trường số lượng
                // Cần xử lý thông qua IMEI hoặc cách khác
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi kiểm tra sản phẩm: " + e.getMessage());
        }

        // ========== XỬ LÝ THANH TOÁN ==========
        try {
            Invoice hoaDonToUpdate = adTaoHoaDonRepository.findById(id.getIdHD())
                    .orElseThrow(() -> new BadRequestException("Hóa đơn không tồn tại"));

            // Xác định phương thức thanh toán
            TypePayment phuongThucThanhToan = determinePaymentMethod(id.getPhuongThucThanhToan());

            // Cập nhật thông tin hóa đơn
            updateInvoiceInfo(hoaDonToUpdate, id, phuongThucThanhToan);

            // Lấy thông tin nhân viên
            Staff nhanVien = adNhanVienRepository.findById(id.getIdNV())
                    .orElseThrow(() -> new BadRequestException("Nhân viên không tồn tại"));
            hoaDonToUpdate.setStaff(nhanVien);

            // Xử lý theo loại hóa đơn
            if (hoaDonToUpdate.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
                return processDeliveryInvoice(hoaDonToUpdate, id, nhanVien);
            } else {
                return processCounterInvoice(hoaDonToUpdate, id, nhanVien);
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi xử lý thanh toán: " + e.getMessage());
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
        // Chỉ cập nhật thông tin nhận hàng nếu là giao hàng và có thông tin
        if (invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
            if (request.getTen() != null && !request.getTen().trim().isEmpty()) {
                invoice.setNameReceiver(request.getTen());
            }
            if (request.getSdt() != null && !request.getSdt().trim().isEmpty()) {
                invoice.setPhoneReceiver(request.getSdt());
            }
            if (request.getDiaChi() != null && !request.getDiaChi().trim().isEmpty()) {
                invoice.setAddressReceiver(request.getDiaChi());
            }
        }

        invoice.setShippingFee(request.getTienShip() != null ? request.getTienShip() : BigDecimal.ZERO);
        invoice.setTotalAmount(request.getTienHang() != null ? request.getTienHang() : BigDecimal.ZERO);
        invoice.setTotalAmountAfterDecrease(request.getTongTien() != null ? request.getTongTien() : BigDecimal.ZERO);
        invoice.setPaymentDate(System.currentTimeMillis());
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

    private void processVoucherIfExists(Invoice invoice, ADThanhToanRequest request) {
        if (request.getIdPGG() != null && !request.getIdPGG().trim().isEmpty()) {
            try {
                Voucher voucher = adVoucherRepository.findById(request.getIdPGG())
                        .orElse(null);

                if (voucher != null && voucher.getRemainingQuantity() > 0) {
                    voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
                    adVoucherRepository.save(voucher);
                    invoice.setVoucher(voucher);
                }
            } catch (Exception e) {
                // Bỏ qua lỗi voucher, không ảnh hưởng đến thanh toán
                System.out.println("Lỗi khi xử lý voucher: " + e.getMessage());
            }
        }
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



    private ResponseObject<?> addNewProductToInvoice(ADThemSanPhamRequest request) {
        ProductDetail sanPham = adSanPhamRepository.findById(request.getIdSP())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Lấy số lượng IMEI còn trong kho
        Long soLuongTrongKho = imeiRepository.countByProductDetailIdAndInvoiceDetailIsNull(request.getIdSP());
        int requestedQuantity = Integer.parseInt(request.getSoLuong());

        if (soLuongTrongKho < requestedQuantity) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                    "Số lượng sản phẩm thêm vào nhiều hơn số lượng trong kho. Hiện có: " + soLuongTrongKho);
        }

        Invoice hoaDon = adTaoHoaDonRepository.findById(request.getIdHD())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        InvoiceDetail hoaDonChiTiet = new InvoiceDetail();
        hoaDonChiTiet.setInvoice(hoaDon);
        hoaDonChiTiet.setProductDetail(sanPham);
        hoaDonChiTiet.setQuantity(requestedQuantity);
        hoaDonChiTiet.setPrice(sanPham.getPrice());
        hoaDonChiTiet.setTotalAmount(sanPham.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)));

        adTaoHoaDonChiTietRepository.save(hoaDonChiTiet);

        return new ResponseObject<>(null, HttpStatus.OK, "Thêm sản phẩm thành công");
    }

    private ResponseObject<?> updateExistingProductInInvoice(ADThemSanPhamRequest request, String idHDCT) {
        InvoiceDetail existingDetail = adTaoHoaDonChiTietRepository.findById(idHDCT)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết hóa đơn"));

        ProductDetail sanPham = adSanPhamRepository.findById(request.getIdSP())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        BigDecimal oldPrice = existingDetail.getPrice();
        BigDecimal newPrice = sanPham.getPrice();
        BigDecimal epsilon = new BigDecimal("0.0001");
        int additionalQuantity = Integer.parseInt(request.getSoLuong());

        // Kiểm tra xem giá có thay đổi không
        if (oldPrice.subtract(newPrice).abs().compareTo(epsilon) > 0) {
            // Giá đã thay đổi - tạo chi tiết mới
            return createNewInvoiceDetailForPriceChange(request, sanPham, existingDetail, additionalQuantity);
        } else {
            // Giá không đổi - cập nhật số lượng
            return updateProductQuantity(request, existingDetail, additionalQuantity);
        }
    }

    private ResponseObject<?> createNewInvoiceDetailForPriceChange(ADThemSanPhamRequest request, ProductDetail sanPham,
                                                                   InvoiceDetail existingDetail, int additionalQuantity) {
        Long soLuongTrongKho = imeiRepository.countByProductDetailIdAndInvoiceDetailIsNull(request.getIdSP());

        if (soLuongTrongKho < additionalQuantity) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                    "Số lượng sản phẩm thêm vào nhiều hơn số lượng trong kho. Hiện có: " + soLuongTrongKho);
        }

        Invoice hoaDon = adTaoHoaDonRepository.findById(request.getIdHD())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        // Tạo chi tiết hóa đơn mới với giá mới
        InvoiceDetail newDetail = new InvoiceDetail();
        newDetail.setInvoice(hoaDon);
        newDetail.setProductDetail(sanPham);
        newDetail.setQuantity(additionalQuantity);
        newDetail.setPrice(sanPham.getPrice());
        newDetail.setTotalAmount(sanPham.getPrice().multiply(BigDecimal.valueOf(additionalQuantity)));

        adTaoHoaDonChiTietRepository.save(newDetail);

        return new ResponseObject<>(null, HttpStatus.OK,
                "Sản phẩm này đang được thay đổi giá từ " + existingDetail.getPrice() + "đ thành " + sanPham.getPrice() + "đ");
    }

    private ResponseObject<?> updateProductQuantity(ADThemSanPhamRequest request, InvoiceDetail existingDetail,
                                                    int additionalQuantity) {
        Long soLuongTrongKho = imeiRepository.countByProductDetailIdAndInvoiceDetailIsNull(request.getIdSP());
        int newTotalQuantity = existingDetail.getQuantity() + additionalQuantity;

        if (soLuongTrongKho < newTotalQuantity) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST,
                    "Số lượng sản phẩm thêm vào nhiều hơn số lượng trong kho. Hiện có: " + soLuongTrongKho);
        }

        existingDetail.setQuantity(newTotalQuantity);
        existingDetail.setTotalAmount(existingDetail.getPrice().multiply(BigDecimal.valueOf(newTotalQuantity)));

        adTaoHoaDonChiTietRepository.save(existingDetail);

        return new ResponseObject<>(null, HttpStatus.OK, "Cập nhật số lượng sản phẩm thành công");
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
}