package com.ssafy.leaper.domain.type.repository;

import com.ssafy.leaper.domain.type.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryTypeRepository extends JpaRepository<CategoryType, Short> {
}