package com.sd20201.datn.core.client.invoice.service.impl;

import com.sd20201.datn.core.client.invoice.model.request.ClientInvoiceCancelRequest;
import com.sd20201.datn.core.client.invoice.repository.ClientInvoiceRepository;
import com.sd20201.datn.core.client.invoice.service.ClientInvoiceService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Invoice;
import com.sd20201.datn.entity.LichSuTrangThaiHoaDon;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.entity.VoucherDetail;
import com.sd20201.datn.infrastructure.constant.EntityTrangThaiHoaDon;
import com.sd20201.datn.repository.CustomerRepository;
import com.sd20201.datn.repository.LichSuTrangThaiHoaDonRepository;
import com.sd20201.datn.repository.VoucherDetailRepository;
import com.sd20201.datn.repository.VoucherRepository;
import com.sd20201.datn.utils.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

}
