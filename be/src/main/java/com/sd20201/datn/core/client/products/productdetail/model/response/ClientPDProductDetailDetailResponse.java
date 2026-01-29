package com.sd20201.datn.core.client.products.productdetail.model.response;

import com.sd20201.datn.core.common.base.IsIdentify;

public interface ClientPDProductDetailDetailResponse extends IsIdentify {

    String getCode();

    String getName();

    String getDescription();

    String getIdProduct();

    String getIdCPU();

    Double getPrice();

    String getIdGPU();

    String getIdColor();

    String getIdRAM();

    String getIdHardDrive();

    String getIdMaterial();

    // 👇 [QUAN TRỌNG 1] BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ NHẬN GIÁ GIẢM
    Integer getPercentage();

    // 👇 [QUAN TRỌNG 2] Thêm dòng này để lấy ảnh sản phẩm
    String getUrlImage();

    // 👇 [KHUYÊN DÙNG] Thêm các dòng này để hiển thị Tên (thay vì ID)
    // Vì câu SQL của bạn đã select: p.product.name as productName, p.cpu.name as cpuName...
    String getProductName();

    String getCpuName();

    String getRamName();

    String getHardDriveName();

    String getColorName();
    // String getScreenName(); // Bỏ comment nếu SQL có select screenName
}