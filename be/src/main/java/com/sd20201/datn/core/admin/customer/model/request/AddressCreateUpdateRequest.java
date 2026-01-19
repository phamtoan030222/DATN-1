package com.sd20201.datn.core.admin.customer.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressCreateUpdateRequest {
    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String provinceCity;

//    @NotBlank(message = "Quận/Huyện/Phường/Xã không được để trống")
    private String district;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String wardCommune;

    @NotBlank(message = "Chi tiết địa chỉ không được để trống")
    private String addressDetail;

    // Có thể thêm trường để set mặc định từ client
    private Boolean isDefault = false;
}
