package com.ssafy.spark.global.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.File;
import java.nio.file.Files;

@Slf4j
@Configuration
public class SparkConfig {

  @Value("${cloud.aws.credentials.access-key}")
  private String accessKey;

  @Value("${cloud.aws.credentials.secret-key}")
  private String secretKey;

  @Value("${cloud.aws.region.static}")
  private String region;

  private SparkSession sparkSessionInstance;

  /**
   * sparkSession : spark어플리케이션의 진입점 = Spark 애플리케이션 전체를 관리하는 컨트롤 객체
   */
  @Bean
  public SparkSession sparkSession() {
    // ✨ 임시 디렉토리 사전 생성
    ensureTempDirectories();
    
    // ✨ Windows 환경 감지 및 Hadoop 우회 설정
    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      setupWindowsHadoopWorkaround();
    }
    
    log.info("SparkSession 초기화 시작");
    
    sparkSessionInstance = SparkSession.builder()
        .appName("LeaperApp")
        .master("local[8]")

        // Spark UI 및 서블릿 충돌 방지
        .config("spark.ui.enabled", "true")
        .config("spark.driver.host", "localhost")

        // S3 연결을 위한 설정
        .config("spark.hadoop.fs.s3a.access.key", accessKey)
        .config("spark.hadoop.fs.s3a.secret.key", secretKey)
        .config("spark.hadoop.fs.s3a.endpoint", "s3." + region + ".amazonaws.com")
        .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")

        // 기본 최적화 설정
        .config("spark.sql.adaptive.enabled", "true")
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

        .config("spark.hadoop.fs.s3a.connection.maximum", "100")
        .config("spark.hadoop.fs.s3a.threads.max", "20")
        .config("spark.hadoop.fs.s3a.fast.upload", "true")
        
        // ⭐ 임시 디렉토리 설정 (DiskErrorException 해결)
        .config("spark.local.dir", "/tmp/spark-temp")
        .config("spark.hadoop.hadoop.tmp.dir", "/tmp/hadoop-temp")
        
        // ⭐⭐⭐ S3A를 완전히 메모리 기반으로 전환 (디스크 절대 사용 안함)
        .config("spark.hadoop.fs.s3a.buffer.dir", "/tmp/s3a-buffer")
        .config("spark.hadoop.fs.s3a.fast.upload.buffer", "array")  // array 방식으로 변경
        .config("spark.hadoop.fs.s3a.fast.upload.active.blocks", "8")
        .config("spark.hadoop.fs.s3a.block.size", "33554432")  // 32MB
        .config("spark.hadoop.fs.s3a.committer.staging.tmp.path", "/tmp/s3a-staging")
        
        // Parquet 최적화
        .config("spark.sql.parquet.compression.codec", "snappy")
        .config("spark.sql.parquet.filterPushdown", "true")

        .getOrCreate();
    
    // Hadoop Configuration 런타임 설정 추가 (SparkSession 생성 후)
    sparkSessionInstance.sparkContext().hadoopConfiguration()
        .set("fs.s3a.buffer.dir", "/tmp/s3a-buffer");
    sparkSessionInstance.sparkContext().hadoopConfiguration()
        .set("fs.s3a.fast.upload.buffer", "array");
    
    // 로그 레벨 설정
    try {
      sparkSessionInstance.sparkContext().setLogLevel("WARN");
    } catch (Exception e) {
      log.warn("Spark 로그 레벨 설정 실패 (무시하고 계속): {}", e.getMessage());
    }
    
    log.info("SparkSession 초기화 완료");
    
    return sparkSessionInstance;
  }
  
  /**
   * 애플리케이션 종료 시 SparkSession 정리
   */
  @PreDestroy
  public void cleanup() {
    if (sparkSessionInstance != null) {
      try {
        log.info("SparkSession 종료 시작...");
        sparkSessionInstance.stop();
        log.info("SparkSession 종료 완료");
      } catch (Exception e) {
        log.warn("SparkSession 종료 중 오류 (무시): {}", e.getMessage());
      }
    }
  }
  
  /**
   * Windows 환경에서 Hadoop Home 임시 설정
   */
  private void setupWindowsHadoopWorkaround() {
    try {
      File tempDir = Files.createTempDirectory("hadoop-temp").toFile();
      File binDir = new File(tempDir, "bin");
      binDir.mkdirs();
      
      File winutilsFile = new File(binDir, "winutils.exe");
      winutilsFile.createNewFile();
      
      System.setProperty("hadoop.home.dir", tempDir.getAbsolutePath());
      
      log.info("✅ Windows Hadoop 우회 설정 완료: {}", tempDir.getAbsolutePath());
      
    } catch (Exception e) {
      log.warn("⚠️ Windows Hadoop 우회 설정 실패, 계속 진행: {}", e.getMessage());
    }
  }
  
  /**
   * 임시 디렉토리 사전 생성 및 권한 설정
   */
  private void ensureTempDirectories() {
    try {
      String[] dirs = {"/tmp/spark-temp", "/tmp/hadoop-temp", "/tmp/s3a-buffer", "/tmp/s3a-staging"};
      for (String dirPath : dirs) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
          boolean created = dir.mkdirs();
          if (created) {
            log.info("✅ 임시 디렉토리 생성: {}", dirPath);
          }
        } else {
          log.info("✅ 임시 디렉토리 이미 존재: {}", dirPath);
        }
      }
    } catch (Exception e) {
      log.warn("⚠️ 임시 디렉토리 생성 실패 (계속 진행): {}", e.getMessage());
    }
  }
}
