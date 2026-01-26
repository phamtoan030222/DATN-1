package com.sd20201.datn.core.admin.hoadon.service.impl;

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
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import com.sd20201.datn.repository.LichSuThanhToanResposiotry;
import com.sd20201.datn.repository.LichSuTrangThaiHoaDonRepository;
import com.sd20201.datn.repository.StaffRepository;
import com.sd20201.datn.utils.EmailService;
import com.sd20201.datn.utils.Helper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ADHoaDonServiceImpl implements ADHoaDonService {

    public final ADInvoiceRepository adHoaDonRepository;

    public final ADHoaDonChiTietRepository adHoaDonChiTietRepository;

    public final LichSuTrangThaiHoaDonRepository lichSuTrangThaiRepository;

    public final StaffRepository staffRepository;


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
            Page<ADHoaDonChiTietResponseDetail> page = adHoaDonChiTietRepository.getAllHoaDonChiTietResponse(request.getMaHoaDon(), pageable);
            return new ResponseObject<>(
                    page,
                    HttpStatus.OK,
                    "Lấy danh sách chi tiết hóa đơn thành công"
            );
        } catch (Exception e) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy chi tiết hóa đơn: " + e.getMessage()
            );
        }
    }

//    @Override
//    @Transactional
//    public ResponseObject<?> changeStatus(ADChangeStatusRequest adChangeStatusRequest) {
//        try {
//            // 1. Tìm hóa đơn theo mã
//            Invoice invoice = (Invoice) adHoaDonRepository.findByCode(adChangeStatusRequest.getMaHoaDon())
//                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với mã: " + adChangeStatusRequest.getMaHoaDon()));
//
//            // 2. Cập nhật trạng thái hóa đơn
//            invoice.setEntityTrangThaiHoaDon(adChangeStatusRequest.getStatus());
//            invoice.setLastModifiedDate(System.currentTimeMillis());
//
//            Invoice updatedInvoice = adHoaDonRepository.save(invoice);
//
//            // 3. Tạo lịch sử trạng thái
//            LichSuTrangThaiHoaDon statusHistory = new LichSuTrangThaiHoaDon();
//            statusHistory.setHoaDon(updatedInvoice);
//            statusHistory.setTrangThai(adChangeStatusRequest.getStatus());
//            statusHistory.setThoiGian(LocalDateTime.now());
//            statusHistory.setNote(adChangeStatusRequest.getNote());
//            lichSuTrangThaiRepository.save(statusHistory);
//
//            System.out.println("Cập nhật trạng thái hóa đơn: " + updatedInvoice.getCode() + " -> " + adChangeStatusRequest.getStatus());
//
//            // 4. Xử lý đặc biệt cho trạng thái HỦY
//            if (adChangeStatusRequest.getStatus() == EntityTrangThaiHoaDon.DA_HUY) {
//                handleCancelledInvoice(updatedInvoice);
//            }
//
//            if (adChangeStatusRequest.getStatus() == EntityTrangThaiHoaDon.DA_XAC_NHAN) {
//                handleCancelledInvoice(updatedInvoice);
//            }
//
//            // Xử lý đặc biệt cho trạng thái HOÀN THÀNH
//            else if (adChangeStatusRequest.getStatus() == EntityTrangThaiHoaDon.HOAN_THANH) {
//                handleCompletedInvoice(updatedInvoice);
//            }
//            // Xử lý đặc biệt cho trạng thái ĐANG GIAO
//            else if (adChangeStatusRequest.getStatus() == EntityTrangThaiHoaDon.DANG_GIAO) {
//                handleShippingInvoice(updatedInvoice);
//            }
//
//            // 5. Gửi email thông báo (nếu có email)
//            sendStatusUpdateEmail(updatedInvoice, adChangeStatusRequest.getStatus());
//
//            // 6. Trả về response
//            Map<String, Object> responseData = new HashMap<>();
//            responseData.put("id", updatedInvoice.getId());
//            responseData.put("maHoaDon", updatedInvoice.getCode());
//            responseData.put("tenHoaDon", updatedInvoice.getName());
//            responseData.put("trangThai", updatedInvoice.getEntityTrangThaiHoaDon());
//            responseData.put("loaiHoaDon", updatedInvoice.getTypeInvoice());
//            responseData.put("ngayCapNhat", updatedInvoice.getLastModifiedDate());
//
//            return new ResponseObject<>(
//                    responseData,
//                    HttpStatus.OK,
//                    "Cập nhật trạng thái thành công"
//            );
//
//        } catch (Exception e) {
//            log.error("Lỗi khi cập nhật trạng thái hóa đơn: ", e);
//            return new ResponseObject<>(
//                    null,
//                    HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Lỗi khi cập nhật trạng thái: " + e.getMessage()
//            );
//        }
//    }
//
//    private void handleCancelledInvoice(Invoice invoice) {
//        System.out.println("Xử lý hóa đơn bị hủy: " + invoice.getCode());
//
//        try {
//            // 1. Hoàn trả số lượng sản phẩm (nếu là laptop)
//            Invoice invoiceDetails = adHoaDonRepository.findByInvoice(invoice.getId());
//
//            if (invoiceDetails != null && !invoiceDetails.isEmpty()) {
//                for (Optional<InvoiceDetail> detail : invoiceDetails) {
//                    if (detail.getProductDetail() != null) {
//                        // Đối với laptop, có thể không có số lượng tồn kho trực tiếp
//                        // Tùy thuộc vào logic quản lý kho của bạn
//                        System.out.println("Chi tiết hóa đơn: " + detail.getId() +
//                                ", Sản phẩm: " + detail.getProductDetail().getProduct().getName());
//
//                        // Nếu có IMEI, đánh dấu IMEI là chưa bán
//                        if (detail.getImeis() != null && !detail.getImeis().isEmpty()) {
//                            for (IMEI imei : detail.getImeis()) {
//                                imei.setInvoiceDetail(null); // Bỏ liên kết với hóa đơn
//                                imei.setStatus(IMEIStatus.AVAILABLE); // Đánh dấu có sẵn
//                                imeiRepository.save(imei);
//                                System.out.println("Đã hoàn trả IMEI: " + imei.getCode());
//                            }
//                        }
//                    }
//                }
//            }
//
//            // 2. Hoàn trả voucher (nếu có)
//            if (invoice.getVoucher() != null) {
//                Voucher voucher = invoice.getVoucher();
//                if (voucher.getRemainingQuantity() != null) {
//                    voucher.setRemainingQuantity(voucher.getRemainingQuantity() + 1);
//                    voucherRepository.save(voucher);
//                    System.out.println("Đã hoàn trả voucher: " + voucher.getCode());
//                }
//
//                // Cập nhật trạng thái sử dụng trong voucher detail
//                VoucherDetail voucherDetail = voucherDetailRepository.findByInvoiceId(invoice.getId());
//                if (voucherDetail != null) {
//                    voucherDetail.setUsageStatus(false); // Đánh dấu chưa sử dụng
//                    voucherDetail.setUsedDate(null);
//                    voucherDetail.setInvoiceId(null);
//                    voucherDetailRepository.save(voucherDetail);
//                }
//            }
//
//            // 3. Hủy đơn vận chuyển (nếu có)
//            if (invoice.getShippingMethod() != null && invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
//                // Tìm và hủy đơn vận chuyển
//                ShippingOrder shippingOrder = shippingOrderRepository.findByInvoiceId(invoice.getId());
//                if (shippingOrder != null) {
//                    shippingOrder.setStatus(ShippingStatus.CANCELLED);
//                    shippingOrderRepository.save(shippingOrder);
//                    System.out.println("Đã hủy đơn vận chuyển: " + shippingOrder.getCode());
//                }
//            }
//
//            // 4. Tạo lịch sử hoàn tiền (nếu đã thanh toán)
//            if (invoice.getTotalAmountAfterDecrease() != null &&
//                    invoice.getTotalAmountAfterDecrease().compareTo(BigDecimal.ZERO) > 0) {
//
//                LichSuThanhToan refundHistory = new LichSuThanhToan();
//                refundHistory.setHoaDon(invoice);
//                refundHistory.setSoTien(invoice.getTotalAmountAfterDecrease().negate()); // Số âm = hoàn tiền
//                refundHistory.setThoiGian(LocalDateTime.now());
//                refundHistory.setMaGiaoDich("REFUND-" + invoice.getCode());
//                refundHistory.setLoaiGiaoDich("HOAN_TIEN");
//                refundHistory.setGhiChu("Hoàn tiền do hủy đơn hàng");
//
//                // Lấy nhân viên hiện tại (có thể từ SecurityContext)
//                Staff currentStaff = getCurrentStaff();
//                if (currentStaff != null) {
//                    refundHistory.setNhanVien(currentStaff);
//                }
//
//                lichSuThanhToanRepository.save(refundHistory);
//                System.out.println("Đã tạo lịch sử hoàn tiền cho hóa đơn: " + invoice.getCode());
//            }
//
//        } catch (Exception e) {
//            System.err.println("Lỗi khi xử lý hóa đơn hủy: " + e.getMessage());
//            // Không throw để tránh rollback transaction chính
//        }
//    }
//
//    private void handleCompletedInvoice(Invoice invoice) {
//        System.out.println("Xử lý hóa đơn hoàn thành: " + invoice.getCode());
//
//        try {
//            // 1. Cập nhật thông tin thanh toán cuối cùng
//            if (invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
//                // Đánh dấu đã giao hàng thành công
//                ShippingOrder shippingOrder = shippingOrderRepository.findByInvoiceId(invoice.getId());
//                if (shippingOrder != null) {
//                    shippingOrder.setStatus(ShippingStatus.DELIVERED);
//                    shippingOrder.setActualDeliveryDate(System.currentTimeMillis());
//                    shippingOrderRepository.save(shippingOrder);
//                }
//            }
//
//            // 2. Cập nhật thống kê doanh thu
//            updateRevenueStatistics(invoice);
//
//            // 3. Đánh dấu IMEI đã bán (nếu có)
//            List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findByInvoiceId(invoice.getId());
//            if (invoiceDetails != null) {
//                for (InvoiceDetail detail : invoiceDetails) {
//                    if (detail.getImeis() != null) {
//                        for (IMEI imei : detail.getImeis()) {
//                            imei.setStatus(IMEIStatus.SOLD);
//                            imeiRepository.save(imei);
//                        }
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            System.err.println("Lỗi khi xử lý hóa đơn hoàn thành: " + e.getMessage());
//        }
//    }
//
//    private void handleShippingInvoice(Invoice invoice) {
//        System.out.println("Xử lý hóa đơn đang giao: " + invoice.getCode());
//
//        try {
//            // Tạo hoặc cập nhật thông tin vận chuyển
//            if (invoice.getTypeInvoice() == TypeInvoice.GIAO_HANG) {
//                ShippingOrder shippingOrder = shippingOrderRepository.findByInvoiceId(invoice.getId());
//
//                if (shippingOrder == null) {
//                    // Tạo mới đơn vận chuyển
//                    shippingOrder = new ShippingOrder();
//                    shippingOrder.setCode("SO-" + invoice.getCode());
//                    shippingOrder.setInvoice(invoice);
//                    shippingOrder.setShippingAddress(invoice.getAddressReceiver());
//                    shippingOrder.setReceiverName(invoice.getNameReceiver());
//                    shippingOrder.setReceiverPhone(invoice.getPhoneReceiver());
//                    shippingOrder.setShippingFee(invoice.getShippingFee());
//                    shippingOrder.setStatus(ShippingStatus.PICKED_UP);
//                    shippingOrder.setEstimatedDeliveryDate(invoice.getEstimatedDeliveryDate());
//                } else {
//                    shippingOrder.setStatus(ShippingStatus.IN_TRANSIT);
//                }
//
//                shippingOrderRepository.save(shippingOrder);
//
//                // Cập nhật tracking code (nếu có dịch vụ vận chuyển bên thứ 3)
//                String trackingCode = generateTrackingCode();
//                shippingOrder.setTrackingCode(trackingCode);
//                shippingOrderRepository.save(shippingOrder);
//
//                System.out.println("Đã tạo tracking code: " + trackingCode);
//            }
//
//        } catch (Exception e) {
//            System.err.println("Lỗi khi xử lý hóa đơn đang giao: " + e.getMessage());
//        }
//    }

    private void sendStatusUpdateEmail(Invoice invoice, EntityTrangThaiHoaDon newStatus) {
        try {
            Customer customer = invoice.getCustomer();
            if (customer == null || customer.getEmail() == null) {
                System.out.println("Không có email khách hàng, bỏ qua gửi email");
                return;
            }

            String email = customer.getEmail();
            String subject = getEmailSubject(newStatus);
            String content = buildEmailContent(invoice, newStatus);

            // Gửi email bất đồng bộ
            CompletableFuture.runAsync(() -> {
                try {
                    EmailService.sendEmail(email, subject, content);
                    System.out.println("Đã gửi email thông báo đến: " + email);
                } catch (Exception e) {
                    System.err.println("Lỗi khi gửi email: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("Lỗi khi chuẩn bị gửi email: " + e.getMessage());
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

    private Staff getCurrentStaff() {
        // Lấy thông tin nhân viên từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return (Staff) staffRepository.findByAccountUsername(username).orElse(null);
        }
        return null;
    }

    private String generateTrackingCode() {
        return "MLT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void updateRevenueStatistics(Invoice invoice) {
        // Cập nhật thống kê doanh thu, best seller, v.v.
        // Tùy thuộc vào yêu cầu business của bạn
    }

    @Override
    public ResponseObject<?> getAllHoaDonCT1(ADHoaDonDetailRequest request) {
        try {
            List<ADHoaDonChiTietResponseDetail> page = adHoaDonChiTietRepository.getAllHoaDonChiTietResponse2(request.getMaHoaDon());
            return new ResponseObject<>(
                    page,
                    HttpStatus.OK,
                    "Lấy danh sách chi tiết hóa đơn thành công"
            );
        } catch (Exception e) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi lấy chi tiết hóa đơn: " + e.getMessage()
            );
        }
    }

}
