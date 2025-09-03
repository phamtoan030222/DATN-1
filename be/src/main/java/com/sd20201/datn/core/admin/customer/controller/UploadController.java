package com.sd20201.datn.core.admin.customer.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:6788", "http://localhost:3000", "http://localhost:5173"})
public class UploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${server.port:2345}")
    private String serverPort;

    // Thêm endpoint để serve files
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);

            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Cache-Control", "max-age=3600")
                    .body(fileContent);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            // Validation
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File rỗng"
                ));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Chỉ chấp nhận hình ảnh"
                ));
            }

            // Kiểm tra kích thước file (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File quá lớn, tối đa 5MB"
                ));
            }

            // Tạo thư mục nếu chưa tồn tại
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            // Xử lý tên file
            String originalName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String ext = "";
            int i = originalName.lastIndexOf('.');
            if (i >= 0) ext = originalName.substring(i).toLowerCase(Locale.ROOT);

            String filename = "customer-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = dir.resolve(filename);

            // Ghi file
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Tạo URL public - SỬA LẠI ĐỂ DÙNG ĐÚNG PORT
            String fileUrl = "http://localhost:" + serverPort + "/api/files/" + filename;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", fileUrl,
                    "filename", filename,
                    "size", file.getSize(),
                    "message", "Upload thành công"
            ));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Lỗi khi lưu file: " + e.getMessage()
                    ));
        }
    }
}