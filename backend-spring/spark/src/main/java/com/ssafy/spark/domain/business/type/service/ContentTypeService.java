package com.ssafy.spark.domain.business.type.service;

import com.ssafy.spark.domain.business.type.dto.response.ContentTypeResponse;
import com.ssafy.spark.domain.business.type.entity.ContentType;
import com.ssafy.spark.domain.business.type.repository.ContentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ContentTypeService {

    private final ContentTypeRepository contentTypeRepository;

    /**
     * contentTypeId로 ContentType 조회
     */
    public Optional<ContentTypeResponse> findById(String contentTypeId) {

        log.info("ContentType 조회 시작 - contentTypeId: {}", contentTypeId);

        Optional<ContentType> contentType = contentTypeRepository.findById(contentTypeId);

        if (contentType.isPresent()) {
            log.info("ContentType 조회 성공 - contentTypeId: {}, typeName: {}",
                    contentTypeId, contentType.get().getTypeName());
            return Optional.of(ContentTypeResponse.from(contentType.get()));
        } else {
            log.warn("ContentType 조회 실패 - contentTypeId: {}", contentTypeId);
            return Optional.empty();
        }
    }

    /**
     * 모든 ContentType 조회
     */
    public List<ContentTypeResponse> findAll() {

        log.info("모든 ContentType 조회 시작");

        List<ContentType> contentTypes = contentTypeRepository.findAll();

        log.info("모든 ContentType 조회 완료 - count: {}", contentTypes.size());

        return contentTypes.stream()
                .map(ContentTypeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * contentTypeId로 ContentType 존재 여부 확인
     */
    public boolean existsById(String contentTypeId) {

        boolean exists = contentTypeRepository.existsById(contentTypeId);

        log.info("ContentType 존재 여부 - contentTypeId: {}, exists: {}",
                contentTypeId, exists);

        return exists;
    }

    /**
     * contentTypeId로 ContentType 엔티티 조회 (내부 사용)
     */
    public Optional<ContentType> findEntityById(String contentTypeId) {

        return contentTypeRepository.findById(contentTypeId);
    }

    /**
     * contentTypeId로 ContentType 엔티티 조회 (내부 사용) - 별칭
     */
    public Optional<ContentType> findEntityByContentTypeId(String contentTypeId) {

        return contentTypeRepository.findById(contentTypeId);
    }
}