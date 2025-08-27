package com.sd20201.datn.core.admin.products.cpu.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ADProductCPUCreateUpdateRequest {

    private String id;

    private String code;

    private String name;

    private String brand;

    private String description;

    private String generation;

    private String series;

    private Integer releaseYear;

}
