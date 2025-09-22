package com.ssafy.leaper.domain.file.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer id;

    private String accessKey; // 파일 저장 키

    private String contentType; // 파일 타입

    /**  파일 이름을 S3 내 저장 파일 이름으로 변환하는 메소드
     *
     */
     public static String generateUniqueKey(String fileName, String fileType) {
         return "file/" + fileType + "/" + UUID.randomUUID() + fileName;
     }
}