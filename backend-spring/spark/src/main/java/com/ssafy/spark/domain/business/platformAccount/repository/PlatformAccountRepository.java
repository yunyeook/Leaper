package com.ssafy.spark.domain.business.platformAccount.repository;

import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * platformTypeId로 PlatformAccount 조회
     */
    @Query("SELECT pa FROM PlatformAccount pa " +
           "JOIN FETCH pa.platformType pt " +
           "WHERE pt.id = :platformTypeId " +
           "AND pa.isDeleted = false")
    Optional<PlatformAccount> findByPlatformTypeId(
        @Param("platformTypeId") String platformTypeId
    );

    /**
     * platformTypeId로 모든 PlatformAccount 조회
     */
    @Query("SELECT pa FROM PlatformAccount pa " +
           "JOIN FETCH pa.platformType pt " +
           "JOIN FETCH pa.categoryType ct " +
           "WHERE pt.id = :platformTypeId " +
           "AND pa.isDeleted = false")
    List<PlatformAccount> findAllByPlatformTypeId(
        @Param("platformTypeId") String platformTypeId
    );

    Optional<PlatformAccount> findByAccountNickname(String username);
    List<PlatformAccount> findByPlatformTypeIdAndIsDeleted(String platformTypeId, Boolean isDeleted);

}