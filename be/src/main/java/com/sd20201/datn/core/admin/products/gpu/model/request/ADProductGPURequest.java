package com.sd20201.datn.core.admin.products.gpu.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADProductGPURequest extends PageableRequest {

    private String brand;

    private Integer releaseYear;

    private String generation;

    private String series;

}
