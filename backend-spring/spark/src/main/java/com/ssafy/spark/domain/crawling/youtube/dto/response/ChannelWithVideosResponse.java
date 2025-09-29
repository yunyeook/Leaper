package com.ssafy.spark.domain.crawling.youtube.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelWithVideosResponse {
    private ChannelInfoResponse channelInfo;
    private List<VideoInfoWithCommentsResponse> videos;

    public ChannelWithVideosResponse(ChannelInfoResponse channelInfo, List<VideoInfoWithCommentsResponse> videos) {
        this.channelInfo = channelInfo;
        this.videos = videos;
    }
}