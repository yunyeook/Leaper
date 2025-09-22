package com.ssafy.spark.domain.business.platformAccount.repository;

import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Integer> {

    /**
     * externalAccountId와 platformTypeId로 PlatformAccount 조회
     */
    @Query("SELECT pa FROM PlatformAccount pa " +
           "JOIN FETCH pa.platformType pt " +
           "WHERE pa.externalAccountId = :externalAccountId " +
           "AND pt.id = :platformTypeId " +
           "AND pa.isDeleted = false")
    Optional<PlatformAccount> findByExternalAccountIdAndPlatformTypeId(
        @Param("externalAccountId") String externalAccountId,
        @Param("platformTypeId") String platformTypeId
    );
}