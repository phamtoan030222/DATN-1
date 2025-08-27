package com.sd20201.datn.core.admin.products.battery.service;

import com.sd20201.datn.core.admin.products.battery.model.request.ADBatteryRequest;
import com.sd20201.datn.core.admin.products.battery.model.request.ADCreateBatteryRequest;
import com.sd20201.datn.core.common.base.ResponseObject;

public interface ADBatteryService {

    ResponseObject<?> getAllBatteries(ADBatteryRequest request);

    ResponseObject<?> createBattery(ADCreateBatteryRequest request);

    ResponseObject<?> updateBatteryStatus(String id);

    ResponseObject<?> updateBattery(String id, ADCreateBatteryRequest request);
}
