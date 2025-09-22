package com.ssafy.spark.domain.business.file.dto.request;

import com.ssafy.spark.domain.business.file.entity.File;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCreateRequest {

    private String accessKey;
    private String contentType;

    /**
     * FileCreateRequest로부터 File 엔티티 생성
     */
    public File toEntity() {
        return File.builder()
                .accessKey(this.accessKey)
                .contentType(this.contentType)
                .createdAt(LocalDateTime.now())
                .build();
    }
}