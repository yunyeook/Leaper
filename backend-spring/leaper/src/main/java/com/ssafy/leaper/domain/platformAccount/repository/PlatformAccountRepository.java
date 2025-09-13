package com.ssafy.leaper.domain.platformAccount.repository;

import com.ssafy.leaper.domain.type.entity.PlatformType.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {
    
    boolean existsByPlatformAccountIdAndIsDeletedFalse(Long platformAccountId);
}
