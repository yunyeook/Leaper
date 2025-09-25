package com.ssafy.spark.domain.business.content.service;

import com.ssafy.spark.domain.business.content.entity.Content;
import com.ssafy.spark.domain.business.content.repository.ContentRepository;
import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.platformAccount.service.PlatformAccountService;
import com.ssafy.spark.domain.business.type.entity.ContentType;
import com.ssafy.spark.domain.business.type.entity.PlatformType;
import com.ssafy.spark.domain.business.type.service.ContentTypeService;
import com.ssafy.spark.domain.business.type.service.PlatformTypeService;
import com.ssafy.spark.domain.business.file.service.FileService;
import com.ssafy.spark.domain.business.file.entity.File;
import com.ssafy.spark.domain.crawling.youtube.dto.response.VideoInfoWithCommentsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Content 엔티티의 배치 Upsert 작업을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ContentUpsertService {

    private final ContentRepository contentRepository;
    private final ContentService contentService;
    private final PlatformAccountService platformAccountService;
    private final PlatformTypeService platformTypeService;
    private final ContentTypeService contentTypeService;
    private final FileService fileService;

    /**
     * YouTube 크롤링 데이터로부터 Content 엔티티 배치 Upsert
     */
    public List<Content> upsertContentsFromYouTubeVideos(List<VideoInfoWithCommentsResponse> videos, String externalAccountId) {
        if (videos == null || videos.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            log.info("Upsert videos from youTube");

            // 1. 필요한 기준 데이터 조회
            String platformTypeId = "YOUTUBE"; // YouTube 플랫폼 타입 ID
            PlatformType platformType = platformTypeService.findEntityByPlatformTypeId(platformTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("YouTube 플랫폼 타입을 찾을 수 없습니다: " + platformTypeId));

            PlatformAccount platformAccount = platformAccountService.findEntityByExternalAccountIdAndPlatformType(externalAccountId, platformTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("플랫폼 계정을 찾을 수 없습니다: " + externalAccountId));

            // 2. 기존 Content 조회 (배치로 한번에)
            List<String> externalContentIds = videos.stream()
                    .map(VideoInfoWithCommentsResponse::getExternalContentId)
                    .collect(Collectors.toList());

            Map<String, Content> existingContents = contentRepository
                    .findByExternalContentIdInAndPlatformTypeId(externalContentIds, platformTypeId)
                    .stream()
                    .collect(Collectors.toMap(Content::getExternalContentId, Function.identity()));

            // 3. Insert/Update 리스트 분리
            List<Content> toInsert = new ArrayList<>();
            List<Content> toUpdate = new ArrayList<>();

            for (VideoInfoWithCommentsResponse video : videos) {
                log.info("{}의 content_type_id : {}", video.getContentUrl(), video.getContentType());
                Content existingContent = existingContents.get(video.getExternalContentId());

                if (existingContent != null) {
                    // 기존 데이터 업데이트
                    updateContentFromVideo(existingContent, video);
                    toUpdate.add(existingContent);
                } else {
                    // 새 데이터 생성
                    Content newContent = createContentFromVideo(video, platformAccount, platformType);
                    toInsert.add(newContent);
                }
            }

            // 4. 배치 저장
            List<Content> result = new ArrayList<>();

            if (!toInsert.isEmpty()) {
                List<Content> inserted = contentRepository.saveAll(toInsert);
                result.addAll(inserted);
                log.info("새로운 Content {} 개 생성 완료", inserted.size());
            }

            if (!toUpdate.isEmpty()) {
                List<Content> updated = contentRepository.saveAll(toUpdate);
                result.addAll(updated);
                log.info("기존 Content {} 개 업데이트 완료", updated.size());
            }

            log.info("Content 배치 Upsert 완료: 총 {} 개 (신규: {}, 업데이트: {})",
                    result.size(), toInsert.size(), toUpdate.size());

            return result;

        } catch (Exception e) {
            log.error("Content 배치 Upsert 실패: externalAccountId={}", externalAccountId, e);
            throw new RuntimeException("Content 배치 Upsert 실패", e);
        }
    }

    /**
     * VideoInfoWithCommentsResponse로부터 새로운 Content 엔티티 생성
     */
    private Content createContentFromVideo(VideoInfoWithCommentsResponse video,
                                         PlatformAccount platformAccount,
                                         PlatformType platformType) {
        // Thumbnail ID 조회
        Integer thumbnailId = getThumbnailIdFromVideo(video);
        log.info("=== Content 생성 디버그 - videoId: {} ===", video.getExternalContentId());
        log.info("Description: {}", video.getDescription() != null ?
                video.getDescription().substring(0, Math.min(100, video.getDescription().length())) + "..." : "NULL");
        log.info("ThumbnailId: {}", thumbnailId);
        log.info("Title: {}", video.getTitle());

        return Content.builder()
                .platformAccount(platformAccount)
                .platformType(platformType)
                .contentType(getContentTypeByVideoType(video.getContentType())) // 비디오 콘텐츠 타입
                .externalContentId(video.getExternalContentId())
                .title(video.getTitle())
                .description(video.getDescription())
                .thumbnailId(thumbnailId) // Thumbnail ID 설정
                .durationSeconds(video.getDurationSeconds())
                .contentUrl(video.getContentUrl())
                .publishedAt(parsePublishedAt(video.getPublishedAt()))
                .tagsJson(video.getTags())
                .totalViews(BigInteger.valueOf(video.getViewsCount() != null ? video.getViewsCount() : 0))
                .totalLikes(BigInteger.valueOf(video.getLikesCount() != null ? video.getLikesCount() : 0))
                .totalComments(BigInteger.valueOf(video.getCommentsCount() != null ? video.getCommentsCount() : 0))
                .createdAt(LocalDateTime.now()) // 현재 시간으로 설정
                .updatedAt(LocalDateTime.now()) // 현재 시간으로 설정
                .snapshotDate(LocalDate.now()) // 오늘 날짜로 설정
                .build();
    }

    /**
     * 기존 Content 엔티티를 VideoInfoWithCommentsResponse 데이터로 업데이트
     */
    private void updateContentFromVideo(Content content, VideoInfoWithCommentsResponse video) {
        // JPA 더티 체킹을 위해 setter 방식 사용 (Builder는 새 객체 생성)
        // 변경 가능한 필드들만 업데이트
        content.setTitle(video.getTitle());
        content.setDescription(video.getDescription());

        // Thumbnail ID 업데이트
        Integer thumbnailId = getThumbnailIdFromVideo(video);
        if (thumbnailId != null) {
            content.setThumbnailId(thumbnailId);
        }

        content.setTagsJson(video.getTags());
        content.setTotalViews(BigInteger.valueOf(video.getViewsCount() != null ? video.getViewsCount() : 0));
        content.setTotalLikes(BigInteger.valueOf(video.getLikesCount() != null ? video.getLikesCount() : 0));
        content.setTotalComments(BigInteger.valueOf(video.getCommentsCount() != null ? video.getCommentsCount() : 0));
        content.setUpdatedAt(LocalDateTime.now()); // 수정 시간 업데이트
        content.setSnapshotDate(LocalDate.now()); // 스냅샷 날짜 업데이트
    }

    /**
     * 비디오 콘텐츠 타입 조회
     */
    private ContentType getContentTypeByVideoType(String contentType) {
        return contentTypeService.findEntityByContentTypeId(contentType)
                .orElseThrow(() -> new IllegalArgumentException("ContentType을 찾을 수 없습니다: " + contentType));
    }

    /**
     * 비디오에서 썸네일 파일 ID 조회
     */
    private Integer getThumbnailIdFromVideo(VideoInfoWithCommentsResponse video) {
        log.info("=== 썸네일 ID 조회 디버그 - videoId: {} ===", video.getExternalContentId());

        if (video.getThumbnailInfo() == null) {
            log.error("ThumbnailInfo가 null입니다 - videoId: {}", video.getExternalContentId());
            return null;
        }

        log.info("ThumbnailInfo 존재함 - videoId: {}", video.getExternalContentId());

        if (video.getThumbnailInfo().getAccessKey() == null) {
            log.error("ThumbnailInfo.accessKey가 null입니다 - videoId: {}", video.getExternalContentId());
            return null;
        }

        String accessKey = video.getThumbnailInfo().getAccessKey();
        log.info("AccessKey 조회됨 - videoId: {}, accessKey: {}", video.getExternalContentId(), accessKey);

        try {
            Optional<File> file = fileService.findEntityByAccessKey(accessKey);
            if (file.isPresent()) {
                log.info("SUCCESS: 썸네일 파일 조회 성공 - videoId: {}, fileId: {}",
                         video.getExternalContentId(), file.get().getId());
                return file.get().getId();
            } else {
                log.error("FAIL: 썸네일 파일을 찾을 수 없습니다 - videoId: {}, accessKey: {}",
                        video.getExternalContentId(), accessKey);
                return null;
            }
        } catch (Exception e) {
            log.error("ERROR: 썸네일 파일 조회 실패 - videoId: {}, accessKey: {}",
                     video.getExternalContentId(), accessKey, e);
            return null;
        }
    }

    /**
     * S3 저장 후 Content의 썸네일 ID 업데이트
     */
    public void updateContentThumbnailIds(List<VideoInfoWithCommentsResponse> videos, String externalAccountId) {
        if (videos == null || videos.isEmpty()) {
            log.info("업데이트할 비디오가 없습니다");
            return;
        }

        try {
            log.info("Content 썸네일 ID 업데이트 시작 - externalAccountId: {}, videos: {} 개",
                    externalAccountId, videos.size());

            String platformTypeId = "YOUTUBE";
            List<String> externalContentIds = videos.stream()
                    .map(VideoInfoWithCommentsResponse::getExternalContentId)
                    .collect(Collectors.toList());

            // 기존 Content 조회
            Map<String, Content> existingContents = contentRepository
                    .findByExternalContentIdInAndPlatformTypeId(externalContentIds, platformTypeId)
                    .stream()
                    .collect(Collectors.toMap(Content::getExternalContentId, Function.identity()));

            int updatedCount = 0;
            for (VideoInfoWithCommentsResponse video : videos) {
                Content content = existingContents.get(video.getExternalContentId());
                if (content != null) {
                    Integer thumbnailId = getThumbnailIdFromVideo(video);
                    if (thumbnailId != null && !thumbnailId.equals(content.getThumbnailId())) {
                        content.setThumbnailId(thumbnailId);
                        content.setUpdatedAt(LocalDateTime.now());
                        updatedCount++;
                        log.debug("썸네일 ID 업데이트 - videoId: {}, thumbnailId: {}",
                                video.getExternalContentId(), thumbnailId);
                    }
                }
            }

            if (updatedCount > 0) {
                contentRepository.saveAll(existingContents.values());
                log.info("Content 썸네일 ID 업데이트 완료 - {} 개", updatedCount);
            } else {
                log.info("업데이트된 썸네일 ID 없음");
            }

        } catch (Exception e) {
            log.error("Content 썸네일 ID 업데이트 실패: externalAccountId={}", externalAccountId, e);
            throw new RuntimeException("Content 썸네일 ID 업데이트 실패", e);
        }
    }

    /**
     * 문자열 날짜를 LocalDateTime으로 파싱
     */
    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null) {
            return null;
        }
        try {
            // YouTube API는 ISO 8601 형식 반환 (예: "2023-12-01T10:30:00Z")
            return LocalDateTime.parse(publishedAt.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", publishedAt, e);
            return null;
        }
    }
}