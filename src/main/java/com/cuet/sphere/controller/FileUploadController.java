package com.cuet.sphere.controller;

import com.cuet.sphere.service.S3Service;
import com.cuet.sphere.response.FileUploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private S3Service s3Service;

    @PostMapping("/file")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(FileUploadResponse.error("File is empty"));
            }

            // Check file size (limit to 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(FileUploadResponse.error("File size exceeds 10MB limit"));
            }

            // Check file type (allow common document and image types)
            String contentType = file.getContentType();
            if (contentType == null || !isAllowedFileType(contentType)) {
                return ResponseEntity.badRequest().body(FileUploadResponse.error("File type not allowed. Allowed types: PDF, DOC, DOCX, JPG, PNG, GIF"));
            }

            String fileUrl = s3Service.uploadFile(file);
            return ResponseEntity.ok(FileUploadResponse.success(fileUrl));
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(FileUploadResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    private boolean isAllowedFileType(String contentType) {
        return contentType.startsWith("application/pdf") ||
               contentType.equals("application/msword") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
               contentType.startsWith("image/");
    }
}
