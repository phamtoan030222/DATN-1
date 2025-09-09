package com.sd20201.datn.core.admin.customer.model.response;


public interface CustomerResponse {
    String getId();
    String getCustomerName();
    String getCustomerPhone();
    String getCustomerEmail();
    String getCustomerAvatar();
    String getCustomerIdAccount();
    String getCustomerCode();
    String getCustomerDescription();
    Integer getCustomerStatus();
    Boolean getCustomerGender();
    Long getCustomerBirthday();
    Long getCustomerCreatedBy();
    Long getCustomerCreatedDate();
    Long getCustomerModifiedBy();
    Long getCustomerModifiedDate();
}

