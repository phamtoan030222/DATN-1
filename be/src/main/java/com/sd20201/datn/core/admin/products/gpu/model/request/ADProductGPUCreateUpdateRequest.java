package com.sd20201.datn.core.admin.products.gpu.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ADProductGPUCreateUpdateRequest {

    private String id;

    private String code;

    private String name;

    private String brand;

    private String description;

    private String generation;

    private String series;

    private Integer releaseYear;

}
