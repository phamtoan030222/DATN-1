package com.sd20201.datn.core.client.banhang.repository;

import com.sd20201.datn.entity.IMEI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientBanHangIMEIRepository extends JpaRepository<IMEI, String> {

//    @Query("SELECT COUNT(i) FROM IMEI i WHERE i.productDetail.id = :productDetailId " +
//            "AND NOT EXISTS (SELECT 1 FROM InvoiceDetail id WHERE id.iMEISold.id = i.id)")
//    Long countAvailableIMEIByProductDetailId(@Param("productDetailId") String productDetailId);
//
//    // Chỉ các phương thức cần thiết cho bán hàng
//    @Query("SELECT COUNT(i) FROM IMEI i WHERE i.productDetail.id = :productDetailId " +
//            "AND NOT EXISTS (SELECT 1 FROM InvoiceDetail id WHERE id.iMEISold.id = i.id)")
//    Long countByProductDetailIdAndInvoiceDetailIsNull(@Param("productDetailId") String productDetailId);

}
