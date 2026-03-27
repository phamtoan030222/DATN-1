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

    @Query(value = """
        SELECT 
            ci.id AS id, 
            ci.quantity AS quantity, 
            pd.price AS price, 
            
            -- Đã thêm ĐIỀU KIỆN XÓA MỀM (pdd2.status = 0) giống hệt Admin!
            COALESCE((
                SELECT MAX(d.percentage)
                FROM product_detail_discount pdd2
                JOIN discount d ON pdd2.id_discount = d.id
                WHERE pdd2.id_product_detail = pd.id
                  AND d.start_date <= :currentTime
                  AND d.end_date >= :currentTime
                  AND d.status = 0
                  AND pdd2.status = 0 
            ), 0) AS percentage,
            
            pd.name AS name, 
            pd.url_image AS imageUrl, 
            cpu.name AS cpu, 
            ram.name AS ram, 
            hd.name AS hardDrive, 
            gpu.name AS gpu, 
            c.name AS color, 
            m.name AS material, 
            pd.id AS productDetailId
        FROM cart_item ci
        LEFT JOIN product_detail pd ON ci.id_product_detail = pd.id
        LEFT JOIN cpu cpu ON pd.id_cpu = cpu.id
        LEFT JOIN ram ram ON pd.id_ram = ram.id
        LEFT JOIN hard_drive hd ON pd.id_hard_drive = hd.id
        LEFT JOIN gpu gpu ON pd.id_gpu = gpu.id
        LEFT JOIN color c ON pd.id_color = c.id
        LEFT JOIN material m ON pd.id_material = m.id
        WHERE ci.id_cart = :idCart
          AND pd.status = 0 -- Đảm bảo không lấy sản phẩm đã ngừng kinh doanh
        ORDER BY ci.last_modified_date DESC
    """, nativeQuery = true)
    List<ClientCartItemResponse> getCartItemsById(
            @Param("idCart") String idCart,
            @Param("currentTime") Long currentTime
    );

    Optional<CartItem> findByCartAndProductDetail(Cart cart, ProductDetail productDetail);
}