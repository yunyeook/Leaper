package com.ssafy.spark.domain.business.type.service;

import com.ssafy.spark.domain.business.type.dto.response.PlatformTypeResponse;
import com.ssafy.spark.domain.business.type.entity.PlatformType;
import com.ssafy.spark.domain.business.type.repository.PlatformTypeRepository;
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
public class PlatformTypeService {

    private final PlatformTypeRepository platformTypeRepository;

    /**
     * platformTypeId로 PlatformType 조회
     */
    public Optional<PlatformTypeResponse> findById(String platformTypeId) {

        log.info("PlatformType 조회 시작 - platformTypeId: {}", platformTypeId);

        Optional<PlatformType> platformType = platformTypeRepository.findById(platformTypeId);

        if (platformType.isPresent()) {
            log.info("PlatformType 조회 성공 - platformTypeId: {}, typeName: {}",
                    platformTypeId, platformType.get().getTypeName());
            return Optional.of(PlatformTypeResponse.from(platformType.get()));
        } else {
            log.warn("PlatformType 조회 실패 - platformTypeId: {}", platformTypeId);
            return Optional.empty();
        }
    }

    /**
     * 모든 PlatformType 조회
     */
    public List<PlatformTypeResponse> findAll() {

        log.info("모든 PlatformType 조회 시작");

        List<PlatformType> platformTypes = platformTypeRepository.findAll();

        log.info("모든 PlatformType 조회 완료 - count: {}", platformTypes.size());

        return platformTypes.stream()
                .map(PlatformTypeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * platformTypeId로 PlatformType 존재 여부 확인
     */
    public boolean existsById(String platformTypeId) {

        boolean exists = platformTypeRepository.existsById(platformTypeId);

        log.info("PlatformType 존재 여부 - platformTypeId: {}, exists: {}",
                platformTypeId, exists);

        return exists;
    }

    /**
     * platformTypeId로 PlatformType 엔티티 조회 (내부 사용)
     */
    public Optional<PlatformType> findEntityById(String platformTypeId) {

        return platformTypeRepository.findById(platformTypeId);
    }

    /**
     * platformTypeId로 PlatformType 엔티티 조회 (내부 사용) - 별칭
     */
    public Optional<PlatformType> findEntityByPlatformTypeId(String platformTypeId) {

        return platformTypeRepository.findById(platformTypeId);
    }
}