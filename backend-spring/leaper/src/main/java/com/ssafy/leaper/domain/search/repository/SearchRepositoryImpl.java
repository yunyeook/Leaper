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
        // 1. 조건에 맞는 플랫폼 계정과 인플루언서 정보를 한 번에 조회
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT i.id, ")
            .append("inf.id, ") // 인플루언서 프로필 이미지 File ID
            .append("i.nickname, ") // 인플루언서 닉네임
            .append("pa.id, ") // 플랫폼 계정 ID
            .append("pt.id, ") // 플랫폼 타입 ID
            .append("paf.id, ") // 계정 프로필 이미지 File ID
            .append("pa.accountNickname, ") // 계정 닉네임
            .append("pa.accountUrl, ") // 계정 URL
            .append("COALESCE(dai.totalFollowers, 0), ") // 팔로워 수
            .append("COALESCE(ct.categoryName, '') ") // 카테고리명
            .append("FROM Influencer i ")
            .append("LEFT JOIN i.profileImage inf ") // 인플루언서 프로필 이미지
            .append("JOIN PlatformAccount pa ON pa.influencer.id = i.id AND pa.isDeleted = false ") // 플랫폼 계정
            .append("JOIN pa.platformType pt ") // 플랫폼 타입
            .append("LEFT JOIN pa.accountProfileImage paf ") // 계정 프로필 이미지
            .append("LEFT JOIN pa.categoryType ct ") // 카테고리
            .append("LEFT JOIN DailyAccountInsight dai ON dai.platformAccount.id = pa.id AND dai.snapshotDate = CURRENT_DATE - 1 DAY ") // 어제 인사이트만
            .append("WHERE i.isDeleted = false "); // 삭제되지 않은 인플루언서만

        Map<String, Object> parameters = new HashMap<>();

        // 2. 검색 조건 추가

        // 키워드 검색: 인플루언서 닉네임 또는 계정 닉네임에서 검색
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            jpql.append("AND (LOWER(i.nickname) LIKE LOWER(:keyword) OR LOWER(pa.accountNickname) LIKE LOWER(:keyword)) ");
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
            jpql.append("AND EXISTS (SELECT 1 FROM Content c WHERE c.platformAccount.id = pa.id AND c.contentType.id = :contentType) ");
            parameters.put("contentType", request.getContentType().trim());
        }

        // 팔로워 수 범위 필터
        if (request.getMinFollowers() != null) {
            jpql.append("AND dai.totalFollowers IS NOT NULL AND dai.totalFollowers >= :minFollowers ");
            parameters.put("minFollowers", request.getMinFollowers());
        }

        if (request.getMaxFollowers() != null) {
            jpql.append("AND dai.totalFollowers IS NOT NULL AND dai.totalFollowers <= :maxFollowers ");
            parameters.put("maxFollowers", request.getMaxFollowers());
        }

        jpql.append("ORDER BY i.nickname DESC NULLS LAST, dai.totalFollowers DESC NULLS LAST, i.id DESC, pa.id ASC"); // likeScore 내림차순, 팔로워 수 내림차순, 인플루언서별로 정렬, 계정별로 정렬

        // 3. 쿼리 실행
        log.info("실행할 JPQL: {}", jpql.toString());
        log.info("파라미터: {}", parameters);

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);

        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            query.setParameter(param.getKey(), param.getValue());
        }

        // TODO : 결과를 150개로 제한 -> 페이지네이션으로 구현할 것
        query.setMaxResults(50);

        List<Object[]> results = query.getResultList();
        log.info("쿼리 결과 개수: {}", results.size());

        // 4. 결과를 인플루언서별로 그룹핑하여 처리
        Map<Integer, InfluencerData> influencerMap = new HashMap<>();

        for (Object[] row : results) {
            Integer influencerId = (Integer) row[0];
            Integer influencerProfileImageId = (Integer) row[1];
            String influencerNickname = (String) row[2];
            Integer platformAccountId = (Integer) row[3];
            String platformTypeId = (String) row[4];
            Integer accountProfileImageId = (Integer) row[5];
            String accountNickname = (String) row[6];
            String accountURL = (String) row[7];
            Integer totalFollowers = (Integer) row[8];
            String categoryName = (String) row[9];

            // 인플루언서 정보가 맵에 없으면 새로 생성
            influencerMap.computeIfAbsent(influencerId, k ->
                new InfluencerData(influencerId, influencerProfileImageId, influencerNickname, new ArrayList<>())
            );

            // 플랫폼 계정 정보 추가
            InfluencerSearchResponse.PlatformAccountInfo accountInfo = InfluencerSearchResponse.PlatformAccountInfo.of(
                platformAccountId,
                platformTypeId,
                accountProfileImageId != null ? accountProfileImageId.toString() : null, // File ID를 String으로 변환
                accountNickname,
                accountURL,
                totalFollowers,
                categoryName
            );

            influencerMap.get(influencerId).platformAccounts.add(accountInfo);
        }

        // 5. 최종 결과 생성
        List<InfluencerSearchResponse.InfluencerInfo> influencers = new ArrayList<>();
        for (InfluencerData data : influencerMap.values()) {
            InfluencerSearchResponse.InfluencerInfo influencerInfo = InfluencerSearchResponse.InfluencerInfo.of(
                data.influencerId,
                data.profileImageId != null ? data.profileImageId.toString() : null, // File ID를 String으로 변환
                data.nickname,
                data.platformAccounts
            );
            influencers.add(influencerInfo);
        }

        log.info("최종 결과 인플루언서 수: {}", influencers.size());
        return influencers;
    }

    // 내부 데이터 클래스
    private static class InfluencerData {
        final Integer influencerId;
        final Integer profileImageId;
        final String nickname;
        final List<InfluencerSearchResponse.PlatformAccountInfo> platformAccounts;

        InfluencerData(Integer influencerId, Integer profileImageId, String nickname,
                      List<InfluencerSearchResponse.PlatformAccountInfo> platformAccounts) {
            this.influencerId = influencerId;
            this.profileImageId = profileImageId;
            this.nickname = nickname;
            this.platformAccounts = platformAccounts;
        }
    }


}