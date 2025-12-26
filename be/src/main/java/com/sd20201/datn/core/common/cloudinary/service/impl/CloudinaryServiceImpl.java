package com.sd20201.datn.core.common.cloudinary.service.impl;

import com.cloudinary.Cloudinary;
import com.sd20201.datn.core.common.cloudinary.model.response.CloudinaryResponse;
import com.sd20201.datn.core.common.cloudinary.service.CloudinaryService;
import com.sd20201.datn.infrastructure.exception.CloudinaryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public CloudinaryResponse upload(MultipartFile file,String filename) {
        try {
            final Map result = cloudinary.uploader().upload(file.getBytes(), Map.of("public_id", "buildMyPC/" + filename));
            final String url = (String) result.get("secure_url");
            final String publicId = (String) result.get("public_id");
            return CloudinaryResponse.builder()
                    .publicId(publicId)
                    .url(url)
                    .build();

        } catch(Exception e) {
            throw new CloudinaryException("Failed to upload file");
        }
    }
}
