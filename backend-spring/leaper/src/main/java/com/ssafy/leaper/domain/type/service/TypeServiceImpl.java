package com.ssafy.leaper.domain.type.service;

import com.ssafy.leaper.domain.type.dto.response.PlatformTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.ContentTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.ProviderTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.CategoryTypeResponse;
import com.ssafy.leaper.domain.type.repository.PlatformTypeRepository;
import com.ssafy.leaper.domain.type.repository.ContentTypeRepository;
import com.ssafy.leaper.domain.type.repository.ProviderTypeRepository;
import com.ssafy.leaper.domain.type.repository.CategoryTypeRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TypeServiceImpl implements TypeService {

    private final PlatformTypeRepository platformTypeRepository;
    private final ContentTypeRepository contentTypeRepository;
    private final ProviderTypeRepository providerTypeRepository;
    private final CategoryTypeRepository categoryTypeRepository;

    @Override
    public ServiceResult<List<PlatformTypeResponse>> getAllPlatformTypes() {
        log.info("TypeService : getAllPlatformTypes() 호출");

        List<PlatformTypeResponse> platformTypes = platformTypeRepository.findAll()
            .stream()
            .map(PlatformTypeResponse::from)
            .toList();

        return ServiceResult.ok(platformTypes);
    }

    @Override
    public ServiceResult<List<ContentTypeResponse>> getAllContentTypes() {
        log.info("TypeService : getAllContentTypes() 호출");

        List<ContentTypeResponse> contentTypes = contentTypeRepository.findAll()
            .stream()
            .map(ContentTypeResponse::from)
            .toList();

        return ServiceResult.ok(contentTypes);
    }

    @Override
    public ServiceResult<List<ProviderTypeResponse>> getAllProviderTypes() {
        log.info("TypeService : getAllProviderTypes() 호출");

        List<ProviderTypeResponse> providerTypes = providerTypeRepository.findAll()
            .stream()
            .map(ProviderTypeResponse::from)
            .toList();

        return ServiceResult.ok(providerTypes);
    }

    @Override
    public ServiceResult<List<CategoryTypeResponse>> getAllCategoryTypes() {
        log.info("TypeService : getAllCategoryTypes() 호출");

        List<CategoryTypeResponse> categoryTypes = categoryTypeRepository.findAll()
            .stream()
            .map(CategoryTypeResponse::from)
            .toList();

        return ServiceResult.ok(categoryTypes);
    }
}