package com.ssafy.spark.domain.spark.service;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SparkAnalysisService {

  // Config에서 만든 SparkSession을 주입받음
  @Autowired
  private SparkSession spark;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  /**
   * Spark가 잘 작동하는지 테스트
   */
  public String testSpark() {
    Dataset<Row> testData = spark.sql("SELECT 'Hello Spark!' as message");
    testData.show();

    return "Spark 테스트 성공!";
  }

}
