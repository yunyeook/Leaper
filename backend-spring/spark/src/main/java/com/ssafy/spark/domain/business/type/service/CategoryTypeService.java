package com.ssafy.spark.domain.business.type.service;

import com.ssafy.spark.domain.business.type.dto.response.CategoryTypeResponse;
import com.ssafy.spark.domain.business.type.entity.CategoryType;
import com.ssafy.spark.domain.business.type.repository.CategoryTypeRepository;
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
public class CategoryTypeService {

    private final CategoryTypeRepository categoryTypeRepository;

    /**
     * categoryTypeId로 CategoryType 엔티티 조회 (내부 사용)
     */
    public Optional<CategoryType> findEntityById(Short categoryTypeId) {

        return categoryTypeRepository.findById(categoryTypeId);
    }
}