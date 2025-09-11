이 문서는 Leaper 프로젝트의 백엔드 개발 시 따라야 할 규칙과 패턴을 설명합니다.

## **1. 프로젝트 구조**

Leaper는 인플루언서와 광고주를 연결하는 플랫폼으로, 다음과 같은 도메인 구조를 가집니다:

```
src/main/java/com/ssafy/leaper/
├── domain/
│   ├── advertiser/          # 광고주 도메인
│   ├── auth/               # 인증/인가 도메인
│   ├── chat/               # 채팅 도메인
│   ├── content/            # 콘텐츠 도메인
│   ├── influencer/         # 인플루언서 도메인
│   ├── insight/            # 인사이트/분석 도메인
│   ├── platformAccount/    # 플랫폼 계정 도메인
│   └── type/               # 공통 타입 도메인
│      ...
└── global/
    ├── common/             # 공통 응답/엔티티
    ├── config/             # 설정
    ├── error/              # 에러 처리
    ├── jwt/                # JWT 관련
      ...
```

## **2. API 응답 체계**

### **2.1 표준 응답 구조**

모든 API 응답은 `ApiResponse<T>` 래퍼를 사용하여 일관된 형식으로 반환됩니다.

```json
{
  "status": "SUCCESS",
  "data": { }
}
```

에러 응답:
```json
{
  "status": "ERROR",
  "data": {
    "errorCode": "USER-001"
  }
}
```

### **2.2 컨트롤러에서의 사용법**

#### **성공 응답 (데이터 없음)**

```java
@DeleteMapping
public ResponseEntity<ApiResponse<Void>> deleteInfluencer() {
  Long influencerId = ...;
  return handle(influencerService.deleteInfluencer(influencerId));
}
```

#### **성공 응답 (데이터 있음)**

```java
@GetMapping
@Operation(summary = "(인플루언서) 내 정보 조회")
public ResponseEntity<ApiResponse<PageDetail<InfluencerResponseDto>>> getInfluencers(
) {
  Long influencerId = ...;
  return handle(influencerService.getInfluencers(influencerId));
}
```

## **3. 에러 코드 관리**

### **3.1 ErrorCode 구조**

현재 `ErrorCode`는 HttpStatus와 코드만 가지고 있습니다:

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    /* 1. COMMON – 공통 */
    COMMON_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "COMMON-001"),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-002"),
    
    /* 2. AUTH – 인증/인가 */
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-001"),
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-002"),
    
    /* 3. USER – 사용자 */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001"),
    USER_DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-002");
    
        ...
    
    private final HttpStatus httpStatus;
    private final String code;
}
```

## **4. 예외 처리**

### **4.1 예외 처리 흐름**

1. 서비스에서 `Exception` 발생
2. `GlobalExceptionHandler`가 예외를 캐치
3. `ApiResponse`로 일관된 에러 응답 반환

```java
@ExceptionHandler(value = Exception.class)
public ResponseEntity<String> handleException(Exception e) {
  log.error("서버 에러 발생 : ", e);
  return ResponseEntity.status(500).body(e.getMessage());
}
```

### **4.2 예외 처리 예시**
```java
  @Transactional
  public ServiceResult<Void> deleteInfluencer(Long influencerId) {
    log.info("influencerService : deleteInfluencer(" + influencerId + ") 호출");

    Influencer influencer = influencerRepository.findById(influencerId).orElse(null);
    if (influencer == null) {
      return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
    }

    influencerRepository.delete(influencerId);
    
    return ServiceResult.ok();
  }

```

## **5. 도메인 개발 패턴**

### **5.1 새로운 도메인 추가 시 체크리스트**

1. **Entity 생성** (`domain/[도메인명]/entity`)

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class) // 해당 어노테이션이 있어야 @CreatedDate @LastModifiedDate 동작함
public class Influencer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "influencer_id")
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String nickname;
    
    @Column(nullable = false)
    private String email;
    
    @OneToMany(mappedBy = "influencer", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Enumerated(EnumType.STRING)
    private List<PlatformAccount> platformAccountList;
    
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private LocalDate snapshotDate;

    // DTO를 받아서 Entity로 변환하는 정적 팩토리 메서드
    public static Influencer from(InfluencerCreateRequest request) {
      return Influencer.builder()
        .nickname(request.getNickname())
        .email(request.getEmail())
        .build();
  }
}
```

