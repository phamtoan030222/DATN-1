package com.sd20201.datn.core.payment.service;

import com.sd20201.datn.core.admin.banhang.repository.*;
import com.sd20201.datn.core.admin.customer.repository.AdCustomerRepository;
import com.sd20201.datn.core.admin.hoadon.repository.ADHoaDonChiTietRepository;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductDetailRepository;
import com.sd20201.datn.core.admin.shift.repository.AdShiftHandoverRepository;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.admin.voucher.voucher.repository.AdVoucherRepository;
import com.sd20201.datn.core.admin.voucher.voucherudetail.repository.AdVoucherDetailRepository;
import com.sd20201.datn.core.payment.config.VnPayConfig;
import com.sd20201.datn.core.payment.dto.IpnResponse;
import com.sd20201.datn.core.payment.dto.PaymentRequest;
import com.sd20201.datn.core.payment.dto.PaymentResponse;
import com.sd20201.datn.core.payment.util.VnpayUtil;
import com.sd20201.datn.entity.*;
import com.sd20201.datn.infrastructure.constant.*;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import com.sd20201.datn.repository.InvoiceRepository;
import com.sd20201.datn.repository.LichSuThanhToanRepository;
import com.sd20201.datn.repository.LichSuTrangThaiHoaDonRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VnPayService {

    private static final Logger logger = LoggerFactory.getLogger(VnPayService.class);

    private final VnPayConfig vnPayConfig;
    private final InvoiceRepository invoiceRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;

    private final ADTaoHoaDonRepository adTaoHoaDonRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final ADTaoHoaDonChiTietRepository adTaoHoaDonChiTietRepository;
    private final ADPDProductDetailRepository adSanPhamRepository;
    private final ADBanHangIMEIRepository imeiRepository;
    private final ADStaffRepository adNhanVienRepository;
    private final AdVoucherRepository adVoucherRepository;
    private final AdVoucherDetailRepository adVoucherDetailRepository;
    private final ADPDProductDetailRepository adPDProductDetailRepository;
    private final ADBanHangSanPhamChiTiet productDetailRepository;
    private final AdCustomerRepository khachHangRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final ADHoaDonChiTietRepository adHoaDonChiTietRepository;
    private final AdShiftHandoverRepository adShiftHandoverRepository;

    // ==================== TẠO THANH TOÁN ====================
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest) {
        PaymentResponse response = new PaymentResponse();

        try {
            if (request.getTotalAmountAfterDecrease() == null
                    || request.getTotalAmountAfterDecrease().compareTo(BigDecimal.ZERO) <= 0) {
                response.setCode("01");
                response.setMessage("Số tiền không hợp lệ");
                return response;
            }

            Optional<Invoice> invoiceOpt = invoiceRepository.findByCode(request.getCode());
            if (invoiceOpt.isEmpty()) {
                response.setCode("02");
                response.setMessage("Không tìm thấy hóa đơn với mã: " + request.getCode());
                return response;
            }

            Invoice invoice = invoiceOpt.get();

            Customer kh = invoice.getCustomer();

            if (invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG
                    || invoice.getTypeInvoice() == TypeInvoice.ONLINE) {
                // Ưu tiên thông tin từ request (khách nhập khi checkout)
                if (request.getCustomerName() != null && !request.getCustomerName().isBlank())
                    invoice.setNameReceiver(request.getCustomerName());
                if (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank())
                    invoice.setPhoneReceiver(request.getCustomerPhone());
                if (request.getCustomerAddress() != null && !request.getCustomerAddress().isBlank())
                    invoice.setAddressReceiver(request.getCustomerAddress());
            } else {
                // TAI_QUAY: ưu tiên request, fallback sang customer
                invoice.setNameReceiver(
                        (request.getCustomerName() != null && !request.getCustomerName().isBlank())
                                ? request.getCustomerName()
                                : (kh != null ? kh.getName() : "Khách lẻ")
                );
                invoice.setPhoneReceiver(
                        (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank())
                                ? request.getCustomerPhone()
                                : (kh != null ? kh.getPhone() : "")
                );
                invoice.setAddressReceiver(
                        (request.getCustomerAddress() != null && !request.getCustomerAddress().isBlank())
                                ? request.getCustomerAddress() : ""
                );
            }

            if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                response.setCode("03");
                response.setMessage("Hóa đơn đã được thanh toán hoàn tất");
                return response;
            }

            // ✅ THÊM: Cập nhật total amount vào invoice trước khi tạo payment URL
            BigDecimal requestAmount = request.getTotalAmountAfterDecrease();
            // ✅ THAY BẰNG ĐOẠN NÀY

// 1. Tiền hàng gốc (trước giảm giá, chưa cộng ship)
            if (request.getTienHang() != null
                    && request.getTienHang().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setTotalAmountAfterDecrease(request.getTienHang());
            } else {
                invoice.setTotalAmountAfterDecrease(requestAmount); // fallback nếu FE không truyền
            }

// 2. Phí vận chuyển
            if (request.getTienShip() != null
                    && request.getTienShip().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setShippingFee(request.getTienShip());
            }

// 3. Gán voucher vào invoice
            if (request.getIdPGG() != null && !request.getIdPGG().isBlank()) {
                adVoucherRepository.findById(request.getIdPGG()).ifPresent(voucher -> {
                    invoice.setVoucher(voucher);

                    BigDecimal tienHangGoc = request.getTienHang() != null ? request.getTienHang() : BigDecimal.ZERO;
                    // Khách phải trả (requestAmount) chính là tổng tiền sau khi trừ voucher và cộng phí ship
                    BigDecimal khachPhaiTra = requestAmount != null ? requestAmount : BigDecimal.ZERO;
                    BigDecimal phiVanChuyen = request.getTienShip() != null ? request.getTienShip() : BigDecimal.ZERO;

                    // Công thức: Tiền giảm = (Tiền hàng gốc + Phí vận chuyển) - Khách phải trả
                    BigDecimal soTienGiam = (tienHangGoc.add(phiVanChuyen)).subtract(khachPhaiTra);

                    // Chỉ lưu nếu có giảm giá (tránh lưu số âm do sai sót)
                    if(soTienGiam.compareTo(BigDecimal.ZERO) > 0) {
                        invoice.setGiamGia(soTienGiam);
                    }
                    logger.info("Đã gán voucher {} vào hóa đơn VNPAY", voucher.getCode());
                });
            }

// 4. Tổng tiền thực tế thanh toán (sau giảm + ship)
            invoice.setTotalAmount(requestAmount);

            logger.info("Đã lưu đầy đủ: tienHang={}, ship={}, voucher={}, total={}",
                    invoice.getTotalAmount(),
                    invoice.getShippingFee(),
                    invoice.getVoucher() != null ? invoice.getVoucher().getCode() : "không có",
                    requestAmount);


            String ipAddr = getClientIpAddress(httpRequest);
            String maGiaoDich = "VNPAY" + System.currentTimeMillis();
            String paymentUrl = buildPaymentUrl(request, ipAddr, maGiaoDich);

            // Gán ca làm việc ngay lúc tạo payment — khi nhân viên còn đang đứng quầy
            if (invoice.getShiftHandover() == null && request.getStaffId() != null) {
                try {
                    adNhanVienRepository.findById(request.getStaffId()).ifPresent(staff -> {
                        if (staff.getAccount() != null) {
                            adShiftHandoverRepository
                                    .findOpenShiftByAccountId(staff.getAccount().getId())
                                    .ifPresent(shift -> {
                                        invoice.setShiftHandover(shift);
                                        logger.info("Đã gán ca làm việc: {} cho nhân viên: {}",
                                                shift.getId(), staff.getId());
                                    });
                        }
                    });
                } catch (Exception e) {
                    logger.error("Lỗi gán ca làm việc lúc tạo payment: ", e);
                }
            }

            saveLichSuThanhToan(invoice, request, maGiaoDich, paymentUrl);

            invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHO_THANH_TOAN);
            invoiceRepository.save(invoice);

            logger.info("Tạo payment URL thành công cho invoice: {}, maGiaoDich: {}",
                    request.getOrderId(), maGiaoDich);

            response.setCode("00");
            response.setMessage("Success");
            response.setPaymentUrl(paymentUrl);
            response.setTransactionId(maGiaoDich);
            response.setAmount(request.getTotalAmountAfterDecrease());
            response.setOrderId(request.getOrderId());

        } catch (Exception e) {
            logger.error("Lỗi tạo thanh toán VNPAY: ", e);
            response.setCode("99");
            response.setMessage("Lỗi hệ thống: " + e.getMessage());
        }

        return response;
    }

    // ==================== XỬ LÝ RETURN ====================
    @Transactional
    public PaymentResponse processReturn(Map<String, String> params) {
        PaymentResponse response = new PaymentResponse();
        try {
            if (!params.containsKey("vnp_ResponseCode")
                    || !params.containsKey("vnp_TxnRef")) {
                response.setCode("98");
                response.setMessage("Thiếu thông tin từ VNPAY");
                return response;
            }

            if (!validateSignature(params)) {
                response.setCode("97");
                response.setMessage("Chữ ký không hợp lệ");
                return response;
            }

            String responseCode  = params.get("vnp_ResponseCode");
            String invoiceId     = params.get("vnp_TxnRef").split("_")[0];
            String transactionNo = params.get("vnp_TransactionNo");
            String amountStr     = params.get("vnp_Amount");
            String bankCode      = params.get("vnp_BankCode");

            BigDecimal vnpayAmount = BigDecimal.ZERO;
            if (amountStr != null && !amountStr.isEmpty()) {
                try {
                    vnpayAmount = new BigDecimal(Long.parseLong(amountStr) / 100);
                } catch (NumberFormatException ignored) {}
            }

            response.setOrderId(invoiceId);
            response.setTransactionId(transactionNo);
            response.setAmount(vnpayAmount);

            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                response.setCode("01");
                response.setMessage("Không tìm thấy hóa đơn");
                return response;
            }

            Invoice invoice = invoiceOpt.get();

            if ("00".equals(responseCode)) {
                // Chống xử lý trùng
                if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                    response.setCode("00");
                    response.setMessage("Thanh toán thành công");
                    return response;
                }

//                // Cập nhật invoice LUU_TAM → CHO_XAC_NHAN
//                invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
//                invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
//                invoice.setTypePayment(TypePayment.VNPAY);
//                invoice.setTransactionId(transactionNo);
//                invoice.setBankCode(bankCode);
//                invoice.setPaymentDate(System.currentTimeMillis());
//                Invoice saved = invoiceRepository.save(invoice);
//
//                // Lịch sử trạng thái
//                LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
//                history.setHoaDon(saved);
//                history.setTrangThai(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
//                history.setNote("Thanh toán VNPAY thành công - chờ xác nhận");
//                history.setThoiGian(LocalDateTime.now());
//                lichSuTrangThaiHoaDonRepository.save(history);
//
//                // Lịch sử thanh toán
//                LichSuThanhToan lichSu = new LichSuThanhToan();
//                lichSu.setHoaDon(saved);
//                lichSu.setSoTien(vnpayAmount);
//                lichSu.setLoaiGiaoDich("VNPAY");
//                lichSu.setMaGiaoDich(transactionNo);
//                lichSu.setThoiGian(LocalDateTime.now());
//                lichSu.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
//                lichSu.setGhiChu("VNPAY return - BankCode: " + bankCode);
//                lichSuThanhToanRepository.save(lichSu);
//
//                response.setCode("00");
//                response.setMessage("Thanh toán thành công");
                try {
                    xuLyThanhToanThanhCongVNPay(invoice, transactionNo, bankCode, vnpayAmount);

                    response.setCode("00");
                    response.setMessage("Thanh toán thành công");
                } catch (Exception e) {
                    logger.error("Lỗi khi xử lý Return URL VNPAY: ", e);
                    response.setCode("99");
                    response.setMessage("Lỗi xử lý nghiệp vụ: " + e.getMessage());
                }
            } else {
                // Thất bại / hủy → giữ LUU_TAM để user thử lại
                response.setCode(responseCode);
                response.setMessage("Thanh toán thất bại hoặc bị hủy");
                logger.info("VNPAY return thất bại - invoiceId: {}, code: {}",
                        invoiceId, responseCode);
            }

        } catch (Exception e) {
            logger.error("Lỗi processReturn VNPAY: ", e);
            response.setCode("99");
            response.setMessage(e.getMessage());
        }
        return response;
    }

    public boolean validateSignaturePublic(Map<String, String> params) {
        return validateSignature(params);
    }

    // ==================== XỬ LÝ IPN ====================
    @Transactional
    public IpnResponse processIpn(Map<String, String> params) {
        try {
            logger.info("=== PARAMS FROM VNPAY IPN ===");
            params.forEach((key, value) -> logger.info("{} = {}", key, value));

            if (!validateSignature(params)) {
                logger.warn("IPN - Chữ ký không hợp lệ: {}", params.get("vnp_TxnRef"));
                return new IpnResponse("97", "Invalid signature");
            }

            String responseCode  = params.get("vnp_ResponseCode");
            String invoiceId     = params.get("vnp_TxnRef").split("_")[0];
            String transactionNo = params.get("vnp_TransactionNo");
            String amountStr     = params.get("vnp_Amount");
            String bankCode      = params.get("vnp_BankCode");
            String payDate       = params.get("vnp_PayDate");

            // ✅ Parse vnpayAmount ĐÚNG
            BigDecimal vnpayAmount = BigDecimal.ZERO;
            if (amountStr != null && !amountStr.isEmpty()) {
                try {
                    vnpayAmount = new BigDecimal(Long.parseLong(amountStr) / 100);
                } catch (NumberFormatException e) {
                    logger.warn("Không parse được vnp_Amount: {}", amountStr);
                }
            }

            logger.info("IPN received - Invoice ID: {}, ResponseCode: {}, Amount: {}, Transaction: {}",
                    invoiceId, responseCode, vnpayAmount, transactionNo);

            Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
            if (invoiceOpt.isEmpty()) {
                logger.warn("IPN - Không tìm thấy hóa đơn: {}", invoiceId);
                return new IpnResponse("01", "Invoice not found");
            }

            Invoice invoice = invoiceOpt.get();

//            // Kiểm tra số tiền khớp
//            BigDecimal invoiceAmount = invoice.getTotalAmountAfterDecrease();
//            if (invoiceAmount != null && vnpayAmount.compareTo(invoiceAmount) != 0) {
//                logger.warn("IPN - Số tiền không khớp. Invoice: {}, VNPAY: {}", invoiceAmount, vnpayAmount);
//                return new IpnResponse("04", "Amount invalid");
//            }

            if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                logger.info("IPN - Hóa đơn đã được thanh toán trước đó: {}", invoiceId);
                return new IpnResponse("00", "Order already confirmed");
            }

            LichSuThanhToan lichSuIpn = new LichSuThanhToan();
            lichSuIpn.setSoTien(vnpayAmount);
            lichSuIpn.setThoiGian(LocalDateTime.now());
            lichSuIpn.setMaGiaoDich(transactionNo);
            lichSuIpn.setLoaiGiaoDich("VNPAY_IPN");
            lichSuIpn.setHoaDon(invoice);
            lichSuIpn.setGhiChu(String.format(
                    "VNPAY IPN - ResponseCode: %s, BankCode: %s, PayDate: %s",
                    responseCode, bankCode, payDate));

            if ("00".equals(responseCode)) {
                lichSuIpn.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);

                try {
                    // ✅ Truyền vnpayAmount xuống toàn bộ call chain
                    xuLyThanhToanThanhCongVNPay(invoice, transactionNo, bankCode, vnpayAmount);
                } catch (Exception e) {
                    logger.error("Lỗi khi xử lý thanh toán thành công từ IPN: ", e);
                }

            } else {
                lichSuIpn.setTrangThaiThanhToan(TrangThaiThanhToan.THANH_TOAN_THAT_BAI);
                invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
                invoiceRepository.save(invoice);
            }

            lichSuThanhToanRepository.save(lichSuIpn);

            lichSuThanhToanRepository
                    .findFirstByHoaDonIdAndLoaiGiaoDichOrderByThoiGianDesc(invoice.getId(), "VNPAY_CREATE")
                    .ifPresent(lichSu -> {
                        lichSu.setTrangThaiThanhToan(lichSuIpn.getTrangThaiThanhToan());
                        lichSu.setGhiChu(lichSu.getGhiChu() + " -> IPN Received");
                        lichSuThanhToanRepository.save(lichSu);
                    });

            return new IpnResponse("00", "Confirm Success");

        } catch (Exception e) {
            logger.error("Lỗi xử lý IPN: ", e);
            return new IpnResponse("99", "Unknown error");
        }
    }

    // ==================== XỬ LÝ THANH TOÁN THÀNH CÔNG ====================
    // ✅ Thêm tham số vnpayAmount vào signature
    @Transactional
    public void xuLyThanhToanThanhCongVNPay(Invoice hoaDon, String maGiaoDich,
                                            String bankCode, BigDecimal vnpayAmount) throws BadRequestException {
        logger.info("=== BẮT ĐẦU XỬ LÝ THANH TOÁN VNPAY THÀNH CÔNG ===");
        logger.info("Mã hóa đơn: {}, Mã GD: {}, Ngân hàng: {}, Số tiền: {}",
                hoaDon.getCode(), maGiaoDich, bankCode, vnpayAmount);

        try {
            if (hoaDon.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                logger.info("Hóa đơn đã được thanh toán trước đó");
                return;
            }

            // ✅ Nếu vnpayAmount null hoặc 0, fallback sang invoice amount
            BigDecimal finalAmount = (vnpayAmount != null && vnpayAmount.compareTo(BigDecimal.ZERO) > 0)
                    ? vnpayAmount
                    : hoaDon.getTotalAmountAfterDecrease();

            logger.info("Số tiền sẽ ghi lịch sử: {}", finalAmount);

            validateProducts(hoaDon);
            processVoucherForInvoice(hoaDon);
            ganCaLamViecChoHoaDon(hoaDon);

            if (hoaDon.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
                xuLyHoaDonGiaoHangVNPay(hoaDon, maGiaoDich, bankCode, finalAmount);
            } else if (hoaDon.getTypeInvoice() == TypeInvoice.ONLINE || hoaDon.getTypeInvoice() == TypeInvoice.ONLINE_TAI_QUAY) {
                xuLyHoaDonOnlineVNPay(hoaDon, maGiaoDich, bankCode, finalAmount);
            } else {
                xuLyHoaDonTaiQuayVNPay(hoaDon, maGiaoDich, bankCode, finalAmount);
            }

            logger.info("=== KẾT THÚC XỬ LÝ THANH TOÁN VNPAY THÀNH CÔNG ===");

        } catch (Exception e) {
            logger.error("Lỗi xử lý thanh toán thành công VNPAY: ", e);
            throw new BadRequestException("Lỗi xử lý thanh toán thành công VNPAY: " + e.getMessage());
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private void validateProducts(Invoice hoaDon) throws BadRequestException {
        logger.info("=== KIỂM TRA SẢN PHẨM ===");

        List<String> idHDCTS = adTaoHoaDonChiTietRepository.getHoaDonChiTiet(hoaDon.getId());

        if (idHDCTS == null || idHDCTS.isEmpty()) {
            throw new BadRequestException("Hóa đơn không có sản phẩm nào");
        }

        for (String idHDCT : idHDCTS) {
            if (idHDCT == null || idHDCT.trim().isEmpty()) continue;

            Optional<InvoiceDetail> detailOpt = adTaoHoaDonChiTietRepository.findById(idHDCT);
            if (detailOpt.isEmpty()) {
                throw new BadRequestException("Chi tiết hóa đơn không tồn tại: " + idHDCT);
            }

            String idSPCT = adTaoHoaDonChiTietRepository.getSanPhamChiTiet(idHDCT);
            if (idSPCT == null || idSPCT.trim().isEmpty()) {
                throw new BadRequestException("Không tìm thấy sản phẩm cho chi tiết hóa đơn");
            }

            Optional<ProductDetail> productOpt = adPDProductDetailRepository.findById(idSPCT);
            if (productOpt.isEmpty()) {
                throw new BadRequestException("Sản phẩm không tồn tại: " + idSPCT);
            }
        }

        logger.info("Kiểm tra sản phẩm thành công!");
    }

    private void processVoucherForInvoice(Invoice invoice) {
        if (invoice.getVoucher() == null) return;

        logger.info("=== XỬ LÝ VOUCHER ===");
        Voucher voucher = invoice.getVoucher();

        if (voucher.getRemainingQuantity() > 0) {
            voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
            adVoucherRepository.save(voucher);
            logger.info("Đã trừ số lượng voucher, còn lại: {}", voucher.getRemainingQuantity());
        }

        if (invoice.getCustomer() != null) {
            String customerId = invoice.getCustomer().getId();
            String voucherId  = voucher.getId();

            adVoucherDetailRepository
                    .findByVoucherIdAndCustomerId(voucherId, customerId)
                    .ifPresent(detail -> {
                        detail.setUsageStatus(true);
                        detail.setUsedDate(System.currentTimeMillis());
                        detail.setInvoiceId(invoice.getCode());
                        adVoucherDetailRepository.save(detail);
                        logger.info("Đã cập nhật trạng thái đã sử dụng cho khách hàng: {}", customerId);
                    });
        }
    }

    private void ganCaLamViecChoHoaDon(Invoice hoaDon) {
        if (hoaDon.getShiftHandover() != null) return;

        try {
            Optional<Staff> staffOpt = adNhanVienRepository.findAll().stream().findFirst();
            if (staffOpt.isPresent() && staffOpt.get().getAccount() != null) {
                String accountId = staffOpt.get().getAccount().getId();
                adShiftHandoverRepository.findOpenShiftByAccountId(accountId)
                        .ifPresent(shift -> {
                            hoaDon.setShiftHandover(shift);
                            logger.info("Đã gán hóa đơn vào ca làm việc: {}", shift.getId());
                        });
            }
        } catch (Exception e) {
            logger.error("Lỗi gán ca làm việc: ", e);
        }
    }

    // ✅ Thêm tham số amount
    private void xuLyHoaDonGiaoHangVNPay(Invoice invoice, String maGiaoDich,
                                         String bankCode, BigDecimal amount) {
        logger.info("=== XỬ LÝ HÓA ĐƠN GIAO HÀNG (VNPAY) - Số tiền: {} ===", amount);

        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.DA_XAC_NHAN);
        invoice.setPaymentDate(System.currentTimeMillis());
        invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        invoice.setTypePayment(TypePayment.VNPAY);
        invoice.setTransactionId(maGiaoDich);
        invoice.setBankCode(bankCode);

        Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);
        Staff staff = savedInvoice.getStaff();

        createStatusHistory(savedInvoice, EntityTrangThaiHoaDon.DA_XAC_NHAN,
                "Hóa đơn giao hàng đã được thanh toán qua VNPAY", staff);

        // ✅ Truyền amount xuống
        createPaymentHistoryVNPay(savedInvoice, maGiaoDich, bankCode, amount);
        updateProductQuantityAndImei(savedInvoice);

        logger.info("Đã xử lý thành công hóa đơn giao hàng qua VNPAY");
    }

    // ✅ Thêm tham số amount
    private void xuLyHoaDonTaiQuayVNPay(Invoice invoice, String maGiaoDich,
                                        String bankCode, BigDecimal amount) {
        logger.info("=== XỬ LÝ HÓA ĐƠN TẠI QUẦY (VNPAY) - Số tiền: {} ===", amount);

        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.HOAN_THANH);
        invoice.setPaymentDate(System.currentTimeMillis());
        invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        invoice.setTypePayment(TypePayment.VNPAY);
        invoice.setTransactionId(maGiaoDich);
        invoice.setBankCode(bankCode);

        Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);
        Staff staff = savedInvoice.getStaff();

        createStatusHistory(savedInvoice, EntityTrangThaiHoaDon.HOAN_THANH,
                "Hóa đơn đã được thanh toán qua VNPAY tại quầy", staff);

        // ✅ Truyền amount xuống
        createPaymentHistoryVNPay(savedInvoice, maGiaoDich, bankCode, amount);
        updateProductQuantityAndImei(savedInvoice);

        logger.info("Đã xử lý thành công hóa đơn tại quầy qua VNPAY");
    }

    private void updateProductQuantityAndImei(Invoice invoice) {
        try {
            logger.info("=== CẬP NHẬT TỒN KHO VÀ IMEI ===");

            List<InvoiceDetail> invoiceDetails = adHoaDonChiTietRepository.findByInvoiceId(invoice.getId());

            for (InvoiceDetail detail : invoiceDetails) {
                if (detail.getImeis() != null && !detail.getImeis().isEmpty()) {
                    for (IMEI imei : detail.getImeis()) {
                        imei.setImeiStatus(ImeiStatus.SOLD);
                        imei.setSoldAt(System.currentTimeMillis());
                        imei.setInvoiceDetail(detail);
                    }
                    imeiRepository.saveAll(detail.getImeis());
                    logger.info("Đã cập nhật trạng thái IMEI thành SOLD");
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi cập nhật tồn kho và IMEI: ", e);
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
            logger.info("Đã tạo lịch sử trạng thái: {} - {}", status, note);
        } catch (Exception e) {
            logger.error("Lỗi tạo lịch sử trạng thái: ", e);
        }
    }

    // ✅ Nhận amount trực tiếp, KHÔNG lấy từ invoice nữa
    private void createPaymentHistoryVNPay(Invoice invoice, String maGiaoDich,
                                           String bankCode, BigDecimal amount) {
        try {
            logger.info("=== TẠO LỊCH SỬ THANH TOÁN VNPAY - Số tiền: {} ===", amount);

            // ✅ Đảm bảo không bao giờ lưu 0
            BigDecimal soTienGhi = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                    ? amount
                    : invoice.getTotalAmountAfterDecrease();

            LichSuThanhToan paymentHistory = new LichSuThanhToan();
            paymentHistory.setHoaDon(invoice);
            paymentHistory.setSoTien(soTienGhi);                          // ✅ Luôn có giá trị đúng
            paymentHistory.setLoaiGiaoDich("VNPAY");
            paymentHistory.setThoiGian(LocalDateTime.now());
            paymentHistory.setMaGiaoDich(maGiaoDich);
            paymentHistory.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
            paymentHistory.setGhiChu("Thanh toán VNPAY - Ngân hàng: " + bankCode);

            lichSuThanhToanRepository.save(paymentHistory);

            logger.info("Đã tạo lịch sử thanh toán: Mã GD={}, Số tiền={}, Ngân hàng={}",
                    maGiaoDich, soTienGhi, bankCode);

        } catch (Exception e) {
            logger.error("Lỗi tạo lịch sử thanh toán: ", e);
        }
    }

    // ==================== URL & SIGNATURE ====================

    private String buildPaymentUrl(PaymentRequest request, String ipAddr, String maGiaoDich) throws Exception {
        Map<String, String> params = new TreeMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        long amountInCents = request.getTotalAmountAfterDecrease()
                .multiply(BigDecimal.valueOf(100)).longValue();
        params.put("vnp_Amount", String.valueOf(amountInCents));
        params.put("vnp_CurrCode", "VND");

        String uniqueTxnRef = request.getOrderId() + "_" + System.currentTimeMillis();
        params.put("vnp_TxnRef", uniqueTxnRef);

        String orderInfo = request.getCode() != null
                ? request.getCode()
                : "Thanh toan hoa don " + request.getOrderId();
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", request.getOrderType());
        params.put("vnp_Locale", request.getLanguage() != null ? request.getLanguage() : "vn");
        String returnUrl = (request.getReturnUrl() != null && !request.getReturnUrl().isBlank())
                ? request.getReturnUrl()
                : vnPayConfig.getReturnUrl();
        params.put("vnp_ReturnUrl", returnUrl);

        params.put("vnp_IpAddr", ipAddr);
        params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        if (request.getCustomerName() != null && !request.getCustomerName().isEmpty()) {
            params.put("vnp_Bill_FirstName", request.getCustomerName());
        }

        StringBuilder hashData    = new StringBuilder();
        StringBuilder queryBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key   = entry.getKey();
            String value = entry.getValue();

            if (value != null && !value.isEmpty()) {
                String encodedValue = URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
                hashData.append(key).append("=").append(encodedValue).append("&");
                queryBuilder.append(key).append("=").append(encodedValue).append("&");
            }
        }

        if (hashData.length() > 0)    hashData.deleteCharAt(hashData.length() - 1);
        if (queryBuilder.length() > 0) queryBuilder.deleteCharAt(queryBuilder.length() - 1);

        String secureHash = VnpayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

        return vnPayConfig.getPayUrl() + "?" + queryBuilder + "&vnp_SecureHash=" + secureHash;
    }

    private boolean validateSignature(Map<String, String> params) {
        try {
            String secureHash = params.get("vnp_SecureHash");
            if (secureHash == null || secureHash.isEmpty()) {
                logger.error("secureHash is null or empty");
                return false;
            }

            Map<String, String> paramsCopy = new HashMap<>(params);
            paramsCopy.remove("vnp_SecureHash");
            paramsCopy.remove("vnp_SecureHashType");

            List<String> fieldNames = new ArrayList<>(paramsCopy.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = paramsCopy.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
                    hashData.append(fieldName).append("=").append(encodedValue).append("&");
                }
            }

            if (hashData.length() > 0) hashData.deleteCharAt(hashData.length() - 1);

            String computedHash = VnpayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            return secureHash.equals(computedHash);

        } catch (Exception e) {
            logger.error("Lỗi validate signature: ", e);
            return false;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "localhost".equals(ip))
            ip = "127.0.0.1";
        return ip;
    }

    private void saveLichSuThanhToan(Invoice invoice, PaymentRequest request,
                                     String maGiaoDich, String paymentUrl) {
        LichSuThanhToan lichSu = new LichSuThanhToan();
        lichSu.setSoTien(request.getTotalAmountAfterDecrease());
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setMaGiaoDich(maGiaoDich);
        lichSu.setLoaiGiaoDich("VNPAY_CREATE");
        lichSu.setHoaDon(invoice);
        lichSu.setTrangThaiThanhToan(TrangThaiThanhToan.CHO_THANH_TOAN);

        String shortUrl = paymentUrl != null && paymentUrl.length() > 200
                ? paymentUrl.substring(0, 200) + "..."
                : paymentUrl;
        lichSu.setGhiChu("Tạo thanh toán VNPAY - URL: " + shortUrl);

        lichSuThanhToanRepository.save(lichSu);
    }

    @Transactional
    public Map<String, Object> manualConfirm(String invoiceId) throws BadRequestException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + invoiceId));

        if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
            return Map.of(
                    "trangThai", "DA_THANH_TOAN",
                    "message", "Hóa đơn đã thanh toán trước đó"
            );
        }

        // Lấy số tiền từ invoice (đã được lưu lúc createPayment)
        BigDecimal amount = invoice.getTotalAmountAfterDecrease();
        String maGiaoDich = "MANUAL_" + System.currentTimeMillis();

        // Gọi đúng method xử lý — y hệt callback VNPAY
        xuLyThanhToanThanhCongVNPay(invoice, maGiaoDich, "MANUAL", amount);

        return Map.of(
                "trangThai", "DA_THANH_TOAN",
                "message", "Xác nhận thành công"
        );
    }

    private void xuLyHoaDonOnlineVNPay(Invoice invoice, String maGiaoDich,
                                       String bankCode, BigDecimal amount) {
        logger.info("=== XỬ LÝ HÓA ĐƠN ONLINE (VNPAY) - Số tiền: {} ===", amount);

        // Đơn online sau thanh toán → CHO_XAC_NHAN
        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
        invoice.setPaymentDate(System.currentTimeMillis());
        invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        invoice.setTypePayment(TypePayment.VNPAY);
        invoice.setTransactionId(maGiaoDich);
        invoice.setBankCode(bankCode);

        Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);
        Staff staff = savedInvoice.getStaff();

        createStatusHistory(savedInvoice, EntityTrangThaiHoaDon.CHO_XAC_NHAN,
                "Đơn hàng online thanh toán VNPAY thành công - chờ shop xác nhận", staff);

        createPaymentHistoryVNPay(savedInvoice, maGiaoDich, bankCode, amount);

        // KHÔNG gọi updateProductQuantityAndImei — serial chưa được gán

        logger.info("Đã xử lý đơn online VNPAY, trạng thái: CHO_XAC_NHAN");
    }
}