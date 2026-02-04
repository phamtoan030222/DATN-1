package com.sd20201.datn.core.client.banhang.repository;

import com.sd20201.datn.core.client.banhang.model.response.ClientCartItemResponse;
import com.sd20201.datn.entity.Cart;
import com.sd20201.datn.entity.CartItem;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.repository.CartItemRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientBanHangCartItemRepository extends CartItemRepository {

    @Query("""
    SELECT
        ci.id as id
        , ci.quantity as quantity
        , pd.price as price
        , d.percentage as percentage
        , pd.name as name
        , pd.urlImage as imageUrl
        , pd.cpu.name as cpu
        , pd.ram.name as ram
        , pd.hardDrive.name as hardDrive
        , pd.gpu.name as gpu
        , pd.color.name as color
        , pd.material.name as material
        , pd.id as productDetailId
    FROM CartItem ci
    LEFT JOIN ProductDetail pd on ci.productDetail.id = pd.id
    LEFT join ProductDetailDiscount pdd on pd.id = pdd.productDetail.id
    LEFT JOIN Discount d on pdd.discount.id = d.id
    WHERE
        ci.cart.id = :idCart
        AND (d.startDate <= :time and :time <= d.endDate OR d.id IS NULL)
    """)
    List<ClientCartItemResponse> getCartItemsById(String idCart, Long time);

    Optional<CartItem> findByCartAndProductDetail(Cart cart, ProductDetail productDetail);

}
