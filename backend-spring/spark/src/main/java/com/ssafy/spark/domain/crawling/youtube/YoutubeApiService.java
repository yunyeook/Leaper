package com.ssafy.spark.domain.crawling.youtube;

import com.ssafy.spark.domain.crawling.youtube.dto.*;
import com.ssafy.spark.domain.crawling.youtube.dto.response.*;
import com.ssafy.spark.domain.crawling.youtube.dto.response.raw.*;
import com.ssafy.spark.global.config.YoutubeApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeApiService {

    private final YoutubeApiConfig config;
    private final WebClient youtubeWebClient;

    // 채널명 캐시를 위한 맵
    private final java.util.Map<String, String> channelIdToNameMap = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 채널 정보 조회 (구독자 수, 비디오 수 등 통계 포함)
     */
    public Mono<YoutubeChannelInfo> getChannelInfo(String channelId) {
        return youtubeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/channels")
                        .queryParam("part", "snippet,statistics,brandingSettings")
                        .queryParam("id", channelId)
                        .queryParam("key", config.getKey())
                        .build())
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            if (response.statusCode().value() == 403) {
                                log.error("YouTube API 403 오류: API 키 확인 필요 또는 할당량 초과");
                                return Mono.error(new RuntimeException("YouTube API 권한 오류: API 키를 확인하거나 할당량이 초과되었습니다."));
                            }
                            log.error("YouTube API 오류: {}", response.statusCode());
                            return response.createException();
                        }
                )
                .bodyToMono(RawYoutubeChannelResponse.class)
                .map(response -> {
                    if (response.getItems() != null && !response.getItems().isEmpty()) {
                        var channel = response.getItems().get(0);
                        YoutubeChannelInfo channelInfo = new YoutubeChannelInfo();

            channelInfo.setExternalAccountId(channel.getId());

                        if (channel.getSnippet() != null) {
                            // customUrl이 있으면 사용, 없으면 채널 제목 사용
                            String accountNickname = channel.getSnippet().getCustomUrl();
                            if (accountNickname == null || accountNickname.trim().isEmpty()) {
                                accountNickname = channel.getSnippet().getTitle();
                            }
                            channelInfo.setAccountNickname(accountNickname);
                        }

                        if (channel.getStatistics() != null) {
                            channelInfo.setFollowersCount(parseLong(channel.getStatistics().getSubscriberCount()));
                            channelInfo.setPostsCount(parseLong(channel.getStatistics().getVideoCount()));
                        }

                        return channelInfo;
                    }
                    throw new RuntimeException("채널을 찾을 수 없습니다: " + channelId);
                })
                .doOnNext(channelInfo -> log.info("채널 정보: {}", channelInfo));
    }

    /**
     * 채널명으로 채널 정보 조회
     */
    public Mono<YoutubeChannelInfo> getChannelInfoByName(String channelName) {
        return getChannelIdByName(channelName)
                .flatMap(channelId -> {
                    // 채널ID와 채널명 매핑 저장
                    String normalizedChannelName = channelName.startsWith("@") ? channelName : "@" + channelName;
                    channelIdToNameMap.put(channelId, normalizedChannelName);

                    return getChannelInfo(channelId)
                            .map(channelInfo -> {
                                // 검색에 사용된 channelName이 핸들 형태라면 그것을 사용
                                if (channelName.startsWith("@")) {
                                    channelInfo.setAccountNickname(channelName);
                                }
                                return channelInfo;
                            });
                });
    }


    /**
     * 채널 ID로 업로드 플레이리스트 ID를 가져옴
     */
    private Mono<String> getUploadPlaylistId(String channelId) {
        return youtubeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/channels")
                        .queryParam("part", "contentDetails")
                        .queryParam("id", channelId)
                        .queryParam("key", config.getKey())
                        .build())
                .retrieve()
                .bodyToMono(RawYoutubeChannelResponse.class)
                .map(response -> {
                    if (response.getItems() != null && !response.getItems().isEmpty()) {
                        return response.getItems().get(0)
                                .getContentDetails()
                                .getRelatedPlaylists()
                                .getUploads();
                    }
                    throw new RuntimeException("채널을 찾을 수 없습니다: " + channelId);
                })
                .doOnNext(playlistId -> log.info("업로드 플레이리스트 ID: {}", playlistId));
    }

    /**
     * 채널명으로 채널 ID를 검색
     */
    private Mono<String> getChannelIdByName(String channelName) {
        log.info("API 키 확인: {}", config.getKey() != null ? "설정됨" : "설정되지 않음");
        log.info("채널명 검색: {}", channelName);

        return youtubeWebClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("type", "channel")
                            .queryParam("q", channelName)
                            .queryParam("key", config.getKey())
                            .queryParam("maxResults", 1)
                            .build();
                    log.info("요청 URL: {}", uri.toString());
                    return uri;
                })
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            if (response.statusCode().value() == 403) {
                                log.error("YouTube API 403 오류: API 키 확인 필요 또는 할당량 초과");
                                return Mono.error(new RuntimeException("YouTube API 권한 오류: API 키를 확인하거나 할당량이 초과되었습니다."));
                            }
                            log.error("YouTube API 오류: {}", response.statusCode());
                            return response.createException();
                        }
                )
                .bodyToMono(RawYoutubeVideoListResponse.class)
                .map(response -> {
                    if (response.getItems() != null && !response.getItems().isEmpty()) {
                        YoutubeVideo firstResult = response.getItems().get(0);
                        // Search API에서는 snippet.channelId를 사용하거나 id에서 channelId를 추출
                        String channelId = firstResult.getChannelIdFromId();
                        if (channelId == null) {
                            channelId = firstResult.getSnippet().getChannelId();
                        }
                        log.info("찾은 채널 ID: {}", channelId);

                        // 검색 결과에서 실제 채널 타이틀도 로깅해보기
                        log.info("검색된 채널 타이틀: {}", firstResult.getSnippet().getTitle());

                        return channelId;
                    }
                    throw new RuntimeException("채널을 찾을 수 없습니다: " + channelName);
                });
    }

    /**
     * 플레이리스트의 모든 비디오를 페이지별로 가져옴
     */
    private Flux<YoutubeVideo> getAllVideosFromPlaylist(String playlistId) {
        return getVideosFromPlaylist(playlistId, null)
                .expand(response -> {
                    if (response.getNextPageToken() != null) {
                        return getVideosFromPlaylist(playlistId, response.getNextPageToken());
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMapIterable(RawYoutubeVideoListResponse::getItems);
    }

    /**
     * 특정 페이지의 비디오들을 가져옴
     */
    private Mono<RawYoutubeVideoListResponse> getVideosFromPlaylist(String playlistId, String pageToken) {
        return youtubeWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/playlistItems")
                            .queryParam("part", "snippet")
                            .queryParam("playlistId", playlistId)
                            .queryParam("key", config.getKey())
                            .queryParam("maxResults", 50); // 최대 50개씩

                    if (pageToken != null) {
                        builder.queryParam("pageToken", pageToken);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(RawYoutubeVideoListResponse.class)
                .doOnNext(response ->
                        log.info("페이지 토큰: {}, 비디오 개수: {}",
                                pageToken, response.getItems().size()));
    }

    /**
     * 특정 채널의 모든 비디오를 가져오는 메인 메서드
     */
    private Flux<YoutubeVideo> getAllVideosFromChannel(String channelId) {
        return getUploadPlaylistId(channelId)
                .flatMapMany(this::getAllVideosFromPlaylist);
    }

    /**
     * 특정 비디오의 댓글을 가져옴
     */
    public Flux<CommentThread> getVideoComments(String videoId, Integer maxResults) {
        if (maxResults == null) {
            maxResults = 20; // 기본값
        }

        Integer finalMaxResults = maxResults;
        return getCommentsFromVideo(videoId, null, maxResults)
                .expand(response -> {
                    if (response.getNextPageToken() != null) {
                        return getCommentsFromVideo(videoId, response.getNextPageToken(), finalMaxResults);
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMapIterable(RawYoutubeCommentResponse::getItems);
    }

    /**
     * 특정 페이지의 댓글들을 가져옴
     */
    private Mono<RawYoutubeCommentResponse> getCommentsFromVideo(String videoId, String pageToken, Integer maxResults) {
        return youtubeWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/commentThreads")
                            .queryParam("part", "snippet")
                            .queryParam("videoId", videoId)
                            .queryParam("key", config.getKey())
                            .queryParam("maxResults", Math.min(maxResults, 100)) // 최대 100개
                            .queryParam("order", "relevance"); // 관련성순 정렬

                    if (pageToken != null) {
                        builder.queryParam("pageToken", pageToken);
                    }
                    return builder.build();
                })
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> {
                            if (response.statusCode().value() == 403) {
                                log.warn("댓글이 비활성화된 비디오: {}", videoId);
                                return Mono.error(new RuntimeException("댓글이 비활성화된 비디오입니다: " + videoId));
                            }
                            return response.createException();
                        }
                )
                .bodyToMono(RawYoutubeCommentResponse.class)
                .doOnNext(response ->
                        log.info("비디오 {} - 페이지 토큰: {}, 댓글 개수: {}",
                                videoId, pageToken, response.getItems() != null ? response.getItems().size() : 0))
                .onErrorReturn(new RawYoutubeCommentResponse()) // 에러시 빈 응답 반환
                .filter(response -> response.getItems() != null); // null items 필터링
    }

    /**
     * 특정 비디오 정보만 조회 (댓글 제외)
     */
    public Mono<YoutubeVideoWithComments> getVideoInfo(String videoId) {
        return getVideoDetails(videoId)
                .map(video -> {
                    YoutubeVideoWithComments result = YoutubeVideoWithComments.fromYouTubeVideo(video);
                    result.setComments(new ArrayList<>()); // 빈 댓글 리스트
                    return result;
                });
    }

    /**
     * 특정 비디오와 댓글을 함께 조회
     */
    public Mono<YoutubeVideoWithComments> getVideoWithComments(String videoId, Integer maxComments) {
        return getVideoDetails(videoId)
                .flatMap(video -> {
                    YoutubeVideoWithComments result = YoutubeVideoWithComments.fromYouTubeVideo(video);

                    return getVideoComments(videoId, maxComments)
                            .map(this::convertToCommentInfo)
                            .filter(comment -> comment != null) // null 댓글 필터링
                            .collectList()
                            .map(comments -> {
                                result.setComments(comments);
                                return result;
                            })
                            .onErrorResume(throwable -> {
                                if (throwable.getMessage() != null && throwable.getMessage().contains("댓글이 비활성화된 비디오")) {
                                    // 댓글이 비활성화된 경우 특별한 댓글 추가
                                    YoutubeVideoWithComments.CommentInfo disabledComment = createDisabledCommentInfo();
                                    result.setComments(java.util.Arrays.asList(disabledComment));
                                    return Mono.just(result);
                                }
                                // 다른 에러의 경우 빈 댓글 리스트
                                result.setComments(new ArrayList<>());
                                return Mono.just(result);
                            });
                });
    }

    /**
     * 비디오 상세 정보 조회
     */
    private Mono<YoutubeVideo> getVideoDetails(String videoId) {
        return youtubeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet,statistics,contentDetails")
                        .queryParam("id", videoId)
                        .queryParam("key", config.getKey())
                        .build())
                .retrieve()
                .bodyToMono(RawYoutubeVideoListResponse.class)
                .map(response -> {
                    if (response.getItems() != null && !response.getItems().isEmpty()) {
                        YoutubeVideo video = response.getItems().get(0);

                        return video;
                    }
                    throw new RuntimeException("비디오를 찾을 수 없습니다: " + videoId);
                });
    }

    /**
     * 채널의 모든 비디오와 댓글을 함께 가져옴
     */
    public Flux<YoutubeVideoWithComments> getAllVideosWithCommentsFromChannel(String channelId, Integer maxCommentsPerVideo) {
        return getAllVideosFromChannel(channelId)
                .flatMap(video -> {
                    final String videoId;
                    if (video.getSnippet().getResourceId() != null) {
                        videoId = video.getSnippet().getResourceId().getVideoId();
                    } else {
                        videoId = video.getVideoId();
                    }

                    if (videoId == null) {
                        log.warn("비디오 ID를 찾을 수 없습니다: {}", video);
                        return Mono.empty();
                    }

                    // 비디오 상세 정보(통계 포함)를 가져온 후 댓글과 결합
                    return getVideoDetails(videoId)
                            .flatMap(detailedVideo -> {
                                YoutubeVideoWithComments result = YoutubeVideoWithComments.fromYouTubeVideo(detailedVideo);

                                if (maxCommentsPerVideo == null || maxCommentsPerVideo <= 0) {
                                    // 댓글을 요청하지 않는 경우
                                    result.setComments(new ArrayList<>());
                                    return Mono.just(result);
                                }

                                return getVideoComments(videoId, maxCommentsPerVideo)
                                        .map(this::convertToCommentInfo)
                                        .filter(comment -> comment != null) // null 댓글 필터링
                                        .collectList()
                                        .map(comments -> {
                                            result.setComments(comments);
                                            return result;
                                        })
                                        .onErrorResume(throwable -> {
                                            if (throwable.getMessage() != null && throwable.getMessage().contains("댓글이 비활성화된 비디오")) {
                                                // 댓글이 비활성화된 경우 특별한 댓글 추가
                                                YoutubeVideoWithComments.CommentInfo disabledComment = createDisabledCommentInfo();
                                                result.setComments(java.util.Arrays.asList(disabledComment));
                                                return Mono.just(result);
                                            }
                                            // 다른 에러의 경우 빈 댓글 리스트
                                            result.setComments(new ArrayList<>());
                                            return Mono.just(result);
                                        });
                            })
                            .onErrorReturn(YoutubeVideoWithComments.fromYouTubeVideo(video)); // 비디오 상세 정보 가져오기 실패시 기본 정보만 반환
                }, 2); // 동시 처리 제한 (API 제한 고려)
    }

    /**
     * CommentThread를 CommentInfo로 변환
     */
    private YoutubeVideoWithComments.CommentInfo convertToCommentInfo(CommentThread commentThread) {
        if (commentThread == null || commentThread.getSnippet() == null) {
            return null;
        }

        var topLevelComment = commentThread.getSnippet().getTopLevelComment();
        if (topLevelComment == null || topLevelComment.getSnippet() == null) {
            return null;
        }

        var comment = topLevelComment.getSnippet();
        var commentInfo = new YoutubeVideoWithComments.CommentInfo();

        commentInfo.setCommentId(commentThread.getId());
        commentInfo.setAuthorName(comment.getAuthorDisplayName());
        commentInfo.setAuthorProfileImage(comment.getAuthorProfileImageUrl());
        commentInfo.setCommentText(comment.getTextDisplay());
        commentInfo.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        commentInfo.setPublishedAt(comment.getPublishedAt());
        commentInfo.setUpdatedAt(comment.getUpdatedAt());
        commentInfo.setReplyCount(commentThread.getSnippet().getTotalReplyCount() != null ?
                                   commentThread.getSnippet().getTotalReplyCount() : 0);

        return commentInfo;
    }

    /**
     * 댓글이 비활성화된 경우를 나타내는 특별한 댓글 정보 생성
     */
    private YoutubeVideoWithComments.CommentInfo createDisabledCommentInfo() {
        YoutubeVideoWithComments.CommentInfo disabledComment = new YoutubeVideoWithComments.CommentInfo();
        disabledComment.setCommentId("COMMENTS_DISABLED");
        disabledComment.setAuthorName("SYSTEM");
        disabledComment.setAuthorProfileImage("");
        disabledComment.setCommentText("댓글이 비활성화되어 있습니다.");
        disabledComment.setLikeCount(0);
        disabledComment.setPublishedAt("");
        disabledComment.setUpdatedAt("");
        disabledComment.setReplyCount(0);
        return disabledComment;
    }

    /**
     * 채널 ID로 저장된 채널명 가져오기
     */
    public String getCachedChannelName(String channelId) {
        return channelIdToNameMap.getOrDefault(channelId, null);
    }

    /**
     * 채널 ID로 실제 채널 핸들 가져오기 (customUrl 우선)
     */
    public Mono<String> getChannelHandle(String channelId) {
        return youtubeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/channels")
                        .queryParam("part", "snippet")
                        .queryParam("id", channelId)
                        .queryParam("key", config.getKey())
                        .build())
                .retrieve()
                .bodyToMono(RawYoutubeChannelResponse.class)
                .map(response -> {
                    if (response.getItems() != null && !response.getItems().isEmpty()) {
                        var channel = response.getItems().get(0);
                        String customUrl = channel.getSnippet().getCustomUrl();
                        String title = channel.getSnippet().getTitle();
                        String handle = channel.getSnippet().getHandle();

                        // handle 필드가 있고 @로 시작하면 우선 사용
                        if (handle != null && !handle.trim().isEmpty() && handle.startsWith("@")) {
                            log.info("Handle 필드 사용: {}", handle);
                            return handle;
                        }

                        // customUrl이 있으면 사용
                        if (customUrl != null && !customUrl.trim().isEmpty()) {
                            String result = customUrl.startsWith("@") ? customUrl : "@" + customUrl;
                            log.info("CustomUrl 사용: {}", result);
                            return result;
                        }

                        // 마지막으로 제목 사용
                        String result = title.startsWith("@") ? title : "@" + title;
                        log.info("Title 사용: {}", result);
                        return result;
                    }
                    return null;
                })
                .onErrorReturn("");
    }


    /**
     * 채널 정보 조회 (ChannelInfoResponse 반환)
     */
    public Mono<ChannelInfoResponse> getChannelInfoResponse(String externalAccountId) {
        return getChannelInfo(externalAccountId)
                .flatMap(channelInfo -> {
                    return getChannelHandle(externalAccountId)
                            .map(accountNickname -> {
                                ChannelInfoResponse response = new ChannelInfoResponse();
                                response.setExternalAccountId(channelInfo.getExternalAccountId());
                                response.setAccountNickname(accountNickname);
                                // TODO: categoryName 넣는 방식 설정
                                response.setCategoryName(null);
                                response.setAccountUrl("https://youtube.com/channel/" + externalAccountId);
                                response.setFollowersCount(channelInfo.getFollowersCount());
                                response.setPostsCount(channelInfo.getPostsCount());
                                response.setCrawledAt(java.time.LocalDateTime.now().toString());
                                return response;
                            });
                });
    }

    /**
     * 채널 정보 + 비디오 + 댓글 통합 조회
     */
    public Mono<ChannelWithVideosResponse> getChannelWithVideosResponse(String externalAccountId, Integer maxCommentsPerVideo) {
        Mono<ChannelInfoResponse> channelInfo = getChannelInfoResponse(externalAccountId);

        Mono<List<VideoInfoWithCommentsResponse>> videos = getChannelVideosWithCommentsAndHandle(externalAccountId, maxCommentsPerVideo)
                .collectList();

        return Mono.zip(channelInfo, videos)
                .map(tuple -> new ChannelWithVideosResponse(tuple.getT1(), tuple.getT2()));
    }

    /**
     * 채널 핸들 조회 (캐시 우선)
     */
    private Mono<String> getChannelHandleWithCache(String channelId) {
        String cachedChannelName = getCachedChannelName(channelId);
        if (cachedChannelName != null) {
            return Mono.just(cachedChannelName);
        } else {
            return getChannelHandle(channelId);
        }
    }

    /**
     * 비디오 제목을 기반으로 contentType 결정
     */
    public String determineContentType(String title) {
        if (title != null && title.toLowerCase().contains("#shorts")) {
            return "VIDEO_SHORT";
        }
        return "VIDEO_LONG";
    }

    /**
     * YoutubeVideoWithComments를 VideoInfoResponse로 변환
     */
    public VideoInfoResponse convertToVideoInfoResponse(YoutubeVideoWithComments video, String accountNickname) {
        VideoInfoResponse response = new VideoInfoResponse();
        response.setAccountNickname(accountNickname);
        response.setExternalContentId(video.getVideoId());
        response.setPlatformType("YOUTUBE");
        response.setContentType(determineContentType(video.getTitle()));
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setDurationSeconds(video.getDurationSeconds());
        response.setContentUrl("https://www.youtube.com/watch?v=" + video.getVideoId());
        response.setPublishedAt(video.getPublishedAt());
        response.setTags(video.getTags());
        response.setViewsCount(video.getViewsCount());
        response.setLikesCount(video.getLikesCount());
        response.setCommentsCount(video.getCommentsCount());

        response.setThumbnailInfo(createThumbnailInfo(video));

        return response;
    }

    /**
     * 비디오 정보 조회 (채널 핸들 조회 포함)
     */
    public Mono<VideoInfoResponse> getVideoInfoResponseWithHandle(String videoId) {
        return getVideoInfo(videoId)
                .flatMap(video ->
                    getChannelHandleWithCache(video.getChannelId())
                        .map(accountNickname -> convertToVideoInfoResponse(video, accountNickname))
                );
    }

    /**
     * 비디오 정보 + 댓글 조회 (채널 핸들 조회 포함)
     */
    public Mono<VideoInfoWithCommentsResponse> getVideoInfoWithCommentsResponseWithHandle(String videoId, Integer maxComments) {
        return getVideoWithComments(videoId, maxComments)
                .flatMap(video ->
                    getChannelHandleWithCache(video.getChannelId())
                        .map(accountNickname -> convertToVideoInfoWithCommentsResponse(video, accountNickname))
                );
    }

    /**
     * 채널의 모든 비디오 조회 (채널 핸들 조회 포함)
     */
    public Flux<VideoInfoResponse> getChannelVideosWithHandle(String externalAccountId) {
        return getAllVideosWithCommentsFromChannel(externalAccountId, 0) // 댓글 제외, 비디오 정보만
                .flatMap(video ->
                    getChannelHandleWithCache(video.getChannelId())
                        .map(accountNickname -> convertToVideoInfoResponse(video, accountNickname))
                );
    }

    /**
     * 채널의 모든 비디오 + 댓글 조회 (채널 핸들 조회 포함)
     */
    public Flux<VideoInfoWithCommentsResponse> getChannelVideosWithCommentsAndHandle(String externalAccountId, Integer maxCommentsPerVideo) {
        return getAllVideosWithCommentsFromChannel(externalAccountId, maxCommentsPerVideo)
                .flatMap(video -> {
                    return getChannelHandleWithCache(video.getChannelId())
                            .map(accountNickname ->
                                convertToVideoInfoWithCommentsResponse(video, accountNickname)
                            );
                });
    }

    /**
     * YoutubeVideoWithComments를 VideoWithCommentsResponse로 변환
     */
    public VideoWithCommentsResponse convertToVideoWithCommentsResponse(YoutubeVideoWithComments video) {
        VideoWithCommentsResponse response = new VideoWithCommentsResponse();

        response.setContentId(video.getVideoId());
        response.setContentUrl("https://www.youtube.com/watch?v=" + video.getVideoId());
        response.setContentType(determineContentType(video.getTitle()));
        response.setCommentsCount(video.getCommentsCount());

        List<VideoWithCommentsResponse.CommentInfo> commentInfos = new java.util.ArrayList<>();
        if (video.getComments() != null) {
            for (YoutubeVideoWithComments.CommentInfo comment : video.getComments()) {
                commentInfos.add(convertToCommentInfo(comment));
            }
        }
        response.setComments(commentInfos);

        return response;
    }

    /**
     * YoutubeVideoWithComments를 VideoInfoWithCommentsResponse로 변환
     */
    public VideoInfoWithCommentsResponse convertToVideoInfoWithCommentsResponse(YoutubeVideoWithComments video, String accountNickname) {
        VideoInfoWithCommentsResponse response = new VideoInfoWithCommentsResponse();

        response.setAccountNickname(accountNickname);
        response.setExternalContentId(video.getVideoId());
        response.setPlatformType("YOUTUBE");
        response.setContentType(determineContentType(video.getTitle()));
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setDurationSeconds(video.getDurationSeconds());
        response.setContentUrl("https://www.youtube.com/watch?v=" + video.getVideoId());
        response.setPublishedAt(video.getPublishedAt());
        response.setTags(video.getTags());
        response.setViewsCount(video.getViewsCount());
        response.setLikesCount(video.getLikesCount());
        response.setCommentsCount(video.getCommentsCount());

        // 썸네일 정보 설정
        response.setThumbnailInfo(createThumbnailInfo(video));

        // 댓글 정보 변환
        List<VideoWithCommentsResponse.CommentInfo> commentInfos = new java.util.ArrayList<>();
        if (video.getComments() != null) {
            for (YoutubeVideoWithComments.CommentInfo comment : video.getComments()) {
                commentInfos.add(convertToCommentInfo(comment));
            }
        }
        response.setComments(commentInfos);

        return response;
    }

    /**
     * 댓글 정보를 CommentInfo로 변환하는 헬퍼 메서드
     */
    private VideoWithCommentsResponse.CommentInfo convertToCommentInfo(YoutubeVideoWithComments.CommentInfo comment) {
        VideoWithCommentsResponse.CommentInfo commentInfo = new VideoWithCommentsResponse.CommentInfo();
        commentInfo.setAccountNickname(comment.getAuthorName());
        commentInfo.setExternalCommentId(comment.getCommentId());
        commentInfo.setText(comment.getCommentText());
        commentInfo.setLikesCount(comment.getLikeCount() != null ? comment.getLikeCount().longValue() : 0L);
        commentInfo.setPublishedAt(comment.getPublishedAt());
        commentInfo.setCrawledAt(java.time.LocalDateTime.now().toString());
        return commentInfo;
    }

    /**
     * 썸네일 정보를 ThumbnailInfo로 변환하는 헬퍼 메서드
     */
    private VideoInfoResponse.ThumbnailInfo createThumbnailInfo(YoutubeVideoWithComments video) {
        VideoInfoResponse.ThumbnailInfo thumbnailInfo = new VideoInfoResponse.ThumbnailInfo();

        // YoutubeVideoWithComments에 저장된 thumbnailUrl 사용 (이미 High 또는 Medium 선택됨)
        if (video.getThumbnailUrl() != null) {
            thumbnailInfo.setAccessKey(video.getThumbnailUrl());
        } else {
            // TODO: S3에 Sample image 저장해놓고 없는 경우 대체해야 함.
            thumbnailInfo.setAccessKey("raw-data/youtube/content/video/thumb_yt_" + video.getVideoId() + ".jpg");
        }

        thumbnailInfo.setContentType("image/jpeg");
        return thumbnailInfo;
    }

    /**
     * 문자열을 Long으로 안전하게 변환하는 유틸리티 메서드
     */
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

}
