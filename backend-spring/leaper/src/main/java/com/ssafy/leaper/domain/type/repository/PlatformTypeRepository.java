package com.ssafy.leaper.domain.type.repository;

import com.ssafy.leaper.domain.type.entity.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformTypeRepository extends JpaRepository<PlatformType, String> {
}