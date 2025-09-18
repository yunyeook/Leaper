package com.ssafy.leaper.domain.type.service;

import com.ssafy.leaper.domain.type.dto.response.PlatformTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.ContentTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.ProviderTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.CategoryTypeResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;

import java.util.List;

public interface TypeService {
    ServiceResult<List<PlatformTypeResponse>> getAllPlatformTypes();
    ServiceResult<List<ContentTypeResponse>> getAllContentTypes();
    ServiceResult<List<ProviderTypeResponse>> getAllProviderTypes();
    ServiceResult<List<CategoryTypeResponse>> getAllCategoryTypes();
}