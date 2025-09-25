package com.ssafy.spark.domain.business.content.repository;

import com.ssafy.spark.domain.business.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentRepository extends JpaRepository<Content, Integer> {

    /**
     * externalContentId와 platformTypeId로 Content 조회
     */
    @Query("SELECT c FROM Content c " +
           "JOIN FETCH c.platformType pt " +
           "JOIN FETCH c.contentType ct " +
           "JOIN FETCH c.platformAccount pa " +
           "WHERE c.externalContentId = :externalContentId " +
           "AND pt.id = :platformTypeId")
    Optional<Content> findByExternalContentIdAndPlatformTypeId(
        @Param("externalContentId") String externalContentId,
        @Param("platformTypeId") String platformTypeId
    );

    /**
     * externalContentId와 platformTypeId로 Content 존재 여부 확인
     */
    @Query("SELECT COUNT(c) > 0 FROM Content c " +
           "JOIN c.platformType pt " +
           "WHERE c.externalContentId = :externalContentId " +
           "AND pt.id = :platformTypeId")
    boolean existsByExternalContentIdAndPlatformTypeId(
        @Param("externalContentId") String externalContentId,
        @Param("platformTypeId") String platformTypeId
    );

    /**
     * 여러 externalContentId와 platformTypeId로 Content 배치 조회 (IN 쿼리)
     */
    @Query("SELECT c FROM Content c " +
           "JOIN FETCH c.platformType pt " +
           "JOIN FETCH c.contentType ct " +
           "JOIN FETCH c.platformAccount pa " +
           "WHERE c.externalContentId IN :externalContentIds " +
           "AND pt.id = :platformTypeId")
    List<Content> findByExternalContentIdInAndPlatformTypeId(
        @Param("externalContentIds") List<String> externalContentIds,
        @Param("platformTypeId") String platformTypeId
    );

    /**
     * 특정 플랫폼 계정의 누적 통계 조회 (totalViews, totalLikes, totalComments, totalContents)
     */
    @Query("SELECT " +
           "COALESCE(SUM(c.totalViews), 0), " +
           "COALESCE(SUM(c.totalLikes), 0), " +
           "COALESCE(SUM(c.totalComments), 0), " +
           "COUNT(c) " +
           "FROM Content c " +
           "WHERE c.platformAccount.id = :platformAccountId")
    Object[] findAccountStats(@Param("platformAccountId") Integer platformAccountId);
}