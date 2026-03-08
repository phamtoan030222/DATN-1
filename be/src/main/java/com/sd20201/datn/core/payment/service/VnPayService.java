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
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.ImeiStatus;
import com.sd20201.datn.infrastructure.constant.TrangThaiThanhToan;
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
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
    private final AdVoucherDetailRepository adVoucherDetailRepository; // SỬA: Dùng đúng repository
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
            // Validation số tiền
            if (request.getTotalAmountAfterDecrease() == null || request.getTotalAmountAfterDecrease().compareTo(BigDecimal.ZERO) <= 0) {
                response.setCode("01");
                response.setMessage("Số tiền không hợp lệ");
                return response;
            }

            // Kiểm tra invoice tồn tại
            Optional<Invoice> invoiceOpt = invoiceRepository.findByCode(request.getCode());
            if (invoiceOpt.isEmpty()) {
                response.setCode("02");
                response.setMessage("Không tìm thấy hóa đơn với mã: " + request.getCode());
                return response;
            }

            Invoice invoice = invoiceOpt.get();

            // Kiểm tra trạng thái thanh toán
            if (invoice.getTrangThaiThanhToan() != TrangThaiThanhToan.CHUA_THANH_TOAN) {
                response.setCode("03");
                response.setMessage("Hóa đơn đã được thanh toán hoặc đang xử lý");
                return response;
            }

            // KIỂM TRA SỐ TIỀN
            BigDecimal invoiceAmount = invoice.getTotalAmountAfterDecrease();
            BigDecimal requestAmount = request.getTotalAmountAfterDecrease();

            logger.info("So sánh số tiền - Invoice: {}, Request: {}", invoiceAmount, requestAmount);

            // BỎ COMMENT DÒNG NÀY KHI TEST XONG
