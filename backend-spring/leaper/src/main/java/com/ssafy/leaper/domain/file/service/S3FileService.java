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
}