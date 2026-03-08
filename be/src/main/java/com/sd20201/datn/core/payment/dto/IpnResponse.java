package com.sd20201.datn.core.payment.dto;

public class IpnResponse {
    private String rspCode;
    private String message;

    public IpnResponse(String rspCode, String message) {
        this.rspCode = rspCode;
        this.message = message;
    }

    // Getters and Setters
    public String getRspCode() {
        return rspCode;
    }

    public void setRspCode(String rspCode) {
        this.rspCode = rspCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}