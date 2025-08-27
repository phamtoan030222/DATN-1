package com.sd20201.datn.core.admin.products.ram.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADCreateRam {
    private String name; // tên ram
    private String brand; //hãng sản xuất
    private String type; // DDR3, 4, 5
    private Integer capacity; //Dung luong (GB)
    private Integer busSpeed; //MHz
    private Integer slotConFig; //Cau hinh khe
    private Integer maxSupported; //Ho tro ram toi da
    private String description;
}
