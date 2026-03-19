package com.sd20201.datn.core.client.invoice.service.impl;

import com.sd20201.datn.core.client.invoice.model.request.ClientInvoiceCancelRequest;
import com.sd20201.datn.core.client.invoice.model.request.ClientPutInvoiceDetailRequest;
import com.sd20201.datn.core.client.invoice.model.request.ClientPutReceiverRequest;
import com.sd20201.datn.core.client.invoice.repository.ClientInvoiceRepository;
import com.sd20201.datn.core.client.invoice.service.ClientInvoiceService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.InvoiceDetail;
import com.sd20201.datn.entity.LichSuTrangThaiHoaDon;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.entity.VoucherDetail;
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import com.sd20201.datn.infrastructure.exception.BusinessException;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.repository.InvoiceDetailRepository;
import com.sd20201.datn.repository.LichSuTrangThaiHoaDonRepository;
import com.sd20201.datn.repository.ProductDetailRepository;
import com.sd20201.datn.repository.VoucherDetailRepository;
import com.sd20201.datn.repository.VoucherRepository;
import com.sd20201.datn.utils.UserContextHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientInvoiceServiceImpl implements ClientInvoiceService {

    private final ClientInvoiceRepository invoiceRepository;

    private final UserContextHelper userContextHelper;

    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    private final CustomerRepository customerRepository;

    private final VoucherDetailRepository voucherDetailRepository;

    private final VoucherRepository voucherRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final ProductDetailRepository productDetailRepository;

    @Override
    public ResponseObject<?> getById(String code) {
        Optional<String> customerIdOptional = userContextHelper.getCurrentUserId();

        return invoiceRepository.getInvoiceByCode(code, customerIdOptional.orElse(null))
                .map(invoice -> ResponseObject.successForward(invoice, "Fetch invoice success"))
                .orElse(ResponseObject.errorForward("Fetch invoice failure", HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseObject<?> getHistoryInvoiceById(String idHoaDon) {
        return ResponseObject.successForward(
                invoiceRepository.getInvoiceLichSuTrangThaiHoaDonByIdHoaDon(idHoaDon, userContextHelper.getCurrentUserId().orElse(null)),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getInvoiceDetailsById(List<String> ids) {
        return ResponseObject.successForward(
                invoiceRepository.getInvoiceDetailsByInvoiceId(ids),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> getInvoiceByIdCustomer() {
        return ResponseObject.successForward(
                invoiceRepository.getInvoicesByIdCustomer(userContextHelper.getCurrentUserId().orElse(null)),
                "OKE"
        );
    }

    @Override
    public ResponseObject<?> cancelInvoice(ClientInvoiceCancelRequest request) {
        Optional<Invoice> invoiceOptional = invoiceRepository.findById(request.getId());
        if (invoiceOptional.isEmpty()) return ResponseObject.errorForward("Invoice not found", HttpStatus.NOT_FOUND);

        Invoice invoice = invoiceOptional.get();
        Optional<String> customerIdOptional = userContextHelper.getCurrentUserId();
        if (
                !((invoice.getCustomer() == null && customerIdOptional.isEmpty())
                        ||
                        (invoice.getCustomer() != null
                                && customerIdOptional.isPresent()
                                && invoice.getCustomer().getId().equals(customerIdOptional.get())))
        )
            return ResponseObject.errorForward("Customer not found", HttpStatus.NOT_FOUND);

        if (invoice.getEntityTrangThaiHoaDon() != EntityTrangThaiHoaDon.CHO_XAC_NHAN)
            return ResponseObject.errorForward("Status invoice is not valid", HttpStatus.CONFLICT);

        invoice.setEntityTrangThaiHoaDon(EntityTrangThaiHoaDon.DA_HUY);

        if (!Objects.isNull(invoice.getVoucher())) {
            Voucher voucher = invoice.getVoucher();
            voucher = handleCancelUsingVoucher(voucher);
            voucherRepository.save(voucher);
        }
        invoiceRepository.saveAndFlush(invoice);

        LichSuTrangThaiHoaDon history = new LichSuTrangThaiHoaDon();
        history.setHoaDon(invoice);
        history.setTrangThai(EntityTrangThaiHoaDon.DA_HUY);
        history.setNote(request.getNote());
        history.setThoiGian(LocalDateTime.now());
        history.setCustomer(customerIdOptional.flatMap(customerRepository::findById).orElse(null));

        lichSuTrangThaiHoaDonRepository.save(history);

        return ResponseObject.successForward(invoice.getId(), "Update invoice success");
    }

    private Voucher handleCancelUsingVoucher(Voucher voucher) {
        switch (voucher.getTargetType()) {
            case INDIVIDUAL -> {
                Optional<String> customerIdOptional = userContextHelper.getCurrentUserId();
                if (customerIdOptional.isEmpty()) return voucher;

                Optional<VoucherDetail> voucherDetailOptional = voucherDetailRepository.findByIdVoucherAndIdCustomer(voucher.getId(), customerIdOptional.get());
                if (voucherDetailOptional.isPresent()) {
                    VoucherDetail voucherDetail = voucherDetailOptional.get();

                    voucherDetail.setUsageStatus(false);
                    voucherDetail.setUsedDate(null);

                    voucherDetailRepository.save(voucherDetail);
                }
            }

            case ALL_CUSTOMERS -> {
                voucher.setRemainingQuantity(voucher.getRemainingQuantity() + 1);
            }

            default -> {

            }
        }

        return voucherRepository.save(voucher);
    }

    @Override
    public ResponseObject<?> putReceiver(String id, ClientPutReceiverRequest request) {
        return invoiceRepository.findById(id)
                .map(invoice -> {
                    invoice.setNameReceiver(request.getTenKhachHang());
                    invoice.setAddressReceiver(request.getDiaChi());
                    invoice.setPhoneReceiver(request.getSdtKH());
                    invoice.setEmail(request.getEmail());

                    invoiceRepository.save(invoice);

                    return ResponseObject.successForward(invoice.getId(), "Update invoice success");
                })
                .orElse(ResponseObject.errorForward("Invoice not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    @Override
    public ResponseObject<?> putInvoiceDetails(String id, ClientPutInvoiceDetailRequest request) {
        Optional<Invoice> invoiceOptional = invoiceRepository.findById(id);
        if (invoiceOptional.isEmpty()) return ResponseObject.errorForward("Invoice not found", HttpStatus.NOT_FOUND);

        Invoice invoice = invoiceOptional.get();

        invoice.setTotalAmountAfterDecrease(request.getTotalAmount());
        invoice.setTotalAmount(
                Optional.ofNullable(invoice.getVoucher())
                        .map(
                                voucher ->
                                        voucher.getTypeVoucher() == TypeVoucher.PERCENTAGE ?
                                                request.getTotalAmount().multiply(BigDecimal.valueOf(100).subtract(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP))
                                                : request.getTotalAmount().subtract(voucher.getDiscountValue())
                                )
                        .orElse(request.getTotalAmount())
        );
        invoiceRepository.saveAndFlush(invoice);

        List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findAllById(
                request.getUpdateProductDetails().stream()
                        .map(ClientPutInvoiceDetailRequest.UpdateInvoiceDetail::getIdInvoiceDetail)
                        .toList()
        );
        List<InvoiceDetail> newInvoiceDetails = new ArrayList<>();
        for(ClientPutInvoiceDetailRequest.UpdateInvoiceDetail item : request.getUpdateProductDetails()) {
            InvoiceDetail invoiceDetail = invoiceDetails.stream()
                    .filter(it -> it.getId().equals(item.getIdInvoiceDetail()))
                    .findFirst()
                    .orElse(null);

            if (invoiceDetail == null) {
                InvoiceDetail newInvoiceDetail = new InvoiceDetail();
                ProductDetail productDetail = productDetailRepository.findById(item.getIdProductDetail()).orElseThrow(() -> new BusinessException("ProductDetailRepository detail not found"));

                newInvoiceDetail.setInvoice(invoice);
                newInvoiceDetail.setProductDetail(productDetail);
                newInvoiceDetail.setGiaGoc(productDetail.getPrice());
                newInvoiceDetail.setPrice(item.getPrice());
                newInvoiceDetail.setQuantity(item.getQuantity());
                newInvoiceDetail.setTotalAmount(item.getTotalAmount());
                newInvoiceDetails.add(newInvoiceDetail);
                continue;
            };

            if (item.getQuantity() == 0) {
                invoiceDetailRepository.delete(invoiceDetail);
                invoiceDetails.remove(invoiceDetail);
                continue;
            }

            invoiceDetail.setGiaGoc(item.getPrice());
            invoiceDetail.setPrice(item.getPrice());
            invoiceDetail.setQuantity(item.getQuantity());
            invoiceDetail.setTotalAmount(item.getTotalAmount());
        }

        if (!newInvoiceDetails.isEmpty()) invoiceDetails.addAll(newInvoiceDetails);

        invoiceDetailRepository.saveAll(invoiceDetails);

        return ResponseObject.successForward(invoice.getId(), "Update invoice success");
    }
}
