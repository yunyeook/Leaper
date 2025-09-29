package com.ssafy.spark.domain.insight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 테스트용 더미 데이터 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DummyDataGeneratorService extends SparkBaseService {

    private final Random random = new Random();

    // 카테고리 목록
    private static final String[] CATEGORIES = {
            "뷰티", "게임", "요리", "여행", "음악",
            "스포츠", "교육", "엔터테인먼트", "기술", "일상"
    };

    // 더미 이름 목록
    private static final String[] FIRST_NAMES = {
            "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"
    };

    private static final String[] LAST_NAMES = {
            "민수", "서연", "지훈", "수빈", "현우", "채원", "도윤", "하은", "시우", "예은"
    };

    /**
     * 전체 데이터셋 생성 (콘텐츠 + 계정 + Parquet 변환)
     */
    public void generateFullDataset(String platformType, LocalDate targetDate,
            int contentCount, int accountCount) {
        log.info("=== 전체 더미 데이터셋 생성 시작 ===");
        log.info("Platform: {}, Date: {}, Content: {}, Account: {}",
                platformType, targetDate, contentCount, accountCount);

        try {
            // 1. DB 기본 데이터 설정
            setupDatabase(platformType);

            // 2. 계정 데이터 생성 및 S3/DB 저장
            List<AccountData> accounts = generateAccountData(accountCount);
            saveAccountDataToS3(platformType, targetDate, accounts);
            saveAccountDataToDB(platformType, accounts);

            // 3. 콘텐츠 데이터 생성 및 S3/DB 저장
            List<ContentData> contents = generateContentData(contentCount, accounts);
            saveContentDataToS3(platformType, targetDate, contents);
            saveContentDataToDB(platformType, contents);

            // 4. JSON → Parquet 변환
            log.info(">>> JSON → Parquet 변환 시작");
            convertContentJsonToParquet(platformType, targetDate);
            convertPlatformAccountJsonToParquet(platformType, targetDate);

            log.info("=== 전체 더미 데이터셋 생성 완료 ===");

        } catch (Exception e) {
            log.error("더미 데이터 생성 실패", e);
            throw new RuntimeException("더미 데이터 생성 실패", e);
        }
    }

    /**
     * DB 기본 데이터 설정 (platform_type, category_type 등)
     */
    public void setupDatabase(String platformType) {
        log.info(">>> DB 기본 데이터 설정 시작");

        try {
            // Platform Type 데이터가 없으면 추가
            String checkSql = "SELECT COUNT(*) FROM platform_type WHERE platform_type_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, platformType.toUpperCase());

            if (count == null || count == 0) {
                String insertPlatformSql = "INSERT INTO platform_type (platform_type_id, platform_name) VALUES (?, ?) "
                        +
                        "ON DUPLICATE KEY UPDATE platform_name = VALUES(platform_name)";
                jdbcTemplate.update(insertPlatformSql, platformType.toUpperCase(), platformType);
                log.info("Platform Type 추가: {}", platformType);
            }

            // Category Type 추가
            String insertCategorySql = "INSERT INTO category_type (category_name) VALUES (?) " +
                    "ON DUPLICATE KEY UPDATE category_name = VALUES(category_name)";

            for (String category : CATEGORIES) {
                jdbcTemplate.update(insertCategorySql, category);
            }

            log.info("Category Type {} 개 추가 완료", CATEGORIES.length);
            log.info(">>> DB 기본 데이터 설정 완료");

        } catch (Exception e) {
            log.error("DB 기본 데이터 설정 실패", e);
            throw new RuntimeException("DB 설정 실패", e);
        }
    }

    /**
     * 계정 더미 데이터 생성
     */
    public List<AccountData> generateAccountData(int count) {
        log.info(">>> 계정 더미 데이터 {} 개 생성 시작", count);

        List<AccountData> accounts = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String nickname = generateRandomName() + "_" + i;
            String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
            int followers = 1000 + random.nextInt(999000); // 1K ~ 1M

            accounts.add(new AccountData(
                    nickname,
                    category,
                    followers,
                    "https://example.com/profile/" + i + ".jpg",
                    "안녕하세요! " + nickname + "입니다."));
        }

        log.info(">>> 계정 더미 데이터 {} 개 생성 완료", accounts.size());
        return accounts;
    }

    /**
     * 콘텐츠 더미 데이터 생성
     */
    public List<ContentData> generateContentData(int count, List<AccountData> accounts) {
        log.info(">>> 콘텐츠 더미 데이터 {} 개 생성 시작", count);

        List<ContentData> contents = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // 랜덤 계정 선택
            AccountData account = accounts.get(random.nextInt(accounts.size()));

            String contentId = "content_" + System.currentTimeMillis() + "_" + i;
            String title = account.categoryName + " 관련 콘텐츠 #" + i;
            int views = random.nextInt(1000000); // 0 ~ 1M
            int likes = Math.min(views, random.nextInt(views + 1)); // 조회수보다 작게
            int comments = Math.min(likes, random.nextInt(likes + 1)); // 좋아요보다 작게

            contents.add(new ContentData(
                    contentId,
                    account.accountNickname,
                    views,
                    likes,
                    comments,
                    title,
                    title + " 설명입니다.",
                    LocalDateTime.now().minusDays(random.nextInt(30))));
        }

        log.info(">>> 콘텐츠 더미 데이터 {} 개 생성 완료", contents.size());
        return contents;
    }

    /**
     * 계정 데이터를 S3에 JSON으로 저장
     */
    private void saveAccountDataToS3(String platformType, LocalDate targetDate, List<AccountData> accounts) {
        log.info(">>> 계정 데이터 S3 저장 시작: {} 건", accounts.size());

        String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        try {
            for (int i = 0; i < accounts.size(); i++) {
                AccountData account = accounts.get(i);

                ObjectNode json = objectMapper.createObjectNode();
                json.put("accountNickname", account.accountNickname);
                json.put("categoryName", account.categoryName);
                json.put("followersCount", account.followersCount);
                json.put("profileImageUrl", account.profileImageUrl);
                json.put("bio", account.bio);
                json.put("createdAt", LocalDateTime.now().toString());

                String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

                String s3Path = String.format("raw_data/json/%s/platform_account/%s/account_%d.json",
                        platformType, dateFolder, i);

                uploadFile(s3Path, jsonData.getBytes(), "application/json");

                if ((i + 1) % 1000 == 0) {
                    log.info("진행 상황: {}/{}", i + 1, accounts.size());
                }
            }

            log.info(">>> 계정 데이터 S3 저장 완료: {} 건", accounts.size());

        } catch (Exception e) {
            log.error("계정 데이터 S3 저장 실패", e);
            throw new RuntimeException("계정 데이터 저장 실패", e);
        }
    }

    /**
     * 콘텐츠 데이터를 S3에 JSON으로 저장
     */
    private void saveContentDataToS3(String platformType, LocalDate targetDate, List<ContentData> contents) {
        log.info(">>> 콘텐츠 데이터 S3 저장 시작: {} 건", contents.size());

        String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        try {
            for (int i = 0; i < contents.size(); i++) {
                ContentData content = contents.get(i);

                ObjectNode json = objectMapper.createObjectNode();
                json.put("externalContentId", content.externalContentId);
                json.put("accountNickname", content.accountNickname);
                json.put("viewsCount", content.viewsCount);
                json.put("likesCount", content.likesCount);
                json.put("commentsCount", content.commentsCount);
                json.put("title", content.title);
                json.put("description", content.description);
                json.put("publishedAt", content.publishedAt.toString());
                json.put("createdAt", LocalDateTime.now().toString());

                String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

                String s3Path = String.format("raw_data/json/%s/content/%s/content_%d.json",
                        platformType, dateFolder, i);

                uploadFile(s3Path, jsonData.getBytes(), "application/json");

                if ((i + 1) % 1000 == 0) {
                    log.info("진행 상황: {}/{}", i + 1, contents.size());
                }
            }

            log.info(">>> 콘텐츠 데이터 S3 저장 완료: {} 건", contents.size());

        } catch (Exception e) {
            log.error("콘텐츠 데이터 S3 저장 실패", e);
            throw new RuntimeException("콘텐츠 데이터 저장 실패", e);
        }
    }

    /**
     * 계정 데이터를 DB에 저장 (Batch Insert)
     * influencer_id는 NULL로 저장 (인플루언서 데이터 미생성)
     */
    private void saveAccountDataToDB(String platformType, List<AccountData> accounts) {
        log.info(">>> 계정 데이터 DB 저장 시작: {} 건", accounts.size());

        String sql = "INSERT INTO platform_account (platform_type_id, account_nickname, created_at) " +
                "VALUES (?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE created_at = NOW()";

        try {
            jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    AccountData account = accounts.get(i);
                    ps.setString(1, platformType.toUpperCase());
                    ps.setString(2, account.accountNickname);
                }

                @Override
                public int getBatchSize() {
                    return accounts.size();
                }
            });
            log.info(">>> 계정 데이터 DB 저장 완료");
        } catch (Exception e) {
            log.error("계정 데이터 DB 저장 실패", e);
            throw new RuntimeException("계정 데이터 DB 저장 실패", e);
        }
    }

    /**
     * 콘텐츠 데이터를 DB에 저장 (Batch Insert)
     */
    private void saveContentDataToDB(String platformType, List<ContentData> contents) {
        log.info(">>> 콘텐츠 데이터 DB 저장 시작: {} 건", contents.size());

        String sql = "INSERT INTO content (platform_type_id, external_content_id, title, description, published_at, created_at) "
                +
                "VALUES (?, ?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE title = VALUES(title)";

        try {
            jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    ContentData content = contents.get(i);
                    ps.setString(1, platformType.toUpperCase());
                    ps.setString(2, content.externalContentId);
                    ps.setString(3, content.title);
                    ps.setString(4, content.description);
                    ps.setObject(5, content.publishedAt);
                }

                @Override
                public int getBatchSize() {
                    return contents.size();
                }
            });
            log.info(">>> 콘텐츠 데이터 DB 저장 완료");
        } catch (Exception e) {
            log.error("콘텐츠 데이터 DB 저장 실패", e);
            throw new RuntimeException("콘텐츠 데이터 DB 저장 실패", e);
        }
    }

    /**
     * 랜덤 이름 생성
     */
    private String generateRandomName() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return firstName + lastName;
    }

    /**
     * 계정 데이터 내부 클래스
     */
    public static class AccountData {
        public String accountNickname;
        public String categoryName;
        public int followersCount;
        public String profileImageUrl;
        public String bio;

        public AccountData(String accountNickname, String categoryName, int followersCount,
                String profileImageUrl, String bio) {
            this.accountNickname = accountNickname;
            this.categoryName = categoryName;
            this.followersCount = followersCount;
            this.profileImageUrl = profileImageUrl;
            this.bio = bio;
        }
    }

    /**
     * 콘텐츠 데이터 내부 클래스
     */
    public static class ContentData {
        public String externalContentId;
        public String accountNickname;
        public int viewsCount;
        public int likesCount;
        public int commentsCount;
        public String title;
        public String description;
        public LocalDateTime publishedAt;

        public ContentData(String externalContentId, String accountNickname, int viewsCount,
                int likesCount, int commentsCount, String title, String description,
                LocalDateTime publishedAt) {
            this.externalContentId = externalContentId;
            this.accountNickname = accountNickname;
            this.viewsCount = viewsCount;
            this.likesCount = likesCount;
            this.commentsCount = commentsCount;
            this.title = title;
            this.description = description;
            this.publishedAt = publishedAt;
        }
    }
}
