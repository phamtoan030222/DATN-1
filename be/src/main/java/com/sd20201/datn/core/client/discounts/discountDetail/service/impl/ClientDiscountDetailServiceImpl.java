package com.sd20201.datn.core.client.discounts.discountDetail.service.impl;

import com.sd20201.datn.core.client.discounts.discountDetail.model.request.ClientCreateDiscountMultiRequest;
import com.sd20201.datn.core.client.discounts.discountDetail.model.request.ClientCreateDiscountRequest;
import com.sd20201.datn.core.client.discounts.discountDetail.model.respone.ClientDiscountDetailRespone;
import com.sd20201.datn.core.client.discounts.discountDetail.model.respone.ClientProductDetailResponse;
import com.sd20201.datn.core.client.discounts.discountDetail.model.respone.ClientProductRespone;
import com.sd20201.datn.core.client.discounts.discountDetail.repository.ClientProductApplyRepositoryProduct;
import com.sd20201.datn.core.client.discounts.discountDetail.repository.ClientProductDetailRepository;
import com.sd20201.datn.core.client.discounts.discountDetail.repository.ClientProductDiscountRepository;
import com.sd20201.datn.core.client.discounts.discountDetail.repository.ClientProductNotApplyRepositoryProduct;
import com.sd20201.datn.core.client.discounts.discountDetail.repository.ClientCRUDProductDiscountRepository;
import com.sd20201.datn.core.client.discounts.discountDetail.service.ClientDiscountDetailService;
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
public class ClientDiscountDetailServiceImpl implements ClientDiscountDetailService {
    private final ClientCRUDProductDiscountRepository crudDiscountRepository;
    private final DiscountRepository discountRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ClientProductApplyRepositoryProduct adProductApplyRepository;
    private final ClientProductNotApplyRepositoryProduct adProductNotApplyRepository;
    private final ClientProductDiscountRepository adProductRepository;
    private final ClientProductDetailRepository adProductDetailRepository;
    @Override
    public ResponseObject<?> creatDiscount(ClientCreateDiscountRequest request) {
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
    public ResponseObject<?> creatDiscountMulti(ClientCreateDiscountMultiRequest request) {
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
    public ResponseObject<Page<ClientDiscountDetailRespone>> getAppliedProductsByDiscountId(String discountId, Pageable pageable) {
        Page<ClientDiscountDetailRespone> pageResult = adProductApplyRepository.getAllAppliedProductsByDiscount(pageable, discountId);

        return new ResponseObject<>(
                pageResult,
                HttpStatus.OK,
                "Lấy danh sách sản phẩm áp dụng giảm giá thành công",
                true,
                null
        );
    }




    @Override
    public ResponseObject<Page<ClientProductDetailResponse>> getNotAppliedProducts(String discountId, Pageable pageable) {
        Page<ClientProductDetailResponse> pageResult = adProductNotApplyRepository.getAllNotAppliedProductsByDiscount(pageable, discountId);
        return new ResponseObject<>(
                pageResult,
                HttpStatus.OK,
                "Lấy danh sách sản phẩm chưa áp dung giảm giá",
                true,
                null
        );
    }

    @Override
    public ResponseObject<Page<ClientProductRespone>> getAllProducts(Pageable pageable) {
        Page<ClientProductRespone> page = adProductRepository.getAllProducts(pageable);

        return new ResponseObject<>(
                page,
                HttpStatus.OK,
                "Lấy danh sách sản phẩm thành công. Tổng số sản phẩm: " + page.getTotalElements(),
                true,
                null
        );
    }

    @Override
    public ResponseObject<List<ClientProductDetailResponse>> getProductDetailsByProductId(String productId) {
        List<ClientProductDetailResponse> details = adProductDetailRepository.findProductDetailsByProductId(productId);

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
