package com.ssafy.leaper.domain.type.repository;

import com.ssafy.leaper.domain.type.entity.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderTypeRepository extends JpaRepository<ProviderType, String> {
}