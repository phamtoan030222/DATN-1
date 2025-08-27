package com.sd20201.datn.core.admin.products.battery.model.response;

import com.sd20201.datn.infrastructure.constant.TechnolyCharging;
import com.sd20201.datn.infrastructure.constant.TypeBattery;

public interface ADBatteryResponse {
    String getId();          // từ PrimaryEntity (UUID hoặc Long)
    String getCode();        // mã pin
    String getName();        // tên pin
    String getBrand();       // thương hiệu
    TechnolyCharging getTechnolyCharging(); // công nghệ sạc
    Integer getCapacity();   // dung lượng (mAh)
    Boolean getRemoveBattery(); // có tháo rời không
    TypeBattery getType(); // loại pin
    String getStatus();


}
