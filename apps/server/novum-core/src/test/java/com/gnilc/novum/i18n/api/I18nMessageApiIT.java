package com.gnilc.novum.i18n.api;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
import com.gnilc.novum.admin.support.AdminApiTestConfiguration;
import com.gnilc.novum.admin.support.AdminApiTestSupport;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import com.gnilc.test.annotation.ApiTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@ApiTest
@Import(AdminApiTestConfiguration.class)
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
class I18nMessageApiIT extends AdminApiTestSupport {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PermissionCacheService cacheService;

    @Test
    void defaultAdministratorCanManageAndReloadDynamicMessages() {
        TokenPair pair = loginAsDefaultAdmin();
        String auth = bearer(pair.getAccessToken());

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/bundle/admin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.zh-CN.menu.dashboard.title", equalTo("首页"));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/categories")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(2))
                .body("data[0]", equalTo("default"))
                .body("data[1]", equalTo("admin"));

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "category":"default",
                          "messageKey":"api.message.title",
                          "values":[
                            {"locale":"zh-CN","value":"接口消息"},
                            {"locale":"en-US","value":"API message"}
                          ]
                        }
                        """)
                .post("/api/sys/i18n-message/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.category", equalTo("default"))
                .body("data.values", hasSize(2));

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "category":"admin",
                          "messageKey":"api.message.title",
                          "values":[
                            {"locale":"en-US","value":"Replacement"}
                          ]
                        }
                        """)
                .post("/api/sys/i18n-message/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("Message key already exists"));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/values/api.message.title")
                .then()
                .statusCode(200)
                .body("data.category", equalTo("default"))
                .body("data.values[1].value", equalTo("API message"));

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "category":"admin",
                          "messageKey":"api.message.title",
                          "values":[
                            {"locale":"zh-CN","value":""},
                            {"locale":"en-US","value":"API heading"}
                          ]
                        }
                        """)
                .post("/api/sys/i18n-message/save")
                .then()
                .statusCode(200)
                .body("data.category", equalTo("admin"))
                .body("data.messageKey", equalTo("api.message.title"))
                .body("data.values", hasSize(1));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/values/api.message.title")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.category", equalTo("admin"))
                .body("data.messageKey", equalTo("api.message.title"))
                .body("data.values[0].locale", equalTo("en-US"));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/remove/api.message.title")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_i18n
                 WHERE category = 'admin' AND message_key LIKE 'api.message.%'
                """, Integer.class)).isZero();
    }

    @Test
    void baselineAdministratorCanReadBundleButCannotManageMessages() {
        Long limitedUserId = jdbc.queryForObject("""
                SELECT user_id FROM sys_admin WHERE username = 'limited' AND del = 0
                """, Long.class);
        Long adminRoleId = jdbc.queryForObject("""
                SELECT id FROM az_role WHERE code = 'admin' AND del = 0
                """, Long.class);
        jdbc.update("""
                INSERT INTO az_user_role (del, create_time, user_id, role_id)
                VALUES (0, NOW(), ?, ?)
                """, limitedUserId, adminRoleId);
        cacheService.resetAll();

        TokenPair limited = loginAsLimitedAdmin();
        given()
                .header("Authorization", bearer(limited.getAccessToken()))
                .post("/api/sys/i18n-message/bundle/admin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        given()
                .header("Authorization", bearer(limited.getAccessToken()))
                .contentType(ContentType.JSON)
                .body("{\"currentPage\":1,\"pageSize\":10}")
                .post("/api/sys/i18n-message/page")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
    }

    @Test
    void runtimeBundleRequiresASupportedCategoryPath() {
        TokenPair admin = loginAsDefaultAdmin();
        given()
                .header("Authorization", bearer(admin.getAccessToken()))
                .header("Accept-Language", "en-US")
                .post("/api/sys/i18n-message/bundle/unknown")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("Category unknown is not supported."));
    }

    @Test
    void pathKeysUseTheExistingServiceValidationAndErrorEnvelope() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/values/menu..title")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("The internationalization key must be a valid dot path."));

        given()
                .header("Authorization", auth)
                .post("/api/sys/i18n-message/remove/{messageKey}", "a".repeat(192))
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("The internationalization key must not exceed 191 characters."));
    }

    @Test
    void createRejectsInvalidValueCollectionsWithoutPersistingPartialLocales() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        List<String> invalidBodies = List.of(
                """
                        {"category":"admin","messageKey":"api.invalid.null","values":null}
                        """,
                """
                        {"category":"admin","messageKey":"api.invalid.locale","values":[
                          {"locale":"fr-FR","value":"Unsupported"},
                          {"locale":"en-US","value":"Fallback"}
                        ]}
                        """,
                """
                        {"category":"admin","messageKey":"api.invalid.duplicate","values":[
                          {"locale":"en-US","value":"First"},
                          {"locale":"en-US","value":"Second"}
                        ]}
                        """,
                """
                        {"category":"admin","messageKey":"api.invalid.fallback","values":[
                          {"locale":"zh-CN","value":"缺少兜底"}
                        ]}
                        """,
                """
                        {"category":"admin","messageKey":"api.invalid.value","values":[
                          {"locale":"en-US","value":"%s"}
                        ]}
                        """.formatted("v".repeat(4001)));

        for (String body : invalidBodies) {
            given()
                    .header("Authorization", auth)
                    .header("Accept-Language", "en-US")
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post("/api/sys/i18n-message/create")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(10001));
        }

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_i18n
                 WHERE message_key LIKE 'api.invalid.%'
                """, Integer.class)).isZero();
    }

    @Test
    void unicodeMessageValuesUseTheFourThousandCodePointBoundary() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        String exactMaximum = "\uD83D\uDE00".repeat(4000);

        given()
                .header("Authorization", auth)
                .header("Accept-Language", "en-US")
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "category", "admin",
                        "messageKey", "api.unicode.maximum",
                        "values", List.of(Map.of("locale", "en-US", "value", exactMaximum))))
                .post("/api/sys/i18n-message/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertThat(jdbc.queryForObject("""
                SELECT CHAR_LENGTH(i18n_value) FROM sys_i18n
                 WHERE message_key = 'api.unicode.maximum' AND locale = 'en-US'
                """, Integer.class)).isEqualTo(4000);

        given()
                .header("Authorization", auth)
                .header("Accept-Language", "en-US")
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "category", "admin",
                        "messageKey", "api.unicode.overflow",
                        "values", List.of(Map.of(
                                "locale", "en-US",
                                "value", "\uD83D\uDE00".repeat(4001)))))
                .post("/api/sys/i18n-message/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001));

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_i18n
                 WHERE message_key = 'api.unicode.overflow'
                """, Integer.class)).isZero();
    }

    @Test
    void concurrentDuplicateCreatesPersistExactlyOneMessage() throws Exception {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        String body = """
                {
                  "category":"admin",
                  "messageKey":"api.concurrent.create",
                  "values":[{"locale":"en-US","value":"Original"}]
                }
                """;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> first = executor.submit(() -> createConcurrently(auth, body, ready, start));
            Future<Integer> second = executor.submit(() -> createConcurrently(auth, body, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(0, 10001);
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_i18n
                     WHERE message_key = 'api.concurrent.create'
                    """, Integer.class)).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private int createConcurrently(
            String auth,
            String body,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent create was not released");
        }
        return given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/sys/i18n-message/create")
                .then()
                .statusCode(200)
                .extract()
                .path("code");
    }
}
