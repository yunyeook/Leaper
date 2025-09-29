package com.ssafy.spark.domain.business.file.service;

import com.ssafy.spark.domain.business.file.dto.request.FileCreateRequest;
import com.ssafy.spark.domain.business.file.dto.response.FileResponse;
import com.ssafy.spark.domain.business.file.entity.File;
import com.ssafy.spark.domain.business.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FileService {

    private final FileRepository fileRepository;

    /**
     * 파일 생성
     */
    @Transactional
    public FileResponse createFile(FileCreateRequest request) {
        log.info("파일 생성 시작 - accessKey: {}", request.getAccessKey());

        try {
            File file = File.builder()
                    .accessKey(request.getAccessKey())
                    .contentType(request.getContentType())
                    .createdAt(LocalDateTime.now())
                    .build();

            File savedFile = fileRepository.save(file);

            log.info("파일 생성 완료 - id: {}, accessKey: {}", savedFile.getId(), savedFile.getAccessKey());

            FileResponse response = FileResponse.from(savedFile);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 파일 생성 (accessKey, contentType 직접 전달)
     */
    @Transactional
    public FileResponse createFile(String accessKey, String contentType) {
        FileCreateRequest request = new FileCreateRequest(accessKey, contentType);
        return createFile(request);
    }

    /**
     * ID로 파일 조회
     */
    public Optional<FileResponse> findById(Integer id) {
        log.info("파일 조회 시작 - id: {}", id);

        Optional<File> file = fileRepository.findById(id);

        if (file.isPresent()) {
            log.info("파일 조회 성공 - id: {}, accessKey: {}", id, file.get().getAccessKey());
            return Optional.of(FileResponse.from(file.get()));
        } else {
            log.warn("파일 조회 실패 - id: {}", id);
            return Optional.empty();
        }
    }

    /**
     * accessKey로 파일 조회
     */
    public Optional<FileResponse> findByAccessKey(String accessKey) {
        log.info("파일 조회 시작 - accessKey: {}", accessKey);

        Optional<File> file = fileRepository.findByAccessKey(accessKey);

        if (file.isPresent()) {
            log.info("파일 조회 성공 - accessKey: {}, id: {}", accessKey, file.get().getId());
            return Optional.of(FileResponse.from(file.get()));
        } else {
            log.warn("파일 조회 실패 - accessKey: {}", accessKey);
            return Optional.empty();
        }
    }

    /**
     * accessKey로 파일 엔티티 조회 (내부 사용)
     */
    public Optional<File> findEntityByAccessKey(String accessKey) {
        return fileRepository.findByAccessKey(accessKey);
    }

    /**
     * accessKey로 파일 존재 여부 확인
     */
    public boolean existsByAccessKey(String accessKey) {
        boolean exists = fileRepository.existsByAccessKey(accessKey);
        log.info("파일 존재 여부 - accessKey: {}, exists: {}", accessKey, exists);
        return exists;
    }

    /**
     * 모든 파일 조회
     */
    public List<FileResponse> findAll() {
        log.info("모든 파일 조회 시작");

        List<File> files = fileRepository.findAll();

        log.info("모든 파일 조회 완료 - count: {}", files.size());

        return files.stream()
                .map(FileResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 파일 삭제
     */
    @Transactional
    public void deleteById(Integer id) {
        log.info("파일 삭제 시작 - id: {}", id);

        if (fileRepository.existsById(id)) {
            fileRepository.deleteById(id);
            log.info("파일 삭제 완료 - id: {}", id);
        } else {
            log.warn("파일 삭제 실패 - 존재하지 않는 파일 id: {}", id);
            throw new IllegalArgumentException("존재하지 않는 파일입니다: " + id);
        }
    }
}