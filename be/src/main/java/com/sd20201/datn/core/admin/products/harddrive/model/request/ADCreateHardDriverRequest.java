package com.sd20201.datn.core.admin.products.harddrive.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADCreateHardDriverRequest {

    private String name;
    private String brand; // thuong hieu o cung
    private String type; // loai o cung HDD, SSD..
    private String typeConnect; //giao tiep
    private Integer capacity; // dung luong
    private Integer readSpeed; // toc do doc/ghi
    private Integer writeSpeed;
    private Integer cacheMemory;  // bo nho dem
    private Float physicalSize;  //kich thuoc vat ly
    private String description;

}
