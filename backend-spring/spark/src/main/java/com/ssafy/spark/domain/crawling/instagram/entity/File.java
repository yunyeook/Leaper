//package com.ssafy.spark.domain.crawling.instagram.entity;
//
//import java.time.LocalDateTime;
//import javax.persistence.Column;
//import javax.persistence.Entity;
//import javax.persistence.GeneratedValue;
//import javax.persistence.GenerationType;
//import javax.persistence.Id;
//import javax.persistence.Table;
//import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedDate;
//
//@Entity
//@Getter
//@Builder
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@AllArgsConstructor(access = AccessLevel.PRIVATE)
//@Table(name = "file")
//public class File {
//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  @Column(name = "file_id")
//  private Integer id;
//
//  @Column(name = "access_key", nullable = false, length = 500)
//  private String accessKey;  // S3 키 경로
//
//  @Column(name = "content_type", nullable = false, length = 100)
//  private String contentType;
//
//  @Column(name = "file_size")
//  private Long fileSize;
//
//  @Column(name = "original_name", length = 255)
//  private String originalName;
//
//  @CreatedDate
//  private LocalDateTime createdAt;
//
//  @LastModifiedDate
//  private LocalDateTime updatedAt;
//}