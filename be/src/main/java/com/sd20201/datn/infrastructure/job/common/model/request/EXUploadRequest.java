package com.sd20201.datn.infrastructure.job.common.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EXUploadRequest {

    private String id;

    private String name;

    private MultipartFile file;

}
