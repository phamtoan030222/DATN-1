package com.sd20201.datn.core.admin.discounts.discountDetail.service.impl;

import com.sd20201.datn.core.admin.discounts.discountDetail.model.request.AdCreateDiscountMultiRequest;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.request.AdCreateDiscountRequest;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdDiscountDetailRespone;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdProductDetailResponse;
import com.sd20201.datn.core.admin.discounts.discountDetail.model.respone.AdProductRespone;
import com.sd20201.datn.core.admin.discounts.discountDetail.repository.AdProductApplyRepositoryProduct;
import com.sd20201.datn.core.admin.discounts.discountDetail.repository.AdProductDetailRepository;
import com.sd20201.datn.core.admin.discounts.discountDetail.repository.AdProductDiscountRepository;
import com.sd20201.datn.core.admin.discounts.discountDetail.repository.AdProductNotApplyRepositoryProduct;
import com.sd20201.datn.core.admin.discounts.discountDetail.repository.CRUDProductDiscountRepository;
import com.sd20201.datn.core.admin.discounts.discountDetail.service.AdDiscountDetailService;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Discount;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.entity.ProductDetailDiscount;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.repository.DiscountRepository;
import com.sd20201.datn.repository.ProductDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdDiscountDetailServiceImpl implements AdDiscountDetailService {
    private final CRUDProductDiscountRepository crudDiscountRepository;
    private final DiscountRepository discountRepository;
    private final ProductDetailRepository productDetailRepository;
    private final AdProductApplyRepositoryProduct adProductApplyRepository;
    private final AdProductNotApplyRepositoryProduct adProductNotApplyRepository;
    private final AdProductDiscountRepository adProductRepository;
    private final AdProductDetailRepository adProductDetailRepository;
    @Override
    public ResponseObject<?> creatDiscount(AdCreateDiscountRequest request) {
        ProductDetail productDetail = productDetailRepository.findById(request.getProductDetailId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        Discount discount = discountRepository.findById(request.getDiscountId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá"));

        ProductDetailDiscount productDetailDiscount = new ProductDetailDiscount();
        productDetailDiscount.setDiscount(discount);
        productDetailDiscount.setProductDetail(productDetail);
        productDetailDiscount.setOriginalPrice(request.getOriginalPrice());
        productDetailDiscount.setSalePrice(request.getSalePrice());
        productDetailDiscount.setDescription(request.getDescription());
        productDetailDiscount.setCreatedBy(System.currentTimeMillis());
        productDetailDiscount.setCreatedDate(System.currentTimeMillis());
        productDetailDiscount.setStatus(EntityStatus.ACTIVE);
        crudDiscountRepository.save(productDetailDiscount);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Áp dụng thành công",
                true,
                null
        );

    }

    @Override
    public ResponseObject<?> creatDiscountMulti(AdCreateDiscountMultiRequest request) {
        Discount discount = discountRepository.findById(request.getDiscountId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá"));

        for (String productDetailId : request.getProductDetailIds()) {
            ProductDetail productDetail = productDetailRepository.findById(productDetailId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + productDetailId));

            ProductDetailDiscount productDetailDiscount = new ProductDetailDiscount();
            productDetailDiscount.setDiscount(discount);
            productDetailDiscount.setProductDetail(productDetail);
            productDetailDiscount.setOriginalPrice(request.getOriginalPrice());
            productDetailDiscount.setSalePrice(request.getSalePrice());
            productDetailDiscount.setDescription(request.getDescription());
            productDetailDiscount.setCreatedBy(System.currentTimeMillis());
            productDetailDiscount.setCreatedDate(System.currentTimeMillis());
            productDetailDiscount.setStatus(EntityStatus.ACTIVE);

            crudDiscountRepository.save(productDetailDiscount);
        }

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Áp dụng giảm giá thành công cho " + request.getProductDetailIds().size() + " sản phẩm",
                true,
                null
        );
    }

    @Override
    public ResponseObject<?> updateStatus(String id) {
        ProductDetailDiscount productDetailDiscount = crudDiscountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi với id: " + id));

        if (productDetailDiscount.getStatus() == EntityStatus.ACTIVE) {
            productDetailDiscount.setStatus(EntityStatus.INACTIVE);
        } else {
            productDetailDiscount.setStatus(EntityStatus.ACTIVE);
        }


        productDetailDiscount.setCreatedBy(System.currentTimeMillis());
        crudDiscountRepository.save(productDetailDiscount);

        return new ResponseObject<>(
                null,
                HttpStatus.OK,
                "Cập nhật trạng thái thành công",
                true,
                null
        );
    }


    @Override
    public ResponseObject<Page<AdDiscountDetailRespone>> getAppliedProductsByDiscountId(String discountId, Pageable pageable) {
        Page<AdDiscountDetailRespone> pageResult = adProductApplyRepository.getAllAppliedProductsByDiscount(pageable, discountId);

        return new ResponseObject<>(
                pageResult,
                HttpStatus.OK,
                "Lấy danh sách sản phẩm áp dụng giảm giá thành công",
                true,
                null
        );
    }




    @Override
    public ResponseObject<Page<AdProductDetailResponse>> getNotAppliedProducts(String discountId, Pageable pageable) {
        Page<AdProductDetailResponse> pageResult = adProductNotApplyRepository.getAllNotAppliedProductsByDiscount(pageable, discountId);
        return new ResponseObject<>(
                pageResult,
                HttpStatus.OK,
                "Lấy danh sách sản phẩm chưa áp dung giảm giá",
                true,
                null
        );
    }

    @Override
    public ResponseObject<Page<AdProductRespone>> getAllProducts(Pageable pageable) {
        Page<AdProductRespone> page = adProductRepository.getAllProducts(pageable);

        return new ResponseObject<>(
                page,
                HttpStatus.OK,
                "Lấy danh sách sản phẩm thành công. Tổng số sản phẩm: " + page.getTotalElements(),
                true,
                null
        );
    }

    @Override
    public ResponseObject<List<AdProductDetailResponse>> getProductDetailsByProductId(String productId) {
        List<AdProductDetailResponse> details = adProductDetailRepository.findProductDetailsByProductId(productId);

        if (details.isEmpty()) {
            return new ResponseObject<>(
                    null,
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy sản phẩm chi tiết cho productId = " + productId,
                    false,
                    null
            );
        }

        return new ResponseObject<>(
                details,
                HttpStatus.OK,
                "Lấy chi tiết sản phẩm thành công",
                true,
                null
        );
    }


}
