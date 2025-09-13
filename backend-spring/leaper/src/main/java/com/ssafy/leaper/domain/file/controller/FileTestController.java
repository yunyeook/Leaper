package com.ssafy.leaper.domain.file.controller;

import com.ssafy.leaper.domain.file.dto.PresignedUrlResponse;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files/test")
@RequiredArgsConstructor
public class FileTestController {

    private final S3PresignedUrlService s3PresignedUrlService;

    @GetMapping("/upload-url")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl(
            @RequestParam String fileName,
            @RequestParam(required = false) String contentType) {
        
        String presignedUrl = s3PresignedUrlService.generatePresignedUploadUrl(fileName, contentType);
        String key = extractKeyFromUrl(presignedUrl);
        
        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl, key));
    }

    @GetMapping("/view-url")
    public ResponseEntity<String> getDownloadUrl(
            @RequestParam Integer imageId) {
        
        String presignedUrl = s3PresignedUrlService.generatePresignedDownloadUrl(imageId);
        return ResponseEntity.ok(presignedUrl);
    }

    private String extractKeyFromUrl(String presignedUrl) {
        String[] parts = presignedUrl.split("\\?")[0].split("/");
        return parts[parts.length - 2] + "/" + parts[parts.length - 1];
    }
}