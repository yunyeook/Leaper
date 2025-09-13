package com.ssafy.leaper.domain.file.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.ssafy.leaper.domain.file.entity.File;
import com.ssafy.leaper.domain.file.repository.FileRepository;
import com.ssafy.leaper.global.error.ErrorCode;
import com.ssafy.leaper.global.error.exception.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {
    private final FileRepository fileRepository;
    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.expirationForPut}")
    private int expirationMinutesForPut;

    @Value("${cloud.aws.s3.expirationForGet}")
    private int expirationMinutesForGet;

    @Transactional
    public String generatePresignedUploadUrl(String fileName, String contentType) {
        String key = File.generateUniqueKey(fileName, contentType);
        
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000L * 60 * expirationMinutesForPut;
        expiration.setTime(expTimeMillis);

        fileRepository.save(File.builder()
                .accessKey(key)
                .contentType(contentType)
                .build());

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        if (contentType != null && !contentType.isEmpty()) {
            generatePresignedUrlRequest.withContentType(contentType);
        }

        URL presignedUrl = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return presignedUrl.toString();
    }

    public String generatePresignedDownloadUrl(int imageId) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000L * 60 * expirationMinutesForGet;
        expiration.setTime(expTimeMillis);

        File file = fileRepository.findById(imageId).orElseThrow(()-> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, file.getAccessKey())
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        URL presignedUrl = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return presignedUrl.toString();
    }
}