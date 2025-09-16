package com.ssafy.spark.global.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {

  @Value("${cloud.aws.credentials.access-key}")
  private String accessKey;

  @Value("${cloud.aws.credentials.secret-key}")
  private String secretKey;

  @Value("${cloud.aws.region.static}")
  private String region;

  /**
   * sparkSession : spark어플리케이션의 진입점 = Spark 애플리케이션 전체를 관리하는 컨트롤 객체
   */
  @Bean
  public SparkSession sparkSession() {
    return SparkSession.builder()
        .appName("LeaperApp")              // Spark 앱 이름 (아무거나 상관없음)
        .master("local[*]")                // 로컬에서 실행, [*]는 CPU 코어 개수만큼 사용

        // Spark UI 및 서블릿 충돌 방지
        .config("spark.ui.enabled", "false")                    // Spark UI 비활성화
        .config("spark.driver.host", "localhost")               // 호스트 명시적 지정

        // S3 연결을 위한 설정
        .config("spark.hadoop.fs.s3a.access.key", accessKey)      // AWS Access Key
        .config("spark.hadoop.fs.s3a.secret.key", secretKey)      // AWS Secret Key
        .config("spark.hadoop.fs.s3a.endpoint", "s3." + region + ".amazonaws.com")  // S3 서버 주소

        // 기본 최적화 설정
        .config("spark.sql.adaptive.enabled", "true")           // 적응형 쿼리 실행
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")  // 빠른 직렬화

        .getOrCreate();  // 세션 생성 (이미 있으면 기존 것 사용)
  }
}