package com.ssafy.leaper.domain.search.repository;

import com.ssafy.leaper.domain.search.dto.request.InfluencerSearchRequest;
import com.ssafy.leaper.domain.search.dto.response.InfluencerSearchResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SearchRepositoryImpl implements SearchRepository {

    private final EntityManager entityManager;

    @Override
    public List<InfluencerSearchResponse.InfluencerInfo> searchInfluencers(InfluencerSearchRequest request) {
        // 1. 조건에 맞는 인플루언서 기본 정보 조회
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT DISTINCT i.id, ")
            .append("CASE WHEN f.accessKey IS NOT NULL THEN f.accessKey ELSE '' END, ") // 프로필 이미지 URL
            .append("i.nickname ")
            .append("FROM Influencer i ")
            .append("LEFT JOIN i.profileImage f ") // 인플루언서 프로필 이미지
            .append("JOIN PlatformAccount pa ON pa.influencer.id = i.id AND pa.isDeleted = false ") // 플랫폼 계정 (INNER JOIN으로 계정이 있는 인플루언서만)
            .append("JOIN pa.platformType pt ") // 플랫폼 타입 정보 (필터링 필요하므로 INNER JOIN)
            .append("LEFT JOIN pa.categoryType ct ") // 카테고리 정보
            .append("LEFT JOIN DailyAccountInsight dai ON dai.platformAccount.id = pa.id ") // 팔로워 수 등 인사이트 정보
            .append("WHERE i.isDeleted = false "); // 삭제되지 않은 인플루언서만

        Map<String, Object> parameters = new HashMap<>();

        // 2. 검색 조건 추가

        // 키워드 검색: 닉네임 또는 bio에서 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            jpql.append("AND (LOWER(i.nickname) LIKE LOWER(:keyword) OR LOWER(i.bio) LIKE LOWER(:keyword)) ");
            parameters.put("keyword", "%" + request.getKeyword().trim() + "%");
        }

        // 플랫폼 필터: 특정 플랫폼만
        if (request.getPlatform() != null && !request.getPlatform().trim().isEmpty()) {
            jpql.append("AND pt.id = :platform ");
            parameters.put("platform", request.getPlatform().trim());
        }

        // 카테고리 필터: 특정 카테고리만
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            jpql.append("AND ct.categoryName = :category ");
            parameters.put("category", request.getCategory().trim());
        }

        // 콘텐츠 타입 필터: 해당 타입의 콘텐츠가 있는 플랫폼 계정만
        if (request.getContentType() != null && !request.getContentType().trim().isEmpty()) {
            // DEBUG: 실제 존재하는 contentType 값들 확인
            List<String> existingContentTypes = entityManager.createQuery(
                "SELECT DISTINCT ct.id FROM Content c JOIN c.contentType ct", String.class
            ).getResultList();
            log.info("실제 존재하는 contentType 값들: {}", existingContentTypes);
            log.info("요청된 contentType: {}", request.getContentType().trim());

            jpql.append("AND EXISTS (SELECT 1 FROM Content c WHERE c.platformAccount.id = pa.id AND c.contentType.id = :contentType) ");
            parameters.put("contentType", request.getContentType().trim());
        }

        // 팔로워 수 범위 필터
        if (request.getMinFollowers() != null) {
            jpql.append("AND dai.totalFollowers >= :minFollowers ");
            parameters.put("minFollowers", request.getMinFollowers());
        }

        if (request.getMaxFollowers() != null) {
            jpql.append("AND dai.totalFollowers <= :maxFollowers ");
            parameters.put("maxFollowers", request.getMaxFollowers());
        }

        jpql.append("ORDER BY i.id DESC"); // 최신 등록순 정렬

        // 3. 쿼리 실행
        log.info("실행할 JPQL: {}", jpql.toString());
        log.info("파라미터: {}", parameters);

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);

        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            query.setParameter(param.getKey(), param.getValue());
        }

        List<Object[]> results = query.getResultList();
        log.info("첫 번째 쿼리 결과 개수: {}", results.size());
        List<InfluencerSearchResponse.InfluencerInfo> influencers = new ArrayList<>();

        // 4. 결과 처리: 각 인플루언서의 플랫폼 계정 정보 조회
        for (Object[] row : results) {
            Integer influencerId = (Integer) row[0];
            String profileImageUrl = (String) row[1];
            String nickname = (String) row[2];

            // 해당 인플루언서의 조건에 맞는 플랫폼 계정들 조회
            List<InfluencerSearchResponse.PlatformAccountInfo> platformAccounts = getPlatformAccounts(influencerId, request);

            // 조건에 맞는 플랫폼 계정이 있는 경우만 결과에 포함
            if (!platformAccounts.isEmpty()) {
                InfluencerSearchResponse.InfluencerInfo influencerInfo = InfluencerSearchResponse.InfluencerInfo.of(
                    influencerId,
                    profileImageUrl,
                    nickname,
                    platformAccounts
                );

                influencers.add(influencerInfo);
            }
        }

        return influencers;
    }

    /**
     * 특정 인플루언서의 조건에 맞는 플랫폼 계정 정보들을 조회
     * @param influencerId 인플루언서 ID
     * @param request 검색 조건
     * @return 조건에 맞는 플랫폼 계정 정보 리스트
     */
    private List<InfluencerSearchResponse.PlatformAccountInfo> getPlatformAccounts(Integer influencerId, InfluencerSearchRequest request) {
        // 1. 플랫폼 계정 상세 정보 조회 쿼리 구성
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT DISTINCT pa.id, pt.id, ") // 플랫폼 계정 ID, 플랫폼 타입 ID
            .append("CASE WHEN paf.accessKey IS NOT NULL THEN paf.accessKey ELSE '' END, ") // 계정 프로필 이미지 URL
            .append("pa.accountNickname, pa.accountUrl, dai.totalFollowers, ct.categoryName ") // 계정 닉네임, URL, 팔로워 수, 카테고리명
            .append("FROM PlatformAccount pa ")
            .append("JOIN pa.platformType pt ") // 플랫폼 타입 (INNER JOIN)
            .append("LEFT JOIN pa.accountProfileImage paf ") // 계정 프로필 이미지
            .append("LEFT JOIN pa.categoryType ct ") // 카테고리 타입
            .append("LEFT JOIN DailyAccountInsight dai ON dai.platformAccount.id = pa.id ") // 인사이트 정보 (팔로워 수 등)
            .append("WHERE pa.influencer.id = :influencerId AND pa.isDeleted = false "); // 특정 인플루언서의 삭제되지 않은 계정만

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("influencerId", influencerId);

        // 2. 검색 조건별 필터링 (위에서 조회한 인플루언서에 대해 다시 한 번 플랫폼 계정 레벨에서 필터링)

        // 플랫폼 필터: 특정 플랫폼의 계정만
        if (request.getPlatform() != null && !request.getPlatform().trim().isEmpty()) {
            jpql.append("AND pt.id = :platform ");
            parameters.put("platform", request.getPlatform().trim());
        }

        // 카테고리 필터: 특정 카테고리의 계정만
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            jpql.append("AND ct.categoryName = :category ");
            parameters.put("category", request.getCategory().trim());
        }

        // 콘텐츠 타입 필터: 해당 타입의 콘텐츠를 가진 계정만
        if (request.getContentType() != null && !request.getContentType().trim().isEmpty()) {
            jpql.append("AND EXISTS (SELECT 1 FROM Content c WHERE c.platformAccount.id = pa.id AND c.contentType.id = :contentType) ");
            parameters.put("contentType", request.getContentType().trim());
        }

        // 팔로워 수 범위 필터
        if (request.getMinFollowers() != null) {
            jpql.append("AND dai.totalFollowers >= :minFollowers ");
            parameters.put("minFollowers", request.getMinFollowers());
        }

        if (request.getMaxFollowers() != null) {
            jpql.append("AND dai.totalFollowers <= :maxFollowers ");
            parameters.put("maxFollowers", request.getMaxFollowers());
        }

        jpql.append("ORDER BY pa.id"); // 계정 ID 순 정렬

        // 3. 쿼리 실행
        log.info("getPlatformAccounts JPQL: {}", jpql.toString());
        log.info("getPlatformAccounts 파라미터: {}", parameters);

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);

        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            query.setParameter(param.getKey(), param.getValue());
        }

        List<Object[]> results = query.getResultList();
        log.info("getPlatformAccounts 결과 개수 (influencerId={}): {}", influencerId, results.size());
        List<InfluencerSearchResponse.PlatformAccountInfo> platformAccounts = new ArrayList<>();

        // 4. 조회 결과를 DTO로 변환
        for (Object[] row : results) {
            Integer platformAccountId = (Integer) row[0];     // 플랫폼 계정 ID
            String platformTypeId = (String) row[1];          // 플랫폼 타입 ID (예: "INSTAGRAM", "YOUTUBE")
            String accountProfileImageUrl = (String) row[2];  // 계정 프로필 이미지 URL
            String accountNickname = (String) row[3];         // 계정 닉네임
            String accountURL = (String) row[4];              // 계정 URL
            Integer totalFollowers = row[5] != null ? (Integer) row[5] : 0; // 팔로워 수 (null이면 0)
            String categoryName = (String) row[6];            // 카테고리명

            InfluencerSearchResponse.PlatformAccountInfo accountInfo = InfluencerSearchResponse.PlatformAccountInfo.of(
                platformAccountId,
                platformTypeId,
                accountProfileImageUrl,
                accountNickname,
                accountURL,
                totalFollowers,
                categoryName
            );

            platformAccounts.add(accountInfo);
        }

        return platformAccounts;
    }

}