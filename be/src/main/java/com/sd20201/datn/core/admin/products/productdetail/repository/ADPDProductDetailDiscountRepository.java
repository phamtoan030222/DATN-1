package com.sd20201.datn.core.admin.products.productdetail.repository;

import com.sd20201.datn.repository.DiscountRepository;
import com.sd20201.datn.repository.ProductDiscountDetailRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ADPDProductDetailDiscountRepository extends ProductDiscountDetailRepository {

    @Query(value = """
    SELECT dp.id
    FROM ProductDetailDiscount dp JOIN Discount d ON d.id = dp.discount.id
    WHERE d.startDate <= :date and :date <= d.endDate
    """)
    List<String> getIdByDate(Long date);

}
