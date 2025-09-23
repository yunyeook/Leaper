//package com.ssafy.spark.domain.crawling.instagram.service;
//
//import com.ssafy.spark.domain.crawling.instagram.entity.File;
//import com.ssafy.spark.domain.crawling.instagram.repository.FileRepository;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ImageService {
//
//  private final S3Service s3Service;
//  private final FileRepository fileRepository;
//  private final RestTemplate restTemplate = new RestTemplate();
//
//  public File downloadAndSaveProfileImage(String imageUrl, String username) {
//    try {
//      log.info("프로필 이미지 다운로드 시도: {}", imageUrl);
//
//      // 1. 먼저 이미지 다운로드 시도
//      ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);
//      byte[] imageBytes = response.getBody();
//
//      if (imageBytes != null && imageBytes.length > 0) {
//        // 다운로드 성공 → S3 업로드 후 S3 키를 accessKey에 저장
//        String contentType = response.getHeaders().getContentType().toString();
//        String fileExtension = getFileExtension(contentType);
//
//        LocalDate now = LocalDate.now();
//        String s3Key = String.format("raw_data/instagram/profile_images/%s/%s_%d%s",
//            username,
//            username,
//            System.currentTimeMillis(),
//            fileExtension);
//
//        String uploadedKey = s3Service.uploadImageFile(imageBytes, s3Key, contentType);
//
//        return fileRepository.save(File.builder()
//            .accessKey(uploadedKey) // S3 키
//            .contentType(contentType)
//            .fileSize((long) imageBytes.length)
//            .originalName(username + "_profile" + fileExtension)
//            .createdAt(LocalDateTime.now())
//            .updatedAt(LocalDateTime.now())
//            .build());
//      }
//
//    } catch (Exception e) {
//      log.warn("이미지 다운로드 실패, 원본 URL로 저장: {}", imageUrl);
//    }
//
//    // 다운로드 실패 → 원본 URL을 accessKey에 저장
//    return fileRepository.save(File.builder()
//        .accessKey(imageUrl) // 원본 URL 직접 저장
//        .contentType("image/jpeg") // 기본값
//        .fileSize(0L)
//        .originalName("external_image")
//        .createdAt(LocalDateTime.now())
//        .updatedAt(LocalDateTime.now())
//        .build());
//  }
//  public File downloadAndSaveThumbnailImage(String imageUrl, String username, String contentId) {
//    try {
//      log.info("썸네일 이미지 다운로드 시도: {}", imageUrl);
//
//      ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);
//      byte[] imageBytes = response.getBody();
//
//      if (imageBytes != null && imageBytes.length > 0) {
//        String contentType = response.getHeaders().getContentType().toString();
//        String fileExtension = getFileExtension(contentType);
//
//        // S3 경로: raw_data/instagram/content_thumbnail_images/{contentId}/
//        String s3Key = String.format("raw_data/instagram/content_thumbnail_images/%s/thumbnail_%d%s",
//            contentId,
//            System.currentTimeMillis(),
//            fileExtension);
//
//        String uploadedKey = s3Service.uploadImageFile(imageBytes, s3Key, contentType);
//
//        return fileRepository.save(File.builder()
//            .accessKey(uploadedKey)
//            .contentType(contentType)
//            .fileSize((long) imageBytes.length)
//            .originalName(contentId + "_thumbnail" + fileExtension)
//            .createdAt(LocalDateTime.now())
//            .updatedAt(LocalDateTime.now())
//            .build());
//      }
//
//    } catch (Exception e) {
//      log.warn("썸네일 이미지 다운로드 실패, 원본 URL로 저장: {}", imageUrl);
//    }
//
//    // 다운로드 실패 시 원본 URL을 accessKey에 저장
//    return fileRepository.save(File.builder()
//        .accessKey(imageUrl)
//        .contentType("image/jpeg")
//        .fileSize(0L)
//        .originalName("external_thumbnail")
//        .createdAt(LocalDateTime.now())
//        .updatedAt(LocalDateTime.now())
//        .build());
//  }
//
//  private String getFileExtension(String contentType) {
//    switch (contentType.toLowerCase()) {
//      case "image/jpeg":
//      case "image/jpg":
//        return ".jpg";
//      case "image/png":
//        return ".png";
//      case "image/gif":
//        return ".gif";
//      case "image/webp":
//        return ".webp";
//      default:
//        return ".jpg";
//    }
//  }
//}