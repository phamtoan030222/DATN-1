package com.sd20201.datn.core.common.cloudinary.service;

import com.sd20201.datn.core.common.cloudinary.model.response.CloudinaryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    CloudinaryResponse upload(MultipartFile file, String filename);



}
