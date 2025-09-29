package com.ssafy.spark.domain.crawling.connect.service;

import com.ssafy.spark.domain.crawling.connect.request.CrawlingRequest;
import org.springframework.scheduling.annotation.Async;

public interface CrawlingService {
  @Async
  void startCrawlingAsync(CrawlingRequest request);

}
