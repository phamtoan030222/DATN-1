package com.sd20201.datn.core.client.invoice.repository;

import com.sd20201.datn.core.client.invoice.model.request.ClientGetInvoicesRequest;
import com.sd20201.datn.core.client.invoice.model.response.ClientInvoiceDetailResponse;
import com.sd20201.datn.core.client.invoice.model.response.ClientInvoiceDetailsResponse;
import com.sd20201.datn.core.client.invoice.model.response.LichSuTrangThaiHoaDonResponse;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Invoice;
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
            WHERE ( i.code = :code OR i.id = :code ) AND i.status = 0 AND (i.customer.id = :customerId OR (:customerId is NULL  AND i.customer.id IS null )) AND i.typeInvoice IN (1, 3)
            GROUP BY
                i.id,
                i.code,
                i.nameReceiver,
                i.email,
                i.addressReceiver,
                i.phoneReceiver,
                i.description,
                i.entityTrangThaiHoaDon,
                i.totalAmount,
                i.totalAmountAfterDecrease,
                i.createdDate,
                i.shippingFee,
                i.typePayment,
                i.trangThaiThanhToan
            """)
    Optional<ClientInvoiceDetailResponse> getInvoiceByCode(String code, String customerId);

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
            WHERE i.status = 0 AND (i.customer.id = :customerId OR (:customerId is NULL  AND i.customer.id IS null ))
                  AND (:#{#request.searchQuery} IS NULL OR :#{#request.searchQuery} = ''
                        OR LOWER(i.code) LIKE LOWER(CONCAT('%', :#{#request.searchQuery}, '%'))
                        )
                      AND ( :#{#request.searchStatus} IS NULL OR i.entityTrangThaiHoaDon = :#{#request.searchStatus})
                      AND ( :#{#request.loaiHoaDon} IS NULL OR i.typeInvoice = :#{#request.loaiHoaDon})
                      AND (:#{#request.startDate} IS NULL OR CAST(i.createdDate AS BIGINTEGER) >= :#{#request.startDate})
                      AND (:#{#request.endDate} IS NULL OR CAST(i.createdDate AS BIGINTEGER) <= :#{#request.endDate})
                      AND i.entityTrangThaiHoaDon != 7
                      AND i.totalAmount > 0
            GROUP BY
                i.id,
                i.code,
                i.nameReceiver,
                i.email,
                i.addressReceiver,
                i.phoneReceiver,
                i.description,
                i.entityTrangThaiHoaDon,
                i.totalAmount,
                i.totalAmountAfterDecrease,
                i.createdDate,
                i.shippingFee,
                i.typePayment,
                i.trangThaiThanhToan
            ORDER BY i.createdDate DESC
            """)
    List<ClientInvoiceDetailResponse> getInvoicesByIdCustomer(String customerId, ClientGetInvoicesRequest request);

    @Query("""
    SELECT
        lstthd.id as id
        , lstthd.trangThai as trangThai
        , lstthd.note as note
        , lstthd.thoiGian as thoiGian
        , lstthd.nhanVien.id as idStaff
        , lstthd.nhanVien.name as nameStaff
    FROM LichSuTrangThaiHoaDon lstthd
    WHERE lstthd.hoaDon.id = :idHoaDon
    AND (lstthd.hoaDon.customer.id = :customerId OR (:customerId IS NULL AND lstthd.hoaDon.customer.id IS NULL))
    """)
    List<LichSuTrangThaiHoaDonResponse> getInvoiceLichSuTrangThaiHoaDonByIdHoaDon(String idHoaDon, String customerId);

    @Query("""
     SELECT
        idt.id AS id,
        idt.invoice.id as idInvoice,
        idt.productDetail.id as idProductDetail,
        idt.code AS code,
        idt.price AS price,
        idt.giaGoc as giaGoc,
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
        idt.productDetail.ram.name AS ram,
        idt.createdDate AS createdDate
    FROM InvoiceDetail idt
    WHERE idt.invoice.id in :invoiceIds
    """)
    List<ClientInvoiceDetailsResponse> getInvoiceDetailsByInvoiceId(List<String> invoiceIds);
}
