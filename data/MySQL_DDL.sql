-- 데이터베이스 생성 및 선택
-- =========================================================
 drop database leaper;
CREATE DATABASE IF NOT EXISTS leaper
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE leaper;

CREATE TABLE file (
    file_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '파일 고유 ID',
    access_key VARCHAR(500) NOT NULL COMMENT 'S3 저장 키 (경로 포함)',
    content_type VARCHAR(30) NOT NULL COMMENT '파일 MIME 타입',
	  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시');

-- =========================================================
-- 1. 플랫폼 종류 (Platform_type)
-- =========================================================
CREATE TABLE platform_type (
	platform_type_id     	VARCHAR(31) PRIMARY KEY COMMENT 'PK: 플랫폼 종류 ID',
	type_name	            VARCHAR(31)	NOT NULL
);


-- =========================================================
-- 2. 컨텐츠 종류(Content_type)
-- =========================================================
CREATE TABLE content_type (
	content_type_id   	VARCHAR(31) PRIMARY KEY COMMENT 'PK: 컨텐츠 종류 ID',
	type_name         	VARCHAR(31)	NOT NULL 
);


-- =========================================================
-- 3. 소셜 제공자 종류(Provider_type)
-- =========================================================
CREATE TABLE provider_type (
	provider_type_id   	VARCHAR(31) PRIMARY KEY COMMENT 'PK: 소셜제공자 종류 ID',
	type_name         	VARCHAR(31)	NOT NULL
);


-- =========================================================
-- 4. 광고주(Advertiser)
-- =========================================================
CREATE TABLE advertiser (
  advertiser_id                INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT 'PK: 회원 ID(광고주)',
  login_id                     VARCHAR(21) NOT NULL COMMENT '광고주 아이디 (광고주 폼 로그인 전용)',
  password                     VARCHAR(255) NOT NULL COMMENT '패스워드, 해시로 저장 (광고주 폼 로그인 전용)',
  brand_name                   VARCHAR(61)  NOT NULL COMMENT '브랜드명(중복 가능)',
  company_name                 VARCHAR(91) NOT NULL COMMENT '회사명',
  company_profile_image_id     INT COMMENT '회사 이미지 S3 id',
  representative_name          VARCHAR(100) NOT NULL COMMENT '대표자 성명',
  business_reg_no              CHAR(10)  NOT NULL COMMENT '사업자 등록번호(하이픈, 띄어쓰기 없음)',
  opening_date                 DATE NULL COMMENT '개업일자',
  created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  updated_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
  is_deleted                   TINYINT(1) NOT NULL COMMENT '삭제(탈퇴) 여부',
  deleted_at                   DATETIME NOT NULL COMMENT '삭제(탈퇴) 일시',
  FOREIGN KEY (company_profile_image_id) REFERENCES file(file_id) ON DELETE CASCADE,
  UNIQUE KEY uq_platform_name (business_reg_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='광고주 상세 프로필 및 사업자 정보 저장';


-- =========================================================
-- 5. 인플루언서 (Influencer)
-- =========================================================
CREATE TABLE influencer (
  influencer_id                   INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT 'PK: 회원 ID(인플루언서)',
  provider_type_id                VARCHAR(31) NOT NULL COMMENT 'FK: 소셜 제공자 ID',
  provider_member_id              VARCHAR(31) NOT NULL COMMENT '소셜 제공자별 고유 식별자',
  nickname                        VARCHAR(61)  NOT NULL COMMENT '닉네임(중복 불가)',
  gender                          TINYINT(1) NOT NULL COMMENT '성별, 남 = 0, 여 = 1',
  birthday                        DATE NOT NULL COMMENT '생년월일',
  email                           VARCHAR(320) NOT NULL COMMENT '레포트 수신용 이메일',
  influencer_profile_image_id     INT  COMMENT ' 프로필 이미지 S3 id',
  bio                             VARCHAR(401) COMMENT '자기소개',
  created_at                      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  updated_at                      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
  is_deleted                      TINYINT(1) NOT NULL COMMENT '삭제(탈퇴) 여부',
  deleted_at                      DATETIME NULL COMMENT '삭제(탈퇴) 일시' ,
  
  FOREIGN KEY (provider_type_id) REFERENCES provider_type(provider_type_id) ON DELETE CASCADE,
  FOREIGN KEY (influencer_profile_image_id) REFERENCES file(file_id) ON DELETE CASCADE,
  UNIQUE KEY uq_provider (provider_type_id, provider_member_id),
  UNIQUE KEY uq_nickname (nickname) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='인플루언서 프로필';


-- =========================================================
-- 6. 인플루언서 광고형태(Influencer_Ad_Format)
-- =========================================================
CREATE TABLE influencer_ad_format (
  influencer_id               INT UNSIGNED NOT NULL COMMENT 'FK: 인플루언서 회원 ID',
  content_type_id             VARCHAR(31) NOT NULL COMMENT 'FK: 컨텐츠 종류 ID', 
  is_accepted                 TINYINT(1) NOT NULL DEFAULT 0 COMMENT '수용 여부(1=수용,0=거부)',
  
  PRIMARY KEY (influencer_id, content_type_id),
  FOREIGN KEY (influencer_id) REFERENCES influencer(influencer_id) ON DELETE CASCADE,
  FOREIGN KEY (content_type_id) REFERENCES content_type(content_type_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='인플루언서가 수용하는 광고 형태';


-- =========================================================
-- 7 카테고리 종류 (Category_Type)
-- =========================================================
CREATE TABLE category_type (
  category_type_id                SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 카테고리 ID',
  category_name                   VARCHAR(200) NOT NULL COMMENT '카테고리명',
  created_at                      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  
  UNIQUE KEY uq_platform_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='카테고리 종류';


-- =========================================================
-- 8. 플랫폼 계정(Platform_Account)
-- =========================================================
CREATE TABLE platform_account (
  platform_account_id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 플랫폼 계정 ID',
  influencer_id                INT UNSIGNED NOT NULL COMMENT 'FK: 보유 회원 ID',
  platform_type_id             VARCHAR(31) NOT NULL COMMENT 'FK : 플랫폼 종류 ID',
  external_account_id          VARCHAR(320) NOT NULL COMMENT '외부 플랫폼 계정 ID(채널/블로그/IG)',
  account_nickname             VARCHAR(301) NOT NULL COMMENT '계정 닉네임',
  account_url                  VARCHAR(500) NOT NULL COMMENT '계정 URL',
  account_profile_image_id     INT COMMENT '계정 프로필 이미지 S3 id',
  category_type_id             SMALLINT UNSIGNED NULL COMMENT 'FK : 카테고리 ID',
  is_deleted                   TINYINT(1) NOT NULL COMMENT '삭제(탈퇴) 여부',
  deleted_at                   DATETIME NOT NULL COMMENT '삭제(탈퇴) 일시',
  created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  updated_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
  
  
  FOREIGN KEY (influencer_id)  REFERENCES influencer(influencer_id) ON DELETE CASCADE,
  FOREIGN KEY (category_type_id) REFERENCES category_type(category_type_id) ON DELETE CASCADE,
  FOREIGN KEY (account_profile_image_id) REFERENCES file(file_id) ON DELETE CASCADE,
  UNIQUE KEY uq_platform_external (platform_type_id, external_account_id),
  KEY idx_influencer_platform (influencer_id, platform_type_id),
  KEY idx_nickname_search (account_nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='유튜브/네이버블로그/인스타 등 외부 플랫폼 계정';


-- =========================================================
-- 9. 일별 계정별 인사이트(Daily_Account_Insight)
-- =========================================================
CREATE TABLE daily_account_insight (
  daily_account_insight_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 일별 계정별 인사이트 ID',
  platform_account_id          INT UNSIGNED NOT NULL COMMENT 'FK: 플랫폼 계정 ID',
  total_views                  BIGINT UNSIGNED NOT NULL COMMENT '누적 조회수(플랫폼 기준)',
  total_followers              INT UNSIGNED NOT NULL COMMENT '누적 팔로워/이웃/구독(통합 지표)',
  total_contents               INT UNSIGNED NOT NULL COMMENT '누적 컨텐츠 수',
  total_likes                  BIGINT UNSIGNED NOT NULL COMMENT '누적 좋아요 수',
  total_comments               BIGINT UNSIGNED NOT NULL COMMENT '누적 댓글 수',
  like_score                   DECIMAL(5,2) NULL COMMENT '호감도 점수 (-100.00~100.00)',
  snapshot_date                DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  
  FOREIGN KEY (platform_account_id) REFERENCES platform_account(platform_account_id) ON DELETE CASCADE,
  UNIQUE KEY uq_account_day (platform_account_id, snapshot_date),
  KEY idx_account_day (platform_account_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='플랫폼 계정 단위 일별 누적 인사이트 스냅샷';


-- =========================================================
-- 10. 콘텐츠(Content) - 계속 최신화
-- =========================================================
CREATE TABLE content (
  content_id                    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 콘텐츠 ID',
  platform_account_id           INT UNSIGNED NOT NULL COMMENT 'FK: 계정 ID(게시 주체)',
  platform_type_id              VARCHAR(31) NOT NULL COMMENT 'FK: 플랫폼 ID',
  content_type_id               VARCHAR(31) NOT NULL COMMENT 'FK: 컨텐츠 타입 ID',
  title                         VARCHAR(500) NOT NULL COMMENT '제목',
  description                   TEXT NOT NULL COMMENT '컨텐츠 내용',
  duration_seconds              INT UNSIGNED NULL COMMENT '영상 길이(초) - 영상류만',
  thumbnail_id                  INT NULL COMMENT '썸네일 S3 id',
  content_url                   VARCHAR(500) NOT NULL COMMENT '콘텐츠 URL',
  published_at                  DATETIME NULL COMMENT '게시(발행) 일시',
  tags_json                     JSON NULL COMMENT '태그/해시태그 원본(JSON)',
  total_views                   BIGINT UNSIGNED NOT NULL COMMENT '누적 조회수(플랫폼 기준)',
  total_likes                   BIGINT UNSIGNED NOT NULL COMMENT '누적 좋아요 수',
  total_comments                BIGINT UNSIGNED NOT NULL COMMENT '누적 댓글 수',
  snapshot_date                DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  
  updated_at                    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
  FOREIGN KEY (platform_account_id)  REFERENCES platform_account(platform_account_id) ON DELETE CASCADE,
  FOREIGN KEY (content_type_id)  REFERENCES content_type(content_type_id) ON DELETE CASCADE,
  FOREIGN KEY (thumbnail_id) REFERENCES file(file_id) ON DELETE CASCADE,
  FOREIGN KEY (platform_type_id) REFERENCES platform_type(platform_type_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='플랫폼별 게시물/영상/쇼츠 메타 정보';


-- =========================================================
-- 11. 일별 유형별 인사이트(Daily_Type_Insight)
-- =========================================================
CREATE TABLE daily_type_insight (
  daily_type_insight_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 일별 인사이트 ID',
  content_type_id               VARCHAR(31) NOT NULL COMMENT 'FK: 컨텐츠 타입 ID',
  platform_account_id           INT UNSIGNED NOT NULL COMMENT 'FK: 계정 ID(게시 주체)',
  today_views                   INT NOT NULL COMMENT '오늘치 조회수',
  today_contents                INT NOT NULL COMMENT '오늘치 컨텐츠 수',
  today_likes                   INT NOT NULL COMMENT '오늘치 좋아요 수',
  month_views                   BIGINT NOT NULL COMMENT '한 달치 조회수(전 달 말일과 비교, 월 별 데이터를 넘길 때는 해당 월의 말일 기준)',
  month_contents                INT NOT NULL COMMENT '한 달치 컨텐츠 수',
  month_likes                   BIGINT NOT NULL COMMENT '한 달치 좋아요 수',
  total_views                   BIGINT UNSIGNED NOT NULL COMMENT '누적 조회수',
  total_contents                INT UNSIGNED NOT NULL COMMENT '누적 컨텐츠 수',
  total_likes                   BIGINT UNSIGNED NOT NULL COMMENT '누적 좋아요 수',
 	snapshot_date                DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  
  FOREIGN KEY (platform_account_id)  REFERENCES platform_account(platform_account_id) ON DELETE CASCADE,
  FOREIGN KEY (content_type_id)  REFERENCES content_type(content_type_id) ON DELETE CASCADE,
  KEY idx_content_day (platform_account_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='콘텐츠 유형 단위(플랫폼 -> 긴 영상, 쇼츠, 게시글) 일별 인사이트 스냅샷';


-- =========================================================
-- 12. 컨텐츠 댓글 인사이트 (Content_Comment_Insight) => MongoDB? (해결?)
-- =========================================================
CREATE TABLE content_comment_insight (
  content_comment_insight_id       INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 댓글 ID',
  content_id                          INT UNSIGNED NOT NULL COMMENT 'FK: 콘텐츠 ID',
  positive_comment_json               JSON NULL COMMENT '긍정 댓글 top 3',
  negative_comment_json               JSON NULL COMMENT '부정 댓글 top 3',
  like_score                          DECIMAL(5,2) NULL COMMENT '호감도 점수 (-100.00~100.00)',
  summary_text                        TEXT NULL COMMENT '요약 텍스트',
  keywords_json                       JSON NULL COMMENT '추출 키워드 목록(JSON)',
  model_version                       VARCHAR(64) NULL COMMENT '분석 모델 버전',
  snapshot_date                       DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  
  FOREIGN KEY (content_id) REFERENCES content(content_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='콘텐츠 댓글 스냅샷(긍정 댓글 Top 3, 부정 댓글 Top 3)';


-- =========================================================
-- 13. 키워드 트렌드 일별(Daily_Keyword_Trend) -> 플랫폼별로 일별 트렌드 키워드를 가져옴, 하둡에서 카운팅하여 가져옴 (해결)
-- =========================================================
CREATE TABLE daily_keyword_trend (
  daily_keyword_trend_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 트렌드 ID',
  platform_type_id            VARCHAR(31) NOT NULL COMMENT 'FK: 플랫폼 ID',
  keywords_json               JSON NULL COMMENT '키워드 목록(JSON)',
  snapshot_date                DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  
  KEY idx_platform_day (platform_type_id, created_at),
  FOREIGN KEY (platform_type_id) REFERENCES platform_type(platform_type_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='플랫폼별 키워드 일별 트렌드';

 
-- =========================================================
-- 14. 일별 급상승 인플루언서(Daily_Trending_Influencer) -> Top10만 저장, 우리 서비스 내 인플루언서만 
-- =========================================================
CREATE TABLE daily_trending_influencer (
  daily_trending_influencer_id   		INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 일별 급상승 인플루언서 ID',
  platform_type_id   		   		 	VARCHAR(31) NOT NULL COMMENT 'FK: 플랫폼 ID',
  influencer_id    		    			INT UNSIGNED NOT NULL COMMENT 'FK: 인플루언서 회원 ID',
  category_type_id     		  		    SMALLINT UNSIGNED COMMENT 'FK: 카테고리 ID',
  influencer_rank  		  		  		TINYINT UNSIGNED NOT NULL COMMENT '순위: 1~ 10',
  snapshot_date                     	DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                        	DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
 
  FOREIGN KEY (influencer_id) REFERENCES influencer(influencer_id) ON DELETE CASCADE,
  FOREIGN KEY (category_type_id) REFERENCES category_type(category_type_id) ON DELETE CASCADE,
  FOREIGN KEY (platform_type_id) REFERENCES platform_type(platform_type_id) ON DELETE CASCADE,
  UNIQUE KEY uq_trend_influencer (platform_type_id, influencer_rank, category_type_id, snapshot_date)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='플랫폼별 일별 급상승 크리에이터';


-- =========================================================
-- 15 . 일별 인기 인플루언서(Daily_Popular_Influncer) -> Top10만 저장, 우리 서비스 내 인플루언서만 포함
-- =========================================================
CREATE TABLE daily_popular_influencer (
  daily_popular_influencer_id			INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 인기 ID',
  platform_type_id 						VARCHAR(31) NOT NULL COMMENT 'FK: 플랫폼 ID',
  influencer_id    						INT UNSIGNED NOT NULL COMMENT 'FK: 인플루언서 회원 ID',
  category_type_id   					SMALLINT UNSIGNED COMMENT 'FK: 카테고리 ID',
  influencer_rank  						TINYINT UNSIGNED NOT NULL COMMENT '순위: 1~ 10',
  snapshot_date                   		DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                      		DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

  FOREIGN KEY (influencer_id) REFERENCES influencer(influencer_id) ON DELETE CASCADE,
  FOREIGN KEY (platform_type_id) REFERENCES platform_type(platform_type_id) ON DELETE CASCADE,
  FOREIGN KEY (category_type_id) REFERENCES category_type(category_type_id) ON DELETE CASCADE,
  UNIQUE KEY uq_trend_influencer (platform_type_id, influencer_rank, category_type_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='일별 인기 인플루언서';


-- =========================================================
-- 16. 일별 급상승 컨텐츠 (Daily_Trending_Content) -> Top10만 저장, 우리 서비스 내 인플루언서만 포함일지
-- =========================================================
CREATE TABLE daily_trending_content (
  daily_trending_content_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 트렌드 ID',
  platform_type_id  			VARCHAR(31) NOT NULL COMMENT 'FK: 플랫폼 ID',
  content_id        			INT UNSIGNED NOT NULL COMMENT 'FK: 콘텐츠 ID',
  category_type_id       		SMALLINT UNSIGNED COMMENT 'FK: 카테고리 ID',
  content_rank      			TINYINT UNSIGNED NOT NULL COMMENT '순위: 1~ 10',
  snapshot_date               	DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

  FOREIGN KEY (platform_type_id) REFERENCES platform_type(platform_type_id) ON DELETE CASCADE,
  FOREIGN KEY (content_id) REFERENCES content(content_id) ON DELETE CASCADE,
  FOREIGN KEY (category_type_id) REFERENCES category_type(category_type_id) ON DELETE CASCADE,
  UNIQUE KEY uq_trend_influencer (platform_type_id, content_rank, category_type_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='일별 급상승 컨텐츠';


-- =========================================================
-- 17. 일별 인기 콘텐츠(Daily_Popular_Content) -> Top10만 저장, 우리 서비스 내 인플루언서만 포함
-- =========================================================
CREATE TABLE daily_popular_content (
  daily_popular_content_id	    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 인기 ID',
  platform_type_id  			VARCHAR(31) NOT NULL COMMENT 'FK: 플랫폼 ID',
  content_id        			INT UNSIGNED NOT NULL COMMENT 'FK: 콘텐츠 ID',
  category_type_id       		SMALLINT UNSIGNED COMMENT 'FK: 카테고리 ID',
  content_rank      			TINYINT UNSIGNED NOT NULL COMMENT '순위: 1~ 10',
  snapshot_date               	DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
  
  FOREIGN KEY (platform_type_id) REFERENCES platform_type(platform_type_id) ON DELETE CASCADE,
  FOREIGN KEY (content_id) REFERENCES content(content_id) ON DELETE CASCADE,
  FOREIGN KEY (category_type_id) REFERENCES category_type(category_type_id) ON DELETE CASCADE,
  UNIQUE KEY uq_trend_influencer (platform_type_id, content_rank, category_type_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='일별 인기 콘텐츠';


-- =========================================================
-- 18. 일별 나의 인기 콘텐츠(Daily_My_Popular_Content) -> 연결된 계정별 Top10만 저장
-- =========================================================
CREATE TABLE daily_my_popular_content (
  daily_my_popular_content_id	      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 인기 ID',
  platform_account_id        		  INT UNSIGNED NOT NULL COMMENT 'FK: 플랫폼 계정 ID',
  content_id        		      	  INT UNSIGNED NOT NULL COMMENT 'FK: 콘텐츠 ID',
  content_rank      			      TINYINT UNSIGNED NOT NULL COMMENT '순위: 1~ 10',
  snapshot_date                       DATE NOT NULL COMMENT '생성 날짜 (조회시 사용)',
  created_at                          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

  FOREIGN KEY (platform_account_id) REFERENCES platform_account(platform_account_id) ON DELETE CASCADE,
  FOREIGN KEY (content_id) REFERENCES content(content_id) ON DELETE CASCADE,
  UNIQUE KEY uq_trend_influencer (platform_account_id, content_rank, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='일별 나의 인기 콘텐츠';


-- =========================================================
-- 19. 채팅방(chat_room)
-- =========================================================
CREATE TABLE chat_room (
  chat_room_id     				 TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'PK: 채팅방 ID',
  influencer_id                  INT UNSIGNED NOT NULL COMMENT 'FK: 보유 회원 ID',
  advertiser_id	     			 INT UNSIGNED NOT NULL	COMMENT 'FK : 보유 광고주 ID',
  influencer_last_seen	     	 DATETIME	NULL COMMENT '인플루언서가 마지막으로 본 시간',
  advertiser_last_seen	         DATETIME	NULL COMMENT '광고주가 마지막으로 본 시간',
  influencer_deleted             TINYINT(1)  COMMENT '인플루언서 삭제(탈퇴) 여부',
  influencer_deleted_at          DATETIME NULL COMMENT '인플루언서 삭제(탈퇴) 일시',
  advertiser_deleted             TINYINT(1)  COMMENT '광고주 삭제(탈퇴) 여부',
  advertiser_deleted_at          DATETIME NULL COMMENT '광고주 삭제(탈퇴) 일시', 
  created_at        			 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시'
);

