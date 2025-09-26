package com.ssafy.spark.domain.business.content.service;

import com.ssafy.spark.domain.business.content.dto.response.ContentResponse;
import com.ssafy.spark.domain.business.content.entity.Content;
import com.ssafy.spark.domain.business.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;

    /**
     * externalContentId와 platformTypeId로 Content 엔티티 조회 (내부 사용)
     */
    public Optional<Content> findEntityByExternalContentIdAndPlatformType(
            String externalContentId, String platformTypeId) {

        return contentRepository
                .findByExternalContentIdAndPlatformTypeId(externalContentId, platformTypeId);
    }
}