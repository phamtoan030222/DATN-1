package com.sd20201.datn.infrastructure.job.common.model.request;

import com.sd20201.datn.core.common.base.PageableRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EXDataRequest extends PageableRequest {

    private Map<String, Object> data;

}
