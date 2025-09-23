package com.ssafy.leaper.domain.file.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ssafy.leaper.domain.file.entity.File;
import com.ssafy.leaper.domain.file.repository.FileRepository;
import com.ssafy.leaper.global.error.ErrorCode;
import com.ssafy.leaper.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class S3FileService {

    private final FileRepository fileRepository;
    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public File uploadFileToS3(MultipartFile multipartFile, String fileType) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        try {
            // 1. 고유한 파일 키 생성
            String originalFileName = multipartFile.getOriginalFilename();
            String contentType = multipartFile.getContentType();
            String key = File.generateUniqueKey(originalFileName, fileType);

            // 2. S3 업로드를 위한 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(multipartFile.getSize());

            // 3. S3에 파일 업로드
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                key,
                multipartFile.getInputStream(),
                metadata
            );

            amazonS3Client.putObject(putObjectRequest);

            // 4. DB에 파일 정보 저장
            File file = File.builder()
                .accessKey(key)
                .contentType(contentType)
                .build();

            return fileRepository.save(file);

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional
    public File uploadFileFromUrl(String imageUrl, String fileType) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        try {
            log.info("URL에서 이미지 다운로드 시작: {}", imageUrl);

            // 1. URL에서 이미지 다운로드
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();

            // YouTube/Google 이미지를 위한 강화된 헤더 설정
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Sec-Fetch-Dest", "image");
            connection.setRequestProperty("Sec-Fetch-Mode", "no-cors");
            connection.setRequestProperty("Sec-Fetch-Site", "cross-site");

            // 연결 및 읽기 타임아웃 설정
            connection.setConnectTimeout(10000); // 10초
            connection.setReadTimeout(10000);    // 10초

            InputStream inputStream = connection.getInputStream();
            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();

            // 2. 파일명 추출 (URL에서)
            String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            if (fileName.indexOf('?') > 0) {
                fileName = fileName.substring(0, fileName.indexOf('?'));
            }
            if (!fileName.contains(".")) {
                fileName += ".jpg"; // 기본 확장자
            }

            // 3. 고유한 파일 키 생성
            String key = File.generateUniqueKey(fileName, fileType);

            // 4. S3 업로드를 위한 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType != null ? contentType : "image/jpeg");
            if (contentLength > 0) {
                metadata.setContentLength(contentLength);
            }

            // 5. S3에 파일 업로드
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                key,
                inputStream,
                metadata
            );

            amazonS3Client.putObject(putObjectRequest);

            // 6. DB에 파일 정보 저장
            File file = File.builder()
                .accessKey(key)
                .contentType(metadata.getContentType())
                .build();

            File savedFile = fileRepository.save(file);

            log.info("URL에서 이미지 업로드 완료: {} -> S3 Key: {}", imageUrl, key);
            return savedFile;

        } catch (Exception e) {
            log.error("URL에서 이미지 다운로드 실패: {}", imageUrl, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}