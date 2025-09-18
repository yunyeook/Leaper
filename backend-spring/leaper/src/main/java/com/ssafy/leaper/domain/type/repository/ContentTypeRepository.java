package com.ssafy.leaper.domain.type.repository;

import com.ssafy.leaper.domain.type.entity.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentTypeRepository extends JpaRepository<ContentType, String> {
}