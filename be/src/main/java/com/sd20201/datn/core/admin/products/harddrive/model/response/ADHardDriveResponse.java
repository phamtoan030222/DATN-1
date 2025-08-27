package com.sd20201.datn.core.admin.products.harddrive.model.response;

public interface ADHardDriveResponse {

    String getId();
    String getName();
    String getBrand();
    String getStatus();
    String getType();
    String getTypeConnect();
    Integer getCapacity();
    Integer getReadSpeed();
    Integer getWriteSpeed();
    Integer getCacheMemory();
    Float getPhysicalSize();
    String getDescription();

}
