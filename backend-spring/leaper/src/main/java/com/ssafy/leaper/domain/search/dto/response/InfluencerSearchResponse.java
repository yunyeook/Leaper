package com.ssafy.leaper.domain.search.dto.response;

import java.util.List;

public record InfluencerSearchResponse(
    List<InfluencerInfo> influencers
) {

    public static InfluencerSearchResponse from(List<InfluencerInfo> influencers) {
        return new InfluencerSearchResponse(influencers);
    }

    public record InfluencerInfo(
        Integer influencerId,
        String influencerProfileImageUrl,
        String nickname,
        List<PlatformAccountInfo> platformAccounts
    ) {

        public static InfluencerInfo of(
            Integer influencerId,
            String influencerProfileImageUrl,
            String nickname,
            List<PlatformAccountInfo> platformAccounts
        ) {
            return new InfluencerInfo(
                influencerId,
                influencerProfileImageUrl,
                nickname,
                platformAccounts
            );
        }
    }

    public record PlatformAccountInfo(
        Integer platformAccountId,
        String platformTypeId,
        String accountProfileImageUrl,
        String accountNickname,
        String accountURL,
        Integer totalFollowers,
        String categoryName
    ) {

        public static PlatformAccountInfo of(
            Integer platformAccountId,
            String platformTypeId,
            String accountProfileImageUrl,
            String accountNickname,
            String accountURL,
            Integer totalFollowers,
            String categoryName
        ) {
            return new PlatformAccountInfo(
                platformAccountId,
                platformTypeId,
                accountProfileImageUrl,
                accountNickname,
                accountURL,
                totalFollowers,
                categoryName
            );
        }
    }
}