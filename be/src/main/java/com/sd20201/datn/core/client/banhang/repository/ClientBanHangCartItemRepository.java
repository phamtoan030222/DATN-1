package com.sd20201.datn.core.client.banhang.repository;

import com.sd20201.datn.core.client.banhang.model.response.ClientCartItemResponse;
import com.sd20201.datn.entity.Cart;
import com.sd20201.datn.entity.CartItem;
import com.sd20201.datn.entity.ProductDetail;
import com.sd20201.datn.repository.CartItemRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientBanHangCartItemRepository extends CartItemRepository {

    @Query("""
    SELECT
        ci.id as id
        , ci.quantity as quantity
        , pd.price as price
        , MAX(d.percentage) as percentage
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
    LEFT JOIN ci.productDetail pd
    LEFT JOIN ProductDetailDiscount pdd ON pdd.productDetail.id = pd.id AND pdd.id IN :idsProductDetailDiscount
    LEFT JOIN pdd.discount d
    WHERE
        ci.cart.id = :idCart
    GROUP BY
        ci.id
        , ci.quantity
        , pd.price
        , pd.name
        , pd.urlImage
        , pd.cpu.name
        , pd.ram.name
        , pd.hardDrive.name
        , pd.gpu.name
        , pd.color.name
        , pd.material.name
        , pd.id
    ORDER BY ci.lastModifiedDate DESC
    """)
    List<ClientCartItemResponse> getCartItemsContainDiscountById(
            @Param("idCart") String idCart,
            @Param("idsProductDetailDiscount") List<String> idsProductDetailDiscount
    );

    @Query("""
    SELECT
        ci.id as id
        , ci.quantity as quantity
        , pd.price as price
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
    LEFT JOIN ci.productDetail pd
    WHERE
        ci.cart.id = :idCart
    ORDER BY ci.lastModifiedDate DESC
    """)
    List<ClientCartItemResponse> getCartItemsById(@Param("idCart") String idCart);

    Optional<CartItem> findByCartAndProductDetail(Cart cart, ProductDetail productDetail);
}