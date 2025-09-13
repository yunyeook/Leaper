package com.ssafy.leaper.domain.file.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class File {

  @Id
  @Column(name = "file_id")
  private Integer fileId;

  @Column(name = "name", length = 100)
  private String name;

  @Column(name = "accessUrl", length = 1025)
  private String accessUrl;
}