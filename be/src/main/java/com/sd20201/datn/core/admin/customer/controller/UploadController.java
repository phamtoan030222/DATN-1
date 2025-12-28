package com.sd20201.datn.core.admin.customer.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired; // Thêm dòng này
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:6788", "http://localhost:3000", "http://localhost:5173"})
public class UploadController {

    // 1. Khai báo Cloudinary
    private final Cloudinary cloudinary;

    @Value("${cloudinary.upload.folder:customer_images}")
    private String cloudinaryFolder;

    // 2. Constructor Injection: Spring tự động tìm file Config kia và đưa Cloudinary vào đây
    // Không cần truyền key, secret ở đây nữa vì Config đã làm rồi!
    @Autowired
    public UploadController(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            // ... (Giữ nguyên phần kiểm tra file rỗng, format ảnh như cũ) ...
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File rỗng"));
            }
            // ...

            // Xử lý upload (Code vẫn như cũ nhưng sạch hơn)
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", cloudinaryFolder,
                            "resource_type", "auto"
                    )
            );

            String publicUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", publicUrl,
                    "public_id", publicId,
                    "filename", Objects.requireNonNull(file.getOriginalFilename()),
                    "message", "Upload thành công"
            ));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi upload: " + e.getMessage()));
        }
    }
}