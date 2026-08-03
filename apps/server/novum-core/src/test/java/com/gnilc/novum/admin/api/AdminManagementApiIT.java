package com.gnilc.novum.admin.api;

import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@ApiTest
@Import({
        AdminApiTestConfiguration.class,
        RestExceptionHandlingConfiguration.class
})
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
class AdminManagementApiIT extends AdminApiTestSupport {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createQueryUpdateRolesAndRemoveAdminThroughApi() {
        TokenPair pair = loginAsDefaultAdmin();
        String auth = bearer(pair.getAccessToken());

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username":"api-user",
                          "password":"Strong#123",
                          "nickname":"API User",
                          "homePath":"/workspace",
                          "status":true,
                          "roleCodes":["admin"]
                        }
                        """)
                .when()
                .post("/api/sys/admin/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        Long adminId = jdbc.queryForObject(
                "select id from sys_admin where username = 'api-user'", Long.class);
        Long userId = jdbc.queryForObject(
                "select user_id from sys_admin where id = ?", Long.class, adminId);
        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + adminId + ",\"roleCodes\":[]}")
                .when()
                .post("/api/sys/admin/roles/save")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        assertThat(activeRoleBindingCount(userId)).isEqualTo(1);
        assertThat(activeRoleCodes(userId)).containsExactly("admin");

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + adminId + ",\"roleCodes\":[\"admin\"]}")
                .when()
                .post("/api/sys/admin/roles/save")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
        assertThat(activeRoleBindingCount(userId)).isEqualTo(1);

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {"username":"api-user","currentPage":1,"pageSize":10}
                        """)
                .when()
                .post("/api/sys/admin/page")
                .then()
                .statusCode(200)
                .body("data.totalCount", equalTo(1))
                .body("data.list.username", hasItem("api-user"));

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + adminId + ",\"nickname\":\"Updated User\"}")
                .when()
                .post("/api/sys/admin/update")
                .then()
                .statusCode(200);
        assertThat(jdbc.queryForObject(
                "select nickname from sys_admin where id = ?", String.class, adminId))
                .isEqualTo("Updated User");

        given()
                .header("Authorization", auth)
                .when()
                .post("/api/sys/admin/remove/{id}", adminId)
                .then()
                .statusCode(200);
        assertThat(jdbc.queryForObject(
                "select count(*) from sys_admin where id = ? and del = 0", Integer.class, adminId))
                .isZero();
    }

    @Test
    void duplicateSubmissionAndInvalidRolesDoNotCreateDuplicatesOrReplaceValidBindings() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        Map<String, Object> request = adminRequest("repeat-user");

        postAdmin(auth, "/api/sys/admin/create", request).body("code", equalTo(0));
        postAdmin(auth, "/api/sys/admin/create", request).body("code", equalTo(10001));

        Long adminId = jdbc.queryForObject(
                "select id from sys_admin where username = 'repeat-user' and del = 0", Long.class);
        Long userId = jdbc.queryForObject(
                "select user_id from sys_admin where id = ?", Long.class, adminId);
        assertThat(jdbc.queryForObject(
                "select count(*) from sys_admin where username = 'repeat-user' and del = 0",
                Integer.class)).isEqualTo(1);

        postAdmin(auth, "/api/sys/admin/roles/save",
                Map.of("id", adminId,
                        "roleCodes", List.of("rbac:manager", "rbac:manager")))
                .body("code", equalTo(0));
        assertThat(activeRoleCodes(userId)).containsExactlyInAnyOrder("admin", "rbac:manager");
        assertThat(activeRoleBindingCount(userId)).isEqualTo(2);

        postAdmin(auth, "/api/sys/admin/roles/save",
                Map.of("id", adminId, "roleCodes", List.of("missing-role")))
                .body("code", equalTo(10002));
        assertThat(activeRoleCodes(userId)).containsExactlyInAnyOrder("admin", "rbac:manager");
    }

    @Test
    void currentAdministratorCannotDisableOrRemoveItself() {
        TokenPair pair = loginAsDefaultAdmin();
        String auth = bearer(pair.getAccessToken());
        Long adminId = jdbc.queryForObject(
                "select id from sys_admin where username = 'admin' and del = 0", Long.class);

        postAdmin(auth, "/api/sys/admin/update", Map.of("id", adminId, "status", false))
                .body("code", equalTo(10002));
        given()
                .header("Authorization", auth)
                .post("/api/sys/admin/remove/{id}", adminId)
                .then()
                .statusCode(200)
                .body("code", equalTo(10002));

        assertThat(jdbc.queryForObject(
                "select status from sys_admin where id = ? and del = 0", Boolean.class, adminId))
                .isTrue();
        given()
                .header("Authorization", auth)
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(200)
                .body("data.username", equalTo("admin"));
    }

    @Test
    void createRejectsFieldsBeyondBusinessLimitsWithoutLeavingPartialUsers() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        int adminCountBefore = countRows("sys_admin");
        int userCountBefore = countRows("az_user");
        List<Map<String, Object>> invalidRequests = List.of(
                adminRequest("u".repeat(256)),
                adminRequest("long-nickname", "n".repeat(256), null, null, null),
                adminRequest("long-avatar", "Valid", "a".repeat(501), null, null),
                adminRequest("long-description", "Valid", null, "d".repeat(501), null),
                adminRequest("long-home", "Valid", null, null, "/" + "h".repeat(500)));

        for (Map<String, Object> request : invalidRequests) {
            postAdmin(auth, "/api/sys/admin/create", request)
                    .body("code", equalTo(10001));
        }

        assertThat(countRows("sys_admin")).isEqualTo(adminCountBefore);
        assertThat(countRows("az_user")).isEqualTo(userCountBefore);
    }

    @Test
    void updateRejectsFieldsBeyondBusinessLimitsWithoutChangingTheAdministrator() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        postAdmin(auth, "/api/sys/admin/create", adminRequest("bounded-update"))
                .body("code", equalTo(0));
        Long adminId = jdbc.queryForObject(
                "select id from sys_admin where username = 'bounded-update' and del = 0", Long.class);
        List<Map<String, Object>> invalidUpdates = List.of(
                Map.of("id", adminId, "username", "u".repeat(256)),
                Map.of("id", adminId, "nickname", "n".repeat(256)),
                Map.of("id", adminId, "avatar", "a".repeat(501)),
                Map.of("id", adminId, "desc", "d".repeat(501)),
                Map.of("id", adminId, "homePath", "/" + "h".repeat(500)));

        for (Map<String, Object> request : invalidUpdates) {
            postAdmin(auth, "/api/sys/admin/update", request)
                    .body("code", equalTo(10001));
        }

        Map<String, Object> stored = jdbc.queryForMap("""
                select username, nickname, avatar, description, home_path
                  from sys_admin
                 where id = ? and del = 0
                """, adminId);
        assertThat(stored)
                .containsEntry("username", "bounded-update")
                .containsEntry("nickname", "API User")
                .containsEntry("home_path", "/dashboard");
        assertThat(stored.get("avatar")).isNull();
        assertThat(stored.get("description")).isNull();
    }

    @Test
    void exactMaximumProfileLengthsAreAcceptedWithoutTruncation() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        String nickname = "\uD83D\uDE00".repeat(255);
        String avatarPrefix = "https://example.test/";
        String avatar = avatarPrefix + "a".repeat(500 - avatarPrefix.length());
        String description = "\uD83D\uDE00".repeat(500);
        String homePath = "/" + "h".repeat(499);

        postAdmin(auth, "/api/sys/admin/create", adminRequest(
                "maximum-profile", nickname, avatar, description, homePath))
                .body("code", equalTo(0));

        Map<String, Object> stored = jdbc.queryForMap("""
                select nickname, avatar, description, home_path
                  from sys_admin
                 where username = 'maximum-profile' and del = 0
                """);
        assertThat(stored)
                .containsEntry("nickname", nickname)
                .containsEntry("avatar", avatar)
                .containsEntry("description", description)
                .containsEntry("home_path", homePath);
    }

    @Test
    void maximumLengthUsernameCanBeRemovedAndRecreated() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        String username = "u".repeat(255);

        postAdmin(auth, "/api/sys/admin/create", adminRequest(username))
                .body("code", equalTo(0));
        Long adminId = jdbc.queryForObject(
                "select id from sys_admin where username = ? and del = 0",
                Long.class,
                username);
        given()
                .header("Authorization", auth)
                .post("/api/sys/admin/remove/{id}", adminId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        postAdmin(auth, "/api/sys/admin/create", adminRequest(username))
                .body("code", equalTo(0));
        assertThat(jdbc.queryForObject(
                "select count(*) from sys_admin where username = ? and del = 0",
                Integer.class,
                username)).isEqualTo(1);
    }

    private io.restassured.response.ValidatableResponse postAdmin(
            String auth, String path, Object body) {
        return given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body(body)
                .post(path)
                .then()
                .statusCode(200);
    }

    private Map<String, Object> adminRequest(String username) {
        return adminRequest(username, "API User", null, null, null);
    }

    private Map<String, Object> adminRequest(String username, String nickname,
                                             String avatar, String description, String homePath) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("username", username);
        request.put("password", "Strong#123");
        request.put("nickname", nickname);
        request.put("avatar", avatar);
        request.put("desc", description);
        request.put("homePath", homePath);
        request.put("status", true);
        request.put("roleCodes", List.of());
        return request;
    }

    private int countRows(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private int activeRoleBindingCount(Long userId) {
        return jdbc.queryForObject(
                "select count(*) from az_user_role where user_id = ? and del = 0",
                Integer.class,
                userId);
    }

    private java.util.List<String> activeRoleCodes(Long userId) {
        return jdbc.queryForList("""
                select r.code
                  from az_user_role ur
                  join az_role r on r.id = ur.role_id
                 where ur.user_id = ?
                   and ur.del = 0
                   and r.del = 0
                """, String.class, userId);
    }
}
