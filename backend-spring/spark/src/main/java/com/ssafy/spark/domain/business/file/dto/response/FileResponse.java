package com.ssafy.spark.domain.business.file.dto.response;

import com.ssafy.spark.domain.business.file.entity.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private Integer id;
    private String accessKey;
    private String contentType;
    private LocalDateTime createdAt;

    /**
     * File 엔티티로부터 FileResponse 생성
     */
    public static FileResponse from(File file) {
        return FileResponse.builder()
                .id(file.getId())
                .accessKey(file.getAccessKey())
                .contentType(file.getContentType())
                .createdAt(file.getCreatedAt())
                .build();
    }
}