package com.everypicfound.interaction.picturelike.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PictureLikeMapperXmlTest {

    private static final String NAMESPACE =
            "com.everypicfound.interaction.picturelike"
                    + ".infrastructure.persistence.mapper"
                    + ".PictureLikeMapper";

    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        configuration = new Configuration();
        ClassPathResource resource = new ClassPathResource(
                "mapper/picturelike/PictureLikeMapper.xml");
        try (InputStream inputStream = resource.getInputStream()) {
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    resource.getPath(),
                    configuration.getSqlFragments());
            builder.parse();
        }
    }

    @Test
    void insertIsOrdinaryInsertWithoutDatabaseSideIgnore() {
        String sql = sql(
                "insert",
                Map.of("pictureId", 7L, "userId", 42L));

        assertThat(sql)
                .startsWith("INSERT INTO picture_like_user")
                .doesNotContain("INSERT IGNORE")
                .doesNotContain("ON DUPLICATE KEY UPDATE")
                .doesNotContain("REPLACE");
    }

    @Test
    void likerQueryUsesDocumentedStableOrder() {
        String sql = sql(
                "findRecentLikers",
                Map.of("pictureId", 7L, "limit", 20));

        assertThat(sql)
                .contains(
                        "ORDER BY created_at DESC, user_id DESC")
                .endsWith("LIMIT ?");
    }

    @Test
    void historyCursorUsesStrictCompositeLessThan() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 42L);
        parameters.put(
                "cursorTime",
                LocalDateTime.of(2026, 7, 24, 12, 0));
        parameters.put("cursorPictureId", 7L);
        parameters.put("limit", 20);

        String sql = sql(
                "findRecentLikedPictures",
                parameters);

        assertThat(sql)
                .contains("created_at AS liked_at")
                .contains("created_at < ?")
                .contains("created_at = ?")
                .contains("picture_id < ?")
                .contains(
                        "ORDER BY created_at DESC, picture_id DESC")
                .endsWith("LIMIT ?");
    }

    @Test
    void firstHistoryPageHasNoCursorPredicate() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 42L);
        parameters.put("cursorTime", null);
        parameters.put("cursorPictureId", null);
        parameters.put("limit", 20);

        String sql = sql(
                "findRecentLikedPictures",
                parameters);

        assertThat(sql)
                .doesNotContain("created_at < ?")
                .contains(
                        "ORDER BY created_at DESC, picture_id DESC");
    }

    private String sql(
            String statement,
            Object parameters) {
        BoundSql boundSql = configuration
                .getMappedStatement(NAMESPACE + "." + statement)
                .getBoundSql(parameters);
        return boundSql.getSql()
                .replaceAll("\\s+", " ")
                .trim();
    }
}
