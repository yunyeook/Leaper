package com.ssafy.spark.global.converter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;

/**
 * List<String> <-> JSON 문자열 변환기
 *
 * JPA @Convert 어노테이션과 함께 사용해서
 * 엔티티의 List<String> 필드를 DB의 JSON 컬럼에 매핑할 수 있게 해준다.
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

  // Jackson ObjectMapper: 자바 객체 <-> JSON 문자열 변환에 사용
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 엔티티의 List<String> 데이터를 DB에 저장할 때 호출됨.
   * 자바의 List<String> -> JSON 문자열로 변환해서 DB에 저장.
   */
  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    try {
      // List<String>을 JSON 문자열로 직렬화
      return objectMapper.writeValueAsString(attribute);
    } catch (Exception e) {
      // 변환 실패 시 빈 배열 JSON으로 저장
      return "[]";
    }
  }

  /**
   * DB의 JSON 문자열 데이터를 엔티티 조회 시 호출됨.
   * DB JSON 문자열 -> 자바 List<String>으로 변환해서 엔티티에 넣어준다.
   */
  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    try {
      // JSON 문자열을 List<String>으로 역직렬화
      return objectMapper.readValue(dbData, new TypeReference<>() {});
    } catch (Exception e) {
      // 변환 실패 시 빈 리스트 반환
      return List.of();
    }
  }
}
