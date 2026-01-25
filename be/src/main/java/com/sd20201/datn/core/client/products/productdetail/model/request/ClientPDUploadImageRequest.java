package com.sd20201.datn.core.client.products.productdetail.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientPDUploadImageRequest {

    private MultipartFile imageProduct;

    private List<MultipartFile> images;

}
