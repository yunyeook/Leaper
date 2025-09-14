package com.ssafy.leaper.domain.chat.dto.response;

import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import com.ssafy.leaper.domain.influencer.entity.Influencer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatRoomListResponse {
    private List<ChatRoomInfo> chatRooms;
    
    public static ChatRoomListResponse from(List<ChatRoomInfo> chatRooms) {
        return ChatRoomListResponse.builder()
                .chatRooms(chatRooms)
                .build();
    }
    
    @Getter
    @Builder
    public static class ChatRoomInfo {
        private Long chatRoomId;
        private PartnerInfo partner;
        private Boolean hasUnreadMessages;
        private LocalDateTime lastMessageTime;
        
        @Getter
        @Builder
        public static class PartnerInfo {
            private Integer id;
            private String name;
            private String profileImageUrl;
            
            // 인플루언서에서 변환 (프로필 이미지 URL은 서비스에서 별도 설정)
            public static PartnerInfo from(Influencer influencer, String profileImageUrl) {
                return PartnerInfo.builder()
                        .id(influencer.getId())
                        .name(influencer.getNickname())
                        .profileImageUrl(profileImageUrl)
                        .build();
            }
            
            // 광고주에서 변환 (프로필 이미지 URL은 서비스에서 별도 설정)
            public static PartnerInfo from(Advertiser advertiser, String profileImageUrl) {
                return PartnerInfo.builder()
                        .id(advertiser.getId())
                        .name(advertiser.getBrandName()) // 브랜드명을 이름으로 사용
                        .profileImageUrl(profileImageUrl)
                        .build();
            }
        }
    }
}