2. **DTO 생성** (`domain/[도메인명]/dto`)
    - **Request**: `dto/request/` 디렉토리
    - **Response**: `dto/response/` 디렉토리
    - 레코드 명에 Dto를 붙이지 않는다.
    - 생성자는 파라미터가 단수인경우 from, 복수인경우 of로 작성한다.

```java
// Request DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InfluencerCreateRequest {
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
    private String nickname;
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
    
    @NotNull(message = "플랫폼은 필수입니다.")
    private List<PlatformAccount> platformAccountList;
}

// Response DTO
@Getter
@Builder
public class InfluencerResponse {
    private Long id;
    private String nickname;
    private String email;
    private Platform platform;
    private LocalDateTime createdAt;
    
    public static InfluencerResponse from(Influencer influencer) {
        return InfluencerResponse.builder()
            .id(influencer.getId())
            .nickname(influencer.getNickname())
            .email(influencer.getEmail())
            .platform(influencer.getPlatform())
            .createdAt(influencer.getCreatedAt())
            .build();
    }
}
```
| 어노테이션 | 사용 타입 | null | 빈 문자열("") | 공백 문자열("   ") | 빈 컬렉션 |
|-----------|----------|------|---------------|-------------------|-----------|
| `@NotNull` | 모든 객체 | ❌ | ✅ | ✅ | ✅ |
| `@NotEmpty` | String, Collection, Map, Array | ❌ | ❌ | ✅ | ❌ |
| `@NotBlank` | String, CharSequence | ❌ | ❌ | ❌ | - |


3. **Repository 생성** (`domain/[도메인명]/repository`)

```java
public interface InfluencerRepository extends JpaRepository<Influencer, Long> {
    Optional<Influencer> findByEmail(String email);
    boolean existsByNickname(String nickname);
    List<Influencer> findByPlatform(Platform platform);
    
    @Query("SELECT i FROM Influencer i WHERE i.nickname LIKE %:keyword%")
    Page<Influencer> findByNicknameContaining(@Param("keyword") String keyword, Pageable pageable);
}
```

4. **Service 인터페이스 및 구현체** (`domain/[도메인명]/service`)

```java
public interface InfluencerService {
  ServiceResult<Map<String, Long>> createInfluencer(InfluencerCreateRequestDto request);
  ServiceResult<InfluencerResponseDto> getInfluencer(Long id);
  ServiceResult<PageDetail<InfluencerResponseDto>> getInfluencers(int page, int size);
  ServiceResult<Map<String, Long>> updateInfluencer(Long id, InfluencerUpdateRequestDto request);
  ServiceResult<Void> deleteInfluencer(Long id);
}

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfluencerServiceImpl implements InfluencerService {
  private final InfluencerRepository influencerRepository;

  @Override
  @Transactional
  public ServiceResult<Map<String, Long>> createInfluencer(InfluencerCreateRequest request) {
    log.info("InfluencerService : createInfluencer() 호출");

    // 중복 닉네임 검증
    if (influencerRepository.existsByNickname(request.nickname())) {
      return ServiceResult.fail(ErrorCode.INFLUENCER_DUPLICATE_NICKNAME);
    }

    Influencer influencer = Influencer.from(request);
    Influencer savedInfluencer = influencerRepository.save(influencer);

    return ServiceResult.ok(Map.of("influencerId", savedInfluencer.getId()));
  }

  @Override
  public ServiceResult<InfluencerResponse> getInfluencer(Long id) {
    log.info("InfluencerService : getInfluencer(" + id + ") 호출");

    Influencer influencer = influencerRepository.findById(id).orElse(null);
    if (influencer == null) {
      return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
    }

    InfluencerResponse response = InfluencerResponseDto.from(influencer);
    return ServiceResult.ok(response);
  }

  @Override
  @Transactional
  public ServiceResult<Map<String, Long>> updateInfluencer(Long id, InfluencerUpdateRequest request) {
    log.info("InfluencerService : updateInfluencer(" + id + ") 호출");

    Influencer influencer = influencerRepository.findById(id).orElse(null);
    if (influencer == null) {
      return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
    }

    // 닉네임 중복 검증 (본인 제외)
    if (request.nickname() != null &&
        !influencer.getNickname().equals(request.nickname()) &&
        influencerRepository.existsByNickname(request.nickname())) {
      return ServiceResult.fail(ErrorCode.INFLUENCER_DUPLICATE_NICKNAME);
    }

    influencer.updateInfluencer(request.nickname(), request.email());
    Influencer savedInfluencer = influencerRepository.save(influencer);

    return ServiceResult.ok(Map.of("influencerId", savedInfluencer.getId()));
  }

  @Override
  @Transactional
  public ServiceResult<Void> deleteInfluencer(Long id) {
    log.info("InfluencerService : deleteInfluencer(" + id + ") 호출");

    Influencer influencer = influencerRepository.findById(id).orElse(null);
    if (influencer == null) {
      return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
    }

    influencerRepository.delete(influencer);

    return ServiceResult.ok();
  }
}
```

