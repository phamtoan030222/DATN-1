package com.sd20201.datn.core.client.banhang.service.impl;

import com.sd20201.datn.core.admin.customer.repository.AdCustomerRepository;
import com.sd20201.datn.core.admin.products.productdetail.model.request.ADPDProductDetailRequest;
import com.sd20201.datn.core.admin.products.productdetail.repository.ADPDProductDetailRepository;
import com.sd20201.datn.core.admin.staff.repository.ADStaffRepository;
import com.sd20201.datn.core.client.banhang.model.request.ClientChonPhieuGiamGiaRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientListKhachHangRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientNhanVienRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientProductItemRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThanhToanRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThemKhachHangRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientThemSanPhamRequest;
import com.sd20201.datn.core.client.banhang.model.request.ClientVoucherSuggestionRequest;
import com.sd20201.datn.core.client.banhang.model.response.ClientApplicableVoucherResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientBetterVoucherResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientChonKhachHangResponse;
import com.sd20201.datn.core.client.banhang.model.response.ClientListHoaDon;
import com.sd20201.datn.core.client.banhang.model.response.ClientPhuongThucThanhToanRespones;
import com.sd20201.datn.core.client.banhang.model.response.ClientVoucherSuggestionResponse;
import com.sd20201.datn.core.client.banhang.repository.ClientBHVoucherDetailRepository;
import com.sd20201.datn.core.client.banhang.repository.ClientBanHangCartItemRepository;
import com.sd20201.datn.core.client.banhang.repository.ClientBanHangCartRepository;
import com.sd20201.datn.core.client.banhang.repository.ClientBanHangIMEIRepository;
import com.sd20201.datn.core.client.banhang.repository.ClientBanHangSanPhamChiTiet;
import com.sd20201.datn.core.client.banhang.repository.ClientTaoHoaDonChiTietRepository;
import com.sd20201.datn.core.client.banhang.repository.ClientTaoHoaDonRepository;
import com.sd20201.datn.core.client.banhang.service.ClientBanHangService;
import com.sd20201.datn.core.client.voucher.repository.ClientVoucherRepository;
import com.sd20201.datn.core.common.base.PageableObject;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Cart;
import com.sd20201.datn.entity.CartItem;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.IMEI;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.InvoiceDetail;
import com.sd20201.datn.entity.LichSuThanhToan;
import com.sd20201.datn.entity.LichSuTrangThaiHoaDon;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.entity.Staff;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.ImeiStatus;
import com.sd20201.datn.infrastructure.constant.TargetType;
import com.sd20201.datn.infrastructure.constant.TypeInvoice;
import com.sd20201.datn.infrastructure.constant.TypePayment;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import com.sd20201.datn.infrastructure.exception.BusinessException;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import com.sd20201.datn.repository.InvoiceRepository;
import com.sd20201.datn.repository.LichSuThanhToanRepository;
import com.sd20201.datn.repository.LichSuTrangThaiHoaDonRepository;
import com.sd20201.datn.utils.Helper;
import com.sd20201.datn.utils.UserContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientBanHangServiceImpl implements ClientBanHangService {

    private final ClientTaoHoaDonRepository adTaoHoaDonRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final ClientTaoHoaDonChiTietRepository adTaoHoaDonChiTietRepository;
    private final ClientBanHangIMEIRepository imeiRepository;
    private final ADStaffRepository adNhanVienRepository;
    public final ClientVoucherRepository adVoucherRepository;
    public final ADPDProductDetailRepository adPDProductDetailRepository;
    public final LichSuThanhToanRepository adLichSuThanhToanRepository;
    public final ClientBanHangSanPhamChiTiet productDetailRepository;
    public final ClientBHVoucherDetailRepository adbhvoucher;
    public final AdCustomerRepository khachHangRepository;
    public final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;

    private final UserContextHelper userContextHelper;

    private final ClientBanHangCartRepository cartRepository;

    private final ClientBanHangCartItemRepository cartItemRepository;

    private final CustomerRepository customerRepository;

    @Override
    public List<ClientListHoaDon> getHoaDon() {
        return adTaoHoaDonRepository.getAll();
    }


    @Transactional
    @Override
    public ResponseObject<?> createOrder(ClientThanhToanRequest request) {
        try {
            // 1. Tạo Invoice (Hóa đơn)
            Invoice invoice = new Invoice();

            // Gen mã hóa đơn: HD + Timestamp (hoặc logic riêng của bạn)
            invoice.setCode("ONLINE_" + System.currentTimeMillis());

            // Set thông tin khách hàng
            invoice.setNameReceiver(request.getTen());
            invoice.setPhoneReceiver(request.getSdt());
            invoice.setAddressReceiver(request.getDiaChi());
            invoice.setDescription(request.getGhiChu());
            invoice.setCustomer(
                    userContextHelper.getCurrentUserId()
                            .flatMap(customerRepository::findById)
                            .orElse(null)
            );

            // Set tiền (Lấy từ request hoặc tính lại nếu cần bảo mật cao)
            invoice.setTotalAmount(request.getTienHang());
            invoice.setShippingFee(request.getTienShip());
//            invoice.setDiscountAmount(request.getGiamGia());
            invoice.setTotalAmountAfterDecrease(request.getTongTien());

            // Set Loại & Trạng thái
            if ("TAI_QUAY".equals(request.getLoaiHoaDon())) {
                invoice.setTypeInvoice(TypeInvoice.TAI_QUAY); // Enum 0
            } else {
                invoice.setTypeInvoice(TypeInvoice.ONLINE); // Enum 1 (Giao hàng)
            }
            invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN); // Mới đặt -> Chờ xác nhận
            invoice.setCreatedDate(System.currentTimeMillis());

            // Lưu hóa đơn trước để có ID
            invoice = invoiceRepository.save(invoice);

            for (ClientProductItemRequest item : request.getProducts()) {

                // a. Tìm thông tin sản phẩm trong DB
                ProductDetail productDetail = productDetailRepository.findById(item.getProductDetailId())
                        .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại: " + item.getProductDetailId()));

                // d. Tạo Hóa đơn chi tiết
                InvoiceDetail detail = new InvoiceDetail();
                detail.setInvoice(invoice);
                detail.setProductDetail(productDetail);
                detail.setQuantity(item.getQuantity());
                detail.setPrice(item.getPrice()); // Giá bán tại thời điểm đặt
                // Tính tổng tiền dòng này (để lưu DB cho chắc)
                detail.setTotalAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

                detail = invoiceDetailRepository.saveAndFlush(detail);

                List<String> imeiIds = imeiRepository.findIdsAvailableImei(
                        item.getProductDetailId(),
                        ImeiStatus.AVAILABLE,
                        PageRequest.of(0, item.getQuantity())
                );

                imeiRepository.updateImeiStatusIdIn(imeiIds, ImeiStatus.RESERVED, detail.getId());
            }

            // 3. Ghi log lịch sử trạng thái
            LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
            history.setHoaDon(invoice);
            history.setTrangThai(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
            history.setThoiGian(LocalDateTime.now());
            history.setNote("Khách đặt hàng Online mới");
            lichSuTrangThaiHoaDonRepository.save(history);

            return ResponseObject.successForward(invoice, "Đặt hàng thành công");

        } catch (Exception e) {
            log.error("Lỗi đặt hàng: ", e);
            throw new BusinessException(e.getMessage()); // Throw ra để Controller bắt lỗi trả về 400
        }
    }

    @Override
    public ResponseObject<?> getProductDetails(ADPDProductDetailRequest request) {
        return ResponseObject.successForward(
                PageableObject.of(
                        productDetailRepository.getProductDetails(Helper.createPageable(request), request)),
                "OKE"
        );
    }

    @Override
    @Transactional
    public ResponseObject<?> createHoaDon(ClientNhanVienRequest adNhanVienRequest) {
        try {
            Invoice hoaDon = new Invoice();
            hoaDon.setTotalAmount(BigDecimal.ZERO);
            hoaDon.setTotalAmountAfterDecrease(BigDecimal.ZERO);

            String code = adNhanVienRequest.getMa();
            if (code == null || code.isEmpty()) {
                code = "HD" + System.currentTimeMillis();
            }
            hoaDon.setCode(code);

            hoaDon.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
            hoaDon.setTypeInvoice(TypeInvoice.TAI_QUAY);
            hoaDon.setCreatedDate(System.currentTimeMillis());

            adTaoHoaDonRepository.save(hoaDon);

            LichSuTrangThaiHoaDon lichSu = new LichSuTrangThaiHoaDon();
            lichSu.setThoiGian(LocalDateTime.now());
            lichSu.setNote("Đơn hàng mới được khởi tạo.");
            lichSu.setHoaDon(hoaDon);
            lichSu.setTrangThai(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
            lichSuTrangThaiHoaDonRepository.save(lichSu);

            return new ResponseObject<>(hoaDon, HttpStatus.CREATED, "Tạo hóa đơn thành công");

        } catch (Exception e) {
            log.error("Error creating invoice: ", e);
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tạo hóa đơn: " + e.getMessage());
        }
    }

    @Override
    public ResponseObject<?> getListGioHang(String id) {
        Long currentTime = System.currentTimeMillis();
        return ResponseObject.successForward(
                cartItemRepository.getCartItemsById(id, currentTime),
                "OKE"
        );
    }

    @Override
    @Transactional
    public ResponseObject<?> xoaSanPhamKhoiGioHang(String idHoaDonChiTiet) {
        try {
            InvoiceDetail detail = invoiceDetailRepository.findById(idHoaDonChiTiet)
                    .orElseThrow(() -> new BadRequestException("Sản phẩm không tồn tại trong giỏ"));

            List<IMEI> imeis = imeiRepository.findByInvoiceDetail(detail);

            if (imeis != null && !imeis.isEmpty()) {
                for (IMEI imei : imeis) {
                    imei.setImeiStatus(ImeiStatus.AVAILABLE);
                    imei.setInvoiceDetail(null);
                }
                imeiRepository.saveAll(imeis);
            }

            invoiceDetailRepository.delete(detail);

            return new ResponseObject<>(null, HttpStatus.OK, "Xóa sản phẩm thành công");
        } catch (Exception e) {
            return new ResponseObject<>(null, HttpStatus.BAD_REQUEST, "Lỗi xóa sản phẩm: " + e.getMessage());
        }
    }

    @Override
    public ResponseObject<?> listKhachHang(ClientListKhachHangRequest listKhachHangRequest) {
        try {
            Pageable pageable = Helper.createPageable(listKhachHangRequest, "createdDate");
            Page<ClientChonKhachHangResponse> page = adTaoHoaDonChiTietRepository.getAllList(listKhachHangRequest, pageable);
            return new ResponseObject<>(PageableObject.of(page), HttpStatus.OK, "Lấy danh sách khách hàng thành công");
        } catch (Exception e) {
            return new ResponseObject<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi lấy danh sách khách hàng");
        }
    }

    @Override
    public ClientChonKhachHangResponse getKhachHang(String id) {
        return adTaoHoaDonChiTietRepository.getKhachHang(id);
    }

    @Override
    public List<ClientPhuongThucThanhToanRespones> getPhuongThucThanhToan(String id) {
        return adTaoHoaDonChiTietRepository.getPhuongThucThanhToan(id);
    }

    @Override
    @Transactional
    public ResponseObject<?> createThemSanPham(ClientThemSanPhamRequest request) {

        // 1. Tìm hóa đơn. Nếu không thấy -> Tự tạo mới luôn!
        Cart cart = Optional.ofNullable(request.getCartId())
                .flatMap(cartRepository::findById)
                .orElseGet(() -> {
                    Cart newCart = new Cart();

                    newCart.setCustomer(
                            userContextHelper.getCurrentUserId().flatMap(customerRepository::findById)
                                    .orElse(null));
                    cartRepository.save(newCart);
                    return newCart;
                });

        // 2. Tìm sản phẩm
        ProductDetail productDetail = productDetailRepository.findById(request.getProductDetailId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm"));

        // 4. Xử lý InvoiceDetail (Giữ nguyên logic của bạn)
        CartItem cartItem = cartItemRepository.findByCartAndProductDetail(cart, productDetail)
                .orElseGet(() -> {
                    CartItem newCartItem = new CartItem();
                    newCartItem.setCart(cart);
                    newCartItem.setProductDetail(productDetail);

                    return newCartItem;
                });
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return new ResponseObject<>(cartItem.getId(), HttpStatus.OK, "Thêm sản phẩm thành công");
    }

    @Override
    public void themKhachHang(ClientThemKhachHangRequest id) {
        if (id.getIdKH() != null) {
            Customer khachHang = khachHangRepository.findById(id.getIdKH()).orElse(null);
            Invoice hoaDon = adTaoHoaDonRepository.findById(id.getIdHD()).orElse(null);
            if (hoaDon != null) {
                hoaDon.setCustomer(khachHang);
                adTaoHoaDonRepository.save(hoaDon);
            }
        }
    }

    @Override
    public ResponseObject<?> thanhToanThanhCong(ClientThanhToanRequest id) throws BadRequestException {

        // 1. Validate ID Hóa đơn
        if (id.getIdHD() == null || id.getIdHD().trim().isEmpty()) {
            throw new BadRequestException("ID hóa đơn không được để trống");
        }

        Invoice hoaDonToUpdate = adTaoHoaDonRepository.findById(id.getIdHD())
                .orElseThrow(() -> new BadRequestException("Hóa đơn không tồn tại"));

        // 2. Xử lý Voucher (Nếu có)
        if (id.getIdPGG() != null && !id.getIdPGG().trim().isEmpty()) {
            try {
                Voucher voucher = adVoucherRepository.findById(id.getIdPGG()).orElse(null);
                if (voucher != null && voucher.getRemainingQuantity() > 0) {
                    voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
                    adVoucherRepository.save(voucher);
                    hoaDonToUpdate.setVoucher(voucher);
                }
            } catch (Exception e) {
                log.error("Lỗi voucher", e); // Không throw lỗi để đơn hàng vẫn tiếp tục
            }
        }

        try {
            // 3. Cập nhật thông tin cơ bản & TIỀN
            if (id.getTen() != null) hoaDonToUpdate.setNameReceiver(id.getTen());
            if (id.getSdt() != null) hoaDonToUpdate.setPhoneReceiver(id.getSdt());
            if (id.getDiaChi() != null) hoaDonToUpdate.setAddressReceiver(id.getDiaChi());
//            if (id.getGhiChu() != null) hoaDonToUpdate.setNote(id.getGhiChu());

            // [QUAN TRỌNG] Cập nhật tiền
            // Sử dụng tienHang (subTotal) nếu có, nếu null thì mặc định 0
            BigDecimal tienHang = id.getTienHang() != null ? id.getTienHang() : BigDecimal.ZERO;
            BigDecimal tienShip = id.getTienShip() != null ? id.getTienShip() : BigDecimal.ZERO;
            BigDecimal tongTienCuoi = id.getTongTien() != null ? id.getTongTien() : BigDecimal.ZERO;

            hoaDonToUpdate.setTotalAmount(tienHang); // Tổng tiền hàng
            hoaDonToUpdate.setShippingFee(tienShip); // Phí ship
            hoaDonToUpdate.setTotalAmountAfterDecrease(tongTienCuoi); // Khách phải trả
            hoaDonToUpdate.setPaymentDate(System.currentTimeMillis());


            hoaDonToUpdate.setTypeInvoice(TypeInvoice.ONLINE);


            // 6. CHỐT ĐƠN: Luôn set trạng thái CHỜ XÁC NHẬN với đơn Online
            hoaDonToUpdate.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
            adTaoHoaDonRepository.save(hoaDonToUpdate);

            // 7. Ghi lịch sử
            createStatusHistory(hoaDonToUpdate, EntityTrangThaiHoaDon.CHO_XAC_NHAN,
                    "Khách đặt đơn Online (" + (id.getLoaiHoaDon().equals("GIAO_HANG") ? "Giao tận nơi" : "Nhận tại cửa hàng") + ")");

            // 8. Ghi lịch sử thanh toán (Nếu là chuyển khoản)
            if ("1".equals(id.getPhuongThucThanhToan())) {
                createPaymentHistory(hoaDonToUpdate, id, hoaDonToUpdate.getStaff());
            }

            return new ResponseObject<>(null, HttpStatus.OK, "Đặt hàng thành công");

        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("Lỗi xử lý thanh toán: " + e.getMessage());
        }
    }

    private TypePayment determinePaymentMethod(String paymentMethod) {
        if ("1".equals(paymentMethod)) return TypePayment.CHUYEN_KHOAN;
        if ("2".equals(paymentMethod)) return TypePayment.TIEN_MAT_CHUYEN_KHOAN;
        return TypePayment.TIEN_MAT;
    }

    private void updateInvoiceInfo(Invoice invoice, ClientThanhToanRequest request, TypePayment paymentMethod) {
        if (request.getTen() != null) invoice.setNameReceiver(request.getTen());
        if (request.getSdt() != null) invoice.setPhoneReceiver(request.getSdt());
        if (request.getDiaChi() != null) invoice.setAddressReceiver(request.getDiaChi());

        invoice.setShippingFee(request.getTienShip() != null ? request.getTienShip() : BigDecimal.ZERO);
        invoice.setTotalAmount(request.getTienHang() != null ? request.getTienHang() : BigDecimal.ZERO);

        BigDecimal finalAmount = request.getTongTien() != null ? request.getTongTien() : BigDecimal.ZERO;
        invoice.setTotalAmountAfterDecrease(finalAmount);

        invoice.setPaymentDate(System.currentTimeMillis());
    }

    private ResponseObject<?> processDeliveryInvoice(Invoice invoice, ClientThanhToanRequest request, Staff staff) {
        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.CHO_XAC_NHAN);
        adTaoHoaDonRepository.save(invoice);

        createStatusHistory(invoice, EntityTrangThaiHoaDon.CHO_XAC_NHAN, "Đơn hàng online chờ xác nhận.");

        if ("1".equals(request.getPhuongThucThanhToan())) {
            createPaymentHistory(invoice, request, staff);
        }
        return new ResponseObject<>(null, HttpStatus.OK, "Đặt hàng thành công");
    }

    private ResponseObject<?> processCounterInvoice(Invoice invoice, ClientThanhToanRequest request, Staff staff) {
        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.HOAN_THANH);
        adTaoHoaDonRepository.save(invoice);

        createStatusHistory(invoice, EntityTrangThaiHoaDon.HOAN_THANH, "Thanh toán thành công tại quầy.");
        createPaymentHistory(invoice, request, staff);

        return new ResponseObject<>(null, HttpStatus.OK, "Thanh toán thành công");
    }

    private void createStatusHistory(Invoice invoice, EntityTrangThaiHoaDon status, String note) {
        LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
        history.setThoiGian(LocalDateTime.now());
        history.setNote(note);
        history.setHoaDon(invoice);
        history.setTrangThai(status);
        lichSuTrangThaiHoaDonRepository.save(history);
    }

    private void createPaymentHistory(Invoice invoice, ClientThanhToanRequest request, Staff staff) {
        try {
            LichSuThanhToan history = new LichSuThanhToan();
            history.setHoaDon(invoice);
            history.setSoTien(request.getTongTien());
            history.setLoaiGiaoDich(request.getPhuongThucThanhToan());
            history.setThoiGian(LocalDateTime.now());
            history.setNhanVien(staff);
            history.setMaGiaoDich(UUID.randomUUID().toString());
            adLichSuThanhToanRepository.save(history);
        } catch (Exception e) {
            log.error("Lỗi lưu lịch sử thanh toán", e);
        }
    }

    @Override
    public List<Voucher> danhSachPhieuGiamGia1(ClientChonPhieuGiamGiaRequest request) throws BadRequestException {
        BigDecimal tongTien = request.getTongTien();
        if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) return new ArrayList<>();

        List<Voucher> phieuGiamGias = adTaoHoaDonRepository.getPhieuGiamGia(request.getIdKH(), tongTien);
        if (phieuGiamGias == null) return new ArrayList<>();

        return phieuGiamGias;
    }

    @Override
    public ClientVoucherSuggestionResponse goiYVoucher(ClientVoucherSuggestionRequest req) {
        BigDecimal tongTien = req.getTongTien();
        Long now = System.currentTimeMillis();
        List<Voucher> vouchers = new ArrayList<>(adbhvoucher.findVoucherHopLe(TargetType.ALL_CUSTOMERS, now));
        if (req.getCustomerId() != null && !req.getCustomerId().isEmpty()) {
            vouchers.addAll(adbhvoucher.findVoucherRiengCuaKH(req.getCustomerId()));
        }

        List<ClientApplicableVoucherResponse> apDung = new ArrayList<>();
        List<ClientBetterVoucherResponse> totHon = new ArrayList<>();

        for (Voucher v : vouchers) {
            if (tongTien.compareTo(v.getConditions()) >= 0) {
                BigDecimal giam = tinhGiam(v, tongTien);
                apDung.add(new ClientApplicableVoucherResponse(
                        v.getId(), v.getCode(), v.getTypeVoucher(), v.getDiscountValue(),
                        v.getMaxValue(), v.getConditions(), giam, tongTien.subtract(giam)
                ));
            } else {
                BigDecimal canMua = v.getConditions().subtract(tongTien);
            }
        }

        apDung.sort(Comparator.comparing(ClientApplicableVoucherResponse::getGiamGiaThucTe).reversed());

        ClientVoucherSuggestionResponse res = new ClientVoucherSuggestionResponse();
        res.setVoucherApDung(apDung);
        res.setVoucherTotHon(totHon);
        return res;
    }

    private BigDecimal tinhGiam(Voucher v, BigDecimal tongTien) {
        BigDecimal giam = v.getTypeVoucher() == TypeVoucher.PERCENTAGE
                ? tongTien.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100))
                : v.getDiscountValue();
        if (v.getMaxValue() != null) giam = giam.min(v.getMaxValue());
        return giam.min(tongTien);
    }

    @Override
    @Transactional
    public ResponseObject<?> tangSoLuongSanPham(String idHDCT) {
        InvoiceDetail detail = invoiceDetailRepository.findById(idHDCT)
                .orElseThrow(() -> new BusinessException("Không tìm thấy dòng sản phẩm"));

        List<IMEI> availableImeis = imeiRepository.findByProductDetailAndImeiStatus(
                detail.getProductDetail(),
                ImeiStatus.AVAILABLE
        );

        if (availableImeis.isEmpty()) {
            throw new BusinessException("Sản phẩm đã hết hàng trong kho!");
        }

        IMEI imeiToAdd = availableImeis.get(0);

        detail.setQuantity(detail.getQuantity() + 1);
        detail.setTotalAmount(detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));
        invoiceDetailRepository.save(detail);

        imeiToAdd.setInvoiceDetail(detail);
        imeiToAdd.setImeiStatus(ImeiStatus.RESERVED);
        imeiRepository.save(imeiToAdd);

        return new ResponseObject<>(null, HttpStatus.OK, "Đã tăng số lượng");
    }

    @Transactional
    @Override
    public ResponseObject<?> giamSoLuongSanPham(String idHDCT) {
        InvoiceDetail detail = invoiceDetailRepository.findById(idHDCT)
                .orElseThrow(() -> new BusinessException("Không tìm thấy dòng sản phẩm"));

        if (detail.getQuantity() <= 1) {
            return xoaSanPhamKhoiGioHang(idHDCT);
        }

        List<IMEI> reservedImeis = imeiRepository.findByInvoiceDetail(detail);

        if (!reservedImeis.isEmpty()) {
            IMEI imeiToRemove = reservedImeis.get(reservedImeis.size() - 1);

            imeiToRemove.setInvoiceDetail(null);
            imeiToRemove.setImeiStatus(ImeiStatus.AVAILABLE);
            imeiRepository.save(imeiToRemove);

            detail.setQuantity(detail.getQuantity() - 1);
            detail.setTotalAmount(detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));
            invoiceDetailRepository.save(detail);
        }

        return new ResponseObject<>(null, HttpStatus.OK, "Đã giảm số lượng");
    }

    @Override
    public ResponseObject<?> getProductDetailCart(List<String> ids) {
        Long currentTime = System.currentTimeMillis();

        return ResponseObject.successForward(
                productDetailRepository.findProductDetailCartResponseByIdIn(ids, currentTime),
                "OKE"
        );
    }
}