//            if (requestAmount.compareTo(invoiceAmount) != 0) {
//                response.setCode("04");
//                response.setMessage(String.format(
//                        "Số tiền không khớp với hóa đơn. Hóa đơn: %s, Gửi lên: %s",
//                        invoiceAmount.toString(), requestAmount.toString()
//                ));
//                return response;
//            }

            // Lấy IP thực của client
            String ipAddr = getClientIpAddress(httpRequest);

            // Tạo mã giao dịch
            String maGiaoDich = "VNPAY" + System.currentTimeMillis();

            // Tạo payment URL
            String paymentUrl = buildPaymentUrl(request, ipAddr, maGiaoDich);

            // Lưu lịch sử thanh toán
            saveLichSuThanhToan(invoice, request, maGiaoDich, paymentUrl);

            // Cập nhật trạng thái thanh toán của hóa đơn
            invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHO_THANH_TOAN_VNPAY);
            invoiceRepository.save(invoice);

            logger.info("Tạo payment URL thành công cho invoice: {}, maGiaoDich: {}",
                    request.getOrderId(), maGiaoDich);

            // Build response
            response.setCode("00");
            response.setMessage("Success");
            response.setPaymentUrl(paymentUrl);
            response.setTransactionId(maGiaoDich);
            response.setAmount(requestAmount);
            response.setOrderId(request.getOrderId());

        } catch (Exception e) {
            logger.error("Lỗi tạo thanh toán VNPAY: ", e);
            response.setCode("99");
            response.setMessage("Lỗi hệ thống: " + e.getMessage());
        }

        return response;
    }

    // ==================== XỬ LÝ RETURN (KẾT QUẢ TỪ VNPAY) ====================
    @Transactional
    public PaymentResponse processReturn(Map<String, String> params) {
        PaymentResponse response = new PaymentResponse();

        try {
            // LOG TẤT CẢ PARAMS NHẬN ĐƯỢC
            logger.info("=== PARAMS FROM VNPAY RETURN ===");
            params.forEach((key, value) -> logger.info("{} = {}", key, value));

            // KIỂM TRA PARAMS CƠ BẢN
            if (!params.containsKey("vnp_ResponseCode") || !params.containsKey("vnp_TxnRef")) {
                response.setCode("98");
                response.setMessage("Thiếu thông tin giao dịch từ VNPAY");
                return response;
            }

            // Xác thực chữ ký
            if (!validateSignature(params)) {
                response.setCode("97");
                response.setMessage("Chữ ký không hợp lệ");
                return response;
            }

            String responseCode = params.get("vnp_ResponseCode");
            String invoiceCode = params.get("vnp_TxnRef");
            String transactionNo = params.get("vnp_TransactionNo");
            String amount = params.get("vnp_Amount");
            String bankCode = params.get("vnp_BankCode");
            String payDate = params.get("vnp_PayDate");

            logger.info("VNPAY return - Invoice: {}, ResponseCode: {}, Transaction: {}",
                    invoiceCode, responseCode, transactionNo);

            // Tìm invoice
            Optional<Invoice> invoiceOpt = invoiceRepository.findByCode(invoiceCode);
            if (invoiceOpt.isPresent()) {
                Invoice invoice = invoiceOpt.get();

                // Tính số tiền từ VNPAY (chia 100)
                BigDecimal vnpayAmount = amount != null ?
                        new BigDecimal(Long.parseLong(amount) / 100) : BigDecimal.ZERO;

                // Tạo lịch sử thanh toán mới cho kết quả
                LichSuThanhToan lichSuReturn = new LichSuThanhToan();
                lichSuReturn.setSoTien(vnpayAmount);
                lichSuReturn.setThoiGian(LocalDateTime.now());
                lichSuReturn.setMaGiaoDich(transactionNo);
                lichSuReturn.setLoaiGiaoDich("VNPAY_RETURN");
                lichSuReturn.setHoaDon(invoice);
                lichSuReturn.setGhiChu(String.format(
                        "VNPAY return - ResponseCode: %s, BankCode: %s, PayDate: %s",
                        responseCode, bankCode, payDate
                ));

                // Xác định trạng thái
                if ("00".equals(responseCode)) {
                    lichSuReturn.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);

                    // GỌI HÀM XỬ LÝ THANH TOÁN THÀNH CÔNG
                    try {
                        xuLyThanhToanThanhCongVNPay(invoice, transactionNo, bankCode);
                    } catch (Exception e) {
                        logger.error("Lỗi khi xử lý thanh toán thành công: ", e);
                    }

                    response.setCode("00");
                    response.setMessage("Thanh toán thành công");

                } else {
                    lichSuReturn.setTrangThaiThanhToan(TrangThaiThanhToan.THANH_TOAN_THAT_BAI);

                    // Cập nhật trạng thái hóa đơn
                    invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
                    invoiceRepository.save(invoice);

                    response.setCode(responseCode);
                    response.setMessage("Thanh toán thất bại - Mã lỗi: " + responseCode);
                }

                // Lưu lịch sử return
                lichSuThanhToanRepository.save(lichSuReturn);

                // Tìm và cập nhật lịch sử create
                Optional<LichSuThanhToan> lichSuCreateOpt = lichSuThanhToanRepository
                        .findFirstByHoaDonIdAndLoaiGiaoDichOrderByThoiGianDesc(
                                invoice.getId(), "VNPAY_CREATE");

                lichSuCreateOpt.ifPresent(lichSu -> {
                    lichSu.setTrangThaiThanhToan(lichSuReturn.getTrangThaiThanhToan());
                    lichSu.setGhiChu(lichSu.getGhiChu() + " -> Completed");
                    lichSuThanhToanRepository.save(lichSu);
                });

                response.setOrderId(invoiceCode);
                response.setTransactionId(transactionNo);
                response.setAmount(vnpayAmount);
            }

        } catch (Exception e) {
            logger.error("Lỗi xử lý return VNPAY: ", e);
            response.setCode("99");
            response.setMessage("Lỗi xử lý: " + e.getMessage());
        }

        return response;
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

            String responseCode = params.get("vnp_ResponseCode");
            String invoiceCode = params.get("vnp_TxnRef");
            String transactionNo = params.get("vnp_TransactionNo");
            String amount = params.get("vnp_Amount");
            String bankCode = params.get("vnp_BankCode");
            String payDate = params.get("vnp_PayDate");

            logger.info("IPN received - Invoice: {}, ResponseCode: {}, Transaction: {}",
                    invoiceCode, responseCode, transactionNo);

            Optional<Invoice> invoiceOpt = invoiceRepository.findByCode(invoiceCode);
            if (invoiceOpt.isEmpty()) {
                logger.warn("IPN - Không tìm thấy hóa đơn: {}", invoiceCode);
                return new IpnResponse("01", "Invoice not found");
            }

            Invoice invoice = invoiceOpt.get();

            BigDecimal vnpayAmount = amount != null ?
                    new BigDecimal(Long.parseLong(amount) / 100) : BigDecimal.ZERO;
            BigDecimal invoiceAmount = invoice.getTotalAmountAfterDecrease();

            if (vnpayAmount.compareTo(invoiceAmount) != 0) {
                logger.warn("IPN - Số tiền không khớp. Invoice: {}, VNPAY: {}",
                        invoiceAmount, vnpayAmount);
                return new IpnResponse("02", "Amount invalid");
            }

            if (invoice.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                logger.info("IPN - Hóa đơn đã được thanh toán trước đó: {}", invoiceCode);
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
                    responseCode, bankCode, payDate
            ));

            if ("00".equals(responseCode)) {
                lichSuIpn.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);

                try {
                    xuLyThanhToanThanhCongVNPay(invoice, transactionNo, bankCode);
                } catch (Exception e) {
                    logger.error("Lỗi khi xử lý thanh toán thành công từ IPN: ", e);
                }

            } else {
                lichSuIpn.setTrangThaiThanhToan(TrangThaiThanhToan.THANH_TOAN_THAT_BAI);
                invoice.setTrangThaiThanhToan(TrangThaiThanhToan.CHUA_THANH_TOAN);
                invoiceRepository.save(invoice);
            }

            lichSuThanhToanRepository.save(lichSuIpn);

            Optional<LichSuThanhToan> lichSuCreateOpt = lichSuThanhToanRepository
                    .findFirstByHoaDonIdAndLoaiGiaoDichOrderByThoiGianDesc(
                            invoice.getId(), "VNPAY_CREATE");

            lichSuCreateOpt.ifPresent(lichSu -> {
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
    @Transactional
    public void xuLyThanhToanThanhCongVNPay(Invoice hoaDon, String maGiaoDich, String bankCode) throws BadRequestException {
        logger.info("=== BẮT ĐẦU XỬ LÝ THANH TOÁN VNPAY THÀNH CÔNG ===");
        logger.info("Mã hóa đơn: {}, Mã GD: {}, Ngân hàng: {}",
                hoaDon.getCode(), maGiaoDich, bankCode);

        try {
            if (hoaDon.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
                logger.info("Hóa đơn đã được thanh toán trước đó");
                return;
            }

            validateProducts(hoaDon);
            processVoucherForInvoice(hoaDon);
            ganCaLamViecChoHoaDon(hoaDon);

            if (hoaDon.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
                xuLyHoaDonGiaoHangVNPay(hoaDon, maGiaoDich, bankCode);
            } else {
                xuLyHoaDonTaiQuayVNPay(hoaDon, maGiaoDich, bankCode);
            }

            logger.info("=== KẾT THÚC XỬ LÝ THANH TOÁN VNPAY THÀNH CÔNG ===");

        } catch (Exception e) {
            logger.error("Lỗi xử lý thanh toán thành công VNPAY: ", e);
            throw new BadRequestException("Lỗi xử lý thanh toán thành công VNPAY: " + e.getMessage());
        }
    }

    private void validateProducts(Invoice hoaDon) throws BadRequestException {
        logger.info("=== KIỂM TRA SẢN PHẨM ===");

        List<String> idHDCTS = adTaoHoaDonChiTietRepository.getHoaDonChiTiet(hoaDon.getId());

        if (idHDCTS == null || idHDCTS.isEmpty()) {
            throw new BadRequestException("Hóa đơn không có sản phẩm nào");
        }

        for (String idHDCT : idHDCTS) {
            if (idHDCT == null || idHDCT.trim().isEmpty()) {
                continue;
            }

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

            ProductDetail product = productOpt.get();
            InvoiceDetail detail = detailOpt.get();

//            if (product.getSoLuong() < detail.getSoLuong()) {
//                throw new BadRequestException(String.format(
//                        "Sản phẩm %s không đủ số lượng (còn: %d, cần: %d)",
//                        product.getSanPham().getTenSanPham(),
//                        product.getSoLuong(),
//                        detail.getSoLuong()
//                ));
//            }
        }

        logger.info("Kiểm tra sản phẩm thành công!");
    }

    private void processVoucherForInvoice(Invoice invoice) {
        if (invoice.getVoucher() == null) return;

        logger.info("=== XỬ LÝ VOUCHER ===");
        logger.info("Voucher: {}", invoice.getVoucher().getCode());

        Voucher voucher = invoice.getVoucher();

        // Trừ số lượng tồn kho voucher chung
        if (voucher.getRemainingQuantity() > 0) {
            voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
            adVoucherRepository.save(voucher);
            logger.info("Đã trừ số lượng voucher, còn lại: {}", voucher.getRemainingQuantity());
        }

        // Cập nhật trạng thái Voucher Detail cho khách hàng
        if (invoice.getCustomer() != null) {
            String customerId = invoice.getCustomer().getId();
            String voucherId = voucher.getId();

            Optional<VoucherDetail> voucherDetailOpt =
                    adVoucherDetailRepository.findByVoucherIdAndCustomerId(voucherId, customerId);

            if (voucherDetailOpt.isPresent()) {
                VoucherDetail detail = voucherDetailOpt.get();
                detail.setUsageStatus(true);
                detail.setUsedDate(System.currentTimeMillis()); // Dùng milliseconds
                detail.setInvoiceId(invoice.getCode());
                adVoucherDetailRepository.save(detail);
                logger.info("Đã cập nhật trạng thái đã sử dụng cho khách hàng: {}", customerId);
            }
        }
    }

    private void ganCaLamViecChoHoaDon(Invoice hoaDon) {
        if (hoaDon.getShiftHandover() != null) return;

        try {
            Optional<Staff> staffOpt = adNhanVienRepository.findAll().stream().findFirst();
            if (staffOpt.isPresent() && staffOpt.get().getAccount() != null) {
                String accountId = staffOpt.get().getAccount().getId();
                Optional<ShiftHandover> openShift = adShiftHandoverRepository.findOpenShiftByAccountId(accountId);

                if (openShift.isPresent()) {
                    hoaDon.setShiftHandover(openShift.get());
                    logger.info("Đã gán hóa đơn vào ca làm việc: {}", openShift.get().getId());
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi gán ca làm việc: ", e);
        }
    }

    private void xuLyHoaDonGiaoHangVNPay(Invoice invoice, String maGiaoDich, String bankCode) {
        logger.info("=== XỬ LÝ HÓA ĐƠN GIAO HÀNG (VNPAY) ===");

        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.DA_XAC_NHAN);
        invoice.setPaymentDate(System.currentTimeMillis());
        invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        invoice.setPaymentMethod("VNPAY");
        invoice.setTransactionId(maGiaoDich);
        invoice.setBankCode(bankCode);

        Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);

        createStatusHistory(savedInvoice, EntityTrangThaiHoaDon.DA_XAC_NHAN,
                "Hóa đơn giao hàng đã được thanh toán qua VNPAY", null);

        createPaymentHistoryVNPay(savedInvoice, maGiaoDich, bankCode);
        updateProductQuantityAndImei(savedInvoice);

        logger.info("Đã xử lý thành công hóa đơn giao hàng qua VNPAY");
    }

    private void xuLyHoaDonTaiQuayVNPay(Invoice invoice, String maGiaoDich, String bankCode) {
        logger.info("=== XỬ LÝ HÓA ĐƠN TẠI QUẦY (VNPAY) ===");

        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.HOAN_THANH);
        invoice.setPaymentDate(System.currentTimeMillis());
        invoice.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
        invoice.setPaymentMethod("VNPAY");
        invoice.setTransactionId(maGiaoDich);
        invoice.setBankCode(bankCode);

        Invoice savedInvoice = adTaoHoaDonRepository.save(invoice);

        createStatusHistory(savedInvoice, EntityTrangThaiHoaDon.HOAN_THANH,
                "Hóa đơn đã được thanh toán qua VNPAY tại quầy", null);

        createPaymentHistoryVNPay(savedInvoice, maGiaoDich, bankCode);
        updateProductQuantityAndImei(savedInvoice);

        logger.info("Đã xử lý thành công hóa đơn tại quầy qua VNPAY");
    }

    private void updateProductQuantityAndImei(Invoice invoice) {
        try {
            logger.info("=== CẬP NHẬT TỒN KHO VÀ IMEI ===");

            List<InvoiceDetail> invoiceDetails = adHoaDonChiTietRepository.findByInvoiceId(invoice.getId());

            for (InvoiceDetail detail : invoiceDetails) {
                // Cập nhật số lượng tồn kho
//                if (detail.getSanPhamChiTiet() != null) {
//                    ProductDetail productDetail = detail.getSanPhamChiTiet();
//                    productDetail.setSoLuong(productDetail.getSoLuong() - detail.getSoLuong());
//                    adPDProductDetailRepository.save(productDetail);
//                    logger.info("Đã cập nhật tồn kho sản phẩm: {}, SL còn: {}",
//                            productDetail.getSanPham().getTenSanPham(),
//                            productDetail.getSoLuong());
//                }

                // Cập nhật trạng thái IMEI
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

    private void createPaymentHistoryVNPay(Invoice invoice, String maGiaoDich, String bankCode) {
        try {
            logger.info("=== TẠO LỊCH SỬ THANH TOÁN VNPAY ===");

            LichSuThanhToan paymentHistory = new LichSuThanhToan();
            paymentHistory.setHoaDon(invoice);
            paymentHistory.setSoTien(invoice.getTotalAmountAfterDecrease());
            paymentHistory.setLoaiGiaoDich("VNPAY");
            paymentHistory.setThoiGian(LocalDateTime.now());
            paymentHistory.setMaGiaoDich(maGiaoDich);
            paymentHistory.setTrangThaiThanhToan(TrangThaiThanhToan.DA_THANH_TOAN);
            paymentHistory.setGhiChu("Thanh toán VNPAY - Ngân hàng: " + bankCode);

            lichSuThanhToanRepository.save(paymentHistory);

            logger.info("Đã tạo lịch sử thanh toán: Mã GD={}, Số tiền={}, Ngân hàng={}",
                    maGiaoDich, paymentHistory.getSoTien(), bankCode);

        } catch (Exception e) {
            logger.error("Lỗi tạo lịch sử thanh toán: ", e);
        }
    }

    private String buildPaymentUrl(PaymentRequest request, String ipAddr, String maGiaoDich) throws Exception {
        Map<String, String> params = new TreeMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        long amountInCents = request.getTotalAmountAfterDecrease().multiply(BigDecimal.valueOf(100)).longValue();
        params.put("vnp_Amount", String.valueOf(amountInCents));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", request.getOrderId());

        String orderInfo = request.getCode() != null ?
                request.getCode() : "Thanh toan hoa don " + request.getOrderId();
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", request.getOrderType());
        params.put("vnp_Locale", request.getLanguage());

        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", ipAddr);

        String createDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        params.put("vnp_CreateDate", createDate);

        if (request.getCustomerName() != null && !request.getCustomerName().isEmpty()) {
            params.put("vnp_Bill_FirstName", request.getCustomerName());
        }
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().isEmpty()) {
            params.put("vnp_Bill_Phone", request.getCustomerPhone());
        }
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isEmpty()) {
            params.put("vnp_Bill_Email", request.getCustomerEmail());
        }
        if (request.getCustomerAddress() != null && !request.getCustomerAddress().isEmpty()) {
            params.put("vnp_Bill_Address", request.getCustomerAddress());
        }

        params.put("vnp_TransactionNo", maGiaoDich);

        StringBuilder hashData = new StringBuilder();
        StringBuilder queryBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value != null && !value.isEmpty()) {
                hashData.append(key).append("=").append(value).append("&");
                queryBuilder.append(key).append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()))
                        .append("&");
            }
        }

        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
        }
        if (queryBuilder.length() > 0) {
            queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        }

        String secureHash = VnpayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

        return vnPayConfig.getPayUrl() + "?" + queryBuilder + "&vnp_SecureHash=" + secureHash;
    }

    private boolean validateSignature(Map<String, String> params) {
        try {
            String secureHash = params.get("vnp_SecureHash");

            // KIỂM TRA NULL
            if (secureHash == null || secureHash.isEmpty()) {
                logger.error("secureHash is null or empty");
                return false;
            }

            // Tạo bản sao để không ảnh hưởng params gốc
            Map<String, String> paramsCopy = new HashMap<>(params);
            paramsCopy.remove("vnp_SecureHash");
            paramsCopy.remove("vnp_SecureHashType");

            List<String> fieldNames = new ArrayList<>(paramsCopy.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = paramsCopy.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append("=").append(fieldValue).append("&");
                }
            }

            if (hashData.length() > 0) {
                hashData.deleteCharAt(hashData.length() - 1);
            }

            String computedHash = VnpayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

            logger.debug("secureHash: {}", secureHash);
            logger.debug("computedHash: {}", computedHash);

            return secureHash.equals(computedHash);

        } catch (Exception e) {
            logger.error("Lỗi validate signature: ", e);
            return false;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip) || "localhost".equals(ip)) {
            ip = "127.0.0.1";
        }

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
        lichSu.setTrangThaiThanhToan(TrangThaiThanhToan.CHO_THANH_TOAN_VNPAY);

        // Cắt ngắn URL nếu cần
        String shortUrl = paymentUrl != null && paymentUrl.length() > 200 ?
                paymentUrl.substring(0, 200) + "..." : paymentUrl;
        lichSu.setGhiChu("Tạo thanh toán VNPAY - URL: " + shortUrl);

        lichSuThanhToanRepository.save(lichSu);
    }
}