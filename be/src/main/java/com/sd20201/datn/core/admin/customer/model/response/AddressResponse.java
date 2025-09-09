package com.sd20201.datn.core.admin.customer.model.response;

public interface AddressResponse {
    String getId();
    String getProvinceCity();
    String getDistrict();
    String getWardCommune();
    String getAddressDetail();
    String getCustomerId(); // nếu muốn lấy kèm ID của Customer
    Boolean getIsDefault();

    Long getCreatedDate();
    Long getLastModifiedDate();
    Long getCreatedBy();
    Long getLastModifiedBy();
}
