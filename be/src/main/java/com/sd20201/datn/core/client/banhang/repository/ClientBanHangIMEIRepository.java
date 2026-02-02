package com.sd20201.datn.core.client.banhang.repository;

import com.sd20201.datn.entity.IMEI;
import com.sd20201.datn.entity.InvoiceDetail;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.infrastructure.constant.ImeiStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientBanHangIMEIRepository extends JpaRepository<IMEI, String> {

    // 1. Tìm các IMEI còn hàng (AVAILABLE = 0) của sản phẩm đó để Tăng số lượng
    // Sử dụng @Query để đảm bảo chính xác, bất kể tên hàm là gì
    @Query("SELECT i FROM IMEI i WHERE i.productDetail = :productDetail AND i.imeiStatus = :imeiStatus")
    List<IMEI> findByProductDetailAndImeiStatus(
            @Param("productDetail") ProductDetail productDetail,
            @Param("imeiStatus") ImeiStatus imeiStatus
    );

    // 2. Tìm các IMEI đang nằm trong chi tiết hóa đơn này để Xóa/Giảm số lượng
    @Query("SELECT i FROM IMEI i WHERE i.invoiceDetail = :invoiceDetail")
    List<IMEI> findByInvoiceDetail(@Param("invoiceDetail") InvoiceDetail invoiceDetail);

    // Nếu bạn cần query tìm list IMEI theo list ID (đã có sẵn của JpaRepository nhưng khai báo lại cho chắc nếu cần)
    // List<IMEI> findAllById(Iterable<String> ids);

    @Query("SELECT i FROM IMEI i WHERE i.productDetail.id = :productDetailId AND i.imeiStatus = :status ORDER BY i.createdDate ASC")
    List<IMEI> findAvailableImei(
            @Param("productDetailId") String productDetailId,
            @Param("status") ImeiStatus status,
            Pageable pageable
    );
}