5. **Controller 생성** (`domain/[도메인명]/controller`)

```java
@RestController
@RequestMapping("/api/v1/influencer")
@RequiredArgsConstructor
@Tag(name = "Influencer", description = "인플루언서 관리 API")
public class InfluencerController extends BaseController {
  private final InfluencerService influencerService;

  @PostMapping
  @Operation(summary = "인플루언서 생성", description = "새로운 인플루언서를 생성합니다.")
  public ResponseEntity<ApiResponse<InfluencerResponse>> createInfluencer(
      @RequestBody @Valid InfluencerCreateRequest request) {
      return handle(influencerService.createInfluencer(request));

  }

  @GetMapping
  @Operation(summary = "인플루언서 단건 조회")
  public ResponseEntity<ApiResponse<InfluencerResponse>> getInfluencer() {
      Long influencerId = ... ;
      return handle(influencerService.getInfluencer(influencerId));

  }

  @PutMapping
  @Operation(summary = "인플루언서 수정")
  public ResponseEntity<ApiResponse<InfluencerResponse>> updateInfluencer(InfluencerUpdateRequest request) {
    Long influencerId = ... ;
    return handle(influencerService.updateInfluencer(influencerId, request));
  }

  @DeleteMapping
  @Operation(summary = "인플루언서 삭제")
  public ResponseEntity<ApiResponse<Void>> deleteInfluencer() {
    Long influencerId = ... ;
    return handle(influencerService.deleteInfluencer(influencerId));
  }
}
```


## **6. 보안 및 인증**

### **6.1 JWT 토큰 사용**

- Access Token: Authorization 헤더에 Bearer 토큰으로 전달
- Refresh Token: 별도 헤더 또는 쿠키로 관리

### **6.2 인증이 필요한 엔드포인트**

```java
//@GetMapping("/my-profile")
//@Operation(summary = "내 프로필 조회")
//public ResponseEntity<ApiResponse<InfluencerResponse>> getMyProfile(
//        @AuthenticationPrincipal UserDetails userDetails) {
//    Long userId = Long.parseLong(userDetails.getUsername());
//    InfluencerResponseDto profile = influencerService.getInfluencer(userId);
//    return ResponseEntity.ok(new ApiResponse<>(ResponseStatus.SUCCESS, profile));
//}
```

## **7. 개발 시 주의사항**

1. **일관된 응답 구조 유지**: 모든 API는 `ApiResponse`를 사용
2. **적절한 에러 코드 사용**: 도메인별로 구체적인 에러 코드 정의
3. **예외 처리**: 서비스 레이어에서 throw Exception 되도록 사용하지 않음.
4. **DTO 분리**: Entity를 직접 반환하지 않고 DTO 사용
5. **검증**: Request DTO에 Bean Validation 적용
6. **트랜잭션**: 읽기 전용 트랜잭션과 쓰기 트랜잭션 구분
7. **로깅**: 중요한 비즈니스 로직과 예외 상황에 로그 추가
8. **API 문서화**: Swagger 어노테이션을 통한 API 문서 작성


이 가이드를 따라 일관성 있고 유지보수가 쉬운 Leaper 백엔드 코드를 작성할 수 있습니다.