package com.sd20201.datn.core.client.invoice.repository;

import com.sd20201.datn.core.client.invoice.model.response.ClientInvoiceDetailResponse;
import com.sd20201.datn.core.client.invoice.model.response.ClientInvoiceDetailsResponse;
import com.sd20201.datn.core.client.invoice.model.response.LichSuTrangThaiHoaDonResponse;
import com.sd20201.datn.repository.InvoiceRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientInvoiceRepository extends InvoiceRepository {

    @Query(value = """
            SELECT
                i.id as id
                , i.code as code
                , i.nameReceiver as nameReceiver
                , i.email as email
                , i.addressReceiver as addressReceiver
                , i.phoneReceiver as phoneReceiver
                , i.description as description
                , i.entityTrangThaiHoaDon as invoiceStatus
                , i.totalAmount as totalAmount
                , i.totalAmountAfterDecrease as totalAmountAfterDecrease
                , SUM(ivd.quantity) as totalQuantity
                , i.createdDate as createDate
                , i.shippingFee as shippingFee
                , i.typePayment as typePayment
                , i.trangThaiThanhToan as trangThaiThanhToan
            FROM Invoice i
            LEFT JOIN InvoiceDetail ivd on ivd.invoice.id = i.id
            WHERE i.code = :code AND i.status = 0 AND i.customer.id = :customerId AND i.typeInvoice = 1
            """)
    Optional<ClientInvoiceDetailResponse> getInvoiceByCode(String code, String customerId);

    @Query("""
    SELECT
        lstthd.id as id
        , lstthd.trangThai as trangThai
        , lstthd.note as note
        , lstthd.thoiGian as thoiGian
    FROM LichSuTrangThaiHoaDon lstthd
    WHERE lstthd.hoaDon.id = :idHoaDon
    """)
    List<LichSuTrangThaiHoaDonResponse> getInvoiceLichSuTrangThaiHoaDonByIdHoaDon(String idHoaDon);

    @Query("""
     SELECT
        idt.id AS id,
        idt.code AS code,
        idt.price AS price,
        idt.quantity AS quantity,
        idt.totalAmount AS totalAmount,
        CONCAT(idt.productDetail.product.name, ' - ', idt.productDetail.code)  AS nameProductDetail,
        idt.productDetail.urlImage AS urlImage,
        idt.productDetail.color.name AS color,
        idt.productDetail.cpu.name AS cpu,
        idt.productDetail.gpu.name AS gpu,
        idt.productDetail.hardDrive.name AS hardDrive,
        idt.productDetail.material.name AS material,
        idt.productDetail.product.name AS product,
        idt.productDetail.ram.name AS ram
    FROM InvoiceDetail idt
    WHERE idt.invoice.id = :invoiceId
    """)
    List<ClientInvoiceDetailsResponse> getInvoiceDetailsByInvoiceId(String invoiceId);
}
