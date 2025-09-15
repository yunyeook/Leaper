package com.ssafy.leaper.domain.advertiser.service;

import com.ssafy.leaper.domain.advertiser.dto.request.BusinessValidationRequest;
import com.ssafy.leaper.domain.advertiser.dto.response.BusinessValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessValidationService {

    @Value("${nts.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate;

    private static final String NTS_API_URL = "http://api.odcloud.kr/api/nts-businessman/v1/validate";

    public boolean validateBusinessRegistration(String businessRegNo, String representativeName, LocalDate openingDate) {
        log.info("Starting business registration validation - businessRegNo: {}, representativeName: {}",
                businessRegNo, representativeName);

        try {
            // 요청 데이터 생성
            BusinessValidationRequest.Business business = BusinessValidationRequest.Business.builder()
                    .businessNumber(businessRegNo)
                    .startDate(openingDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                    .representativeName(representativeName)
                    .representativeName2("") // 빈 값으로 설정
                    .businessName("") // 빈 값으로 설정 (우리는 모르는 정보)
                    .corporationNumber("") // 빈 값으로 설정
                    .businessSector("") // 빈 값으로 설정
                    .businessType("") // 빈 값으로 설정
                    .businessAddress("") // 빈 값으로 설정
                    .build();

            BusinessValidationRequest request = BusinessValidationRequest.builder()
                    .businesses(Collections.singletonList(business))
                    .build();

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.ALL));

            HttpEntity<BusinessValidationRequest> entity = new HttpEntity<>(request, headers);

            // API URL 생성 (서비스키 포함)
            String url = UriComponentsBuilder.fromHttpUrl(NTS_API_URL)
                    .queryParam("serviceKey", serviceKey)
                    .build()
                    .toUriString();

            // API 호출
            ResponseEntity<BusinessValidationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    BusinessValidationResponse.class
            );

            // HTTP 상태 코드 확인 (API 호출 성공 여부)
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Business validation API call failed - businessRegNo: {}, httpStatus: {}",
                        businessRegNo, response.getStatusCode());
                return false;
            }

            // 응답 본문 확인
            BusinessValidationResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getData() == null || responseBody.getData().isEmpty()) {
                log.warn("Business validation failed - empty response data: businessRegNo: {}", businessRegNo);
                return false;
            }

            // valid 필드로 실제 유효성 판단
            BusinessValidationResponse.BusinessValidationResult result = responseBody.getData().get(0);
            boolean isValid = "01".equals(result.getValid()); // "01"=유효, "02"=무효

            log.info("Business validation completed - businessRegNo: {}, valid: {}, validMessage: {}",
                    businessRegNo, result.getValid(), result.getValidMessage());

            return isValid;

        } catch (Exception e) {
            log.error("Failed to validate business registration - businessRegNo: {}, representativeName: {}",
                    businessRegNo, representativeName, e);
            return false; // 검증 실패 시 false 반환
        }
    }
}