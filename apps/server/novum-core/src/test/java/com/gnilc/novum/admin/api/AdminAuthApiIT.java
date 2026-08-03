package com.gnilc.novum.admin.api;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
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

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@ApiTest
@Import({
        AdminApiTestConfiguration.class,
        RestExceptionHandlingConfiguration.class
})
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
class AdminAuthApiIT extends AdminApiTestSupport {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PermissionCacheService cacheService;

    @Test
    void loginRepeatedRefreshAndLogoutRunThroughTheRealHttpAndRedisStack() {
        TokenPair pair = loginAsDefaultAdmin();

        String firstRefreshedAccessToken = given()
                .header("X-Refresh-Token", pair.getRefreshToken())
                .when()
                .post("/api/sys/admin/refresh")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.refreshToken", equalTo(pair.getRefreshToken()))
                .body("data.accessToken", not(equalTo(pair.getAccessToken())))
                .extract()
                .path("data.accessToken");

        given()
                .header("Authorization", bearer(pair.getAccessToken()))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(401);

        given()
                .header("Authorization", bearer(firstRefreshedAccessToken))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(200)
                .body("data.username", equalTo("admin"));

        String secondRefreshedAccessToken = given()
                .header("X-Refresh-Token", pair.getRefreshToken())
                .when()
                .post("/api/sys/admin/refresh")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.refreshToken", equalTo(pair.getRefreshToken()))
                .body("data.accessToken", not(equalTo(firstRefreshedAccessToken)))
                .extract()
                .path("data.accessToken");

        given().header("Authorization", bearer(firstRefreshedAccessToken))
                .get("/api/sys/admin/user-info").then().statusCode(401);
        given().header("Authorization", bearer(secondRefreshedAccessToken))
                .get("/api/sys/admin/user-info").then().statusCode(200);

        given()
                .header("X-Refresh-Token", pair.getRefreshToken())
                .when()
                .post("/api/sys/admin/logout")
                .then()
                .statusCode(200);

        given()
                .header("X-Refresh-Token", pair.getRefreshToken())
                .when()
                .post("/api/sys/admin/refresh")
                .then()
                .statusCode(401)
                .body("code", equalTo(20002))
                .body("error", equalTo("Your login has expired. Please sign in again."))
                .body("message", equalTo("Your login has expired. Please sign in again."));

        given()
                .header("Authorization", bearer(secondRefreshedAccessToken))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(401);

        given()
                .header("X-Refresh-Token", pair.getRefreshToken())
                .when()
                .post("/api/sys/admin/logout")
                .then()
                .statusCode(401)
                .body("code", equalTo(20002));
    }

    @Test
    void refreshRejectsMissingBlankMalformedAndAccessTokens() {
        TokenPair pair = loginAsDefaultAdmin();

        given()
                .header("Accept-Language", "zh-CN")
                .post("/api/sys/admin/refresh")
                .then()
                .statusCode(401)
                .body("code", equalTo(20002))
                .body("error", equalTo("登录已过期，请重新登录。"))
                .body("message", equalTo("登录已过期，请重新登录。"));

        for (String invalidToken : List.of(
                " ",
                "not-a-token",
                "sys_admin.not-a-number.value",
                pair.getAccessToken())) {
            given()
                    .header("X-Refresh-Token", invalidToken)
                    .post("/api/sys/admin/refresh")
                    .then()
                    .statusCode(401)
                    .body("code", equalTo(20002));
        }
    }

    @Test
    void invalidCredentialsReturnAuthenticationBusinessError() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"username":"admin","password":"wrong"}
                        """)
                .when()
                .post("/api/sys/admin/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(20001))
                .body("error", equalTo("Incorrect username or password."));

        given()
                .header("Accept-Language", "zh-CN")
                .contentType(ContentType.JSON)
                .body("""
                        {"username":"admin","password":"wrong"}
                        """)
                .when()
                .post("/api/sys/admin/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(20001))
                .body("error", equalTo("用户名或密码错误。"));
    }

    @Test
    void currentAdministratorReceivesTheirBackendNavigationRoutes() {
        TokenPair pair = loginAsDefaultAdmin();

        given()
                .header("Authorization", bearer(pair.getAccessToken()))
                .when()
                .get("/api/sys/admin/menu/routes")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.size()", equalTo(3))
                .body("data[0].name", equalTo("Dashboard"))
                .body("data[0].path", equalTo("/dashboard"))
                .body("data[0].component", equalTo("/dashboard/index"))
                .body("data[0].meta.title", equalTo("menu.dashboard.title"))
                .body("data[1].name", equalTo("System"))
                .body("data[1].path", equalTo("/system"))
                .body("data[1].children.size()", equalTo(5))
                .body("data[1].children.name", contains(
                        "I18nMessage", "Admin", "Role", "Permission", "Menu"))
                .body("data[1].children[0].path", equalTo("/system/i18n-message"))
                .body("data[1].children[0].meta.title", equalTo("menu.i18nMessage.title"))
                .body("data[2].name", equalTo("Profile"))
                .body("data[2].path", equalTo("/profile"))
                .body("data[2].meta.title", equalTo("menu.profile.title"))
                .body("data[2].meta.hideInMenu", equalTo(true));
    }

    @Test
    void backendNavigationRoutesUseTheCurrentSessionUserMenuBindings() {
        Long limitedRoleId = jdbc.queryForObject(
                "select id from az_role where code = 'limited' and del = 0", Long.class);
        Long routePermissionId = jdbc.queryForObject("""
                select id from az_permission
                where code = 'GET:/sys/admin/menu/routes' and del = 0
                """, Long.class);
        jdbc.update("""
                insert into az_role_permission
                    (del, create_time, role_id, permission_id)
                values (0, now(), ?, ?)
                """, limitedRoleId, routePermissionId);
        jdbc.update("""
                insert into az_menu
                    (del, create_time, pid, type, status, name, path, component,
                     iframe_src, link, `order`, title)
                values
                    (0, now(), 0, 'catalog', 1, 'LimitedCatalog', '/limited', null,
                     null, null, 1, 'Limited'),
                    (0, now(), 0, 'embedded', 1, 'LimitedDocs', '/limited-docs', null,
                     'https://example.test/docs', null, 2, 'Limited docs'),
                    (0, now(), 0, 'link', 1, 'LimitedRepository', '/limited-repository', null,
                     null, 'https://example.test/repository', 3, 'Limited repository'),
                    (0, now(), 0, 'catalog', 1, 'LimitedEmpty', '/limited-empty', null,
                     null, null, 4, 'Limited empty'),
                    (0, now(), 0, 'catalog', 0, 'LimitedDisabled', '/limited-disabled', null,
                     null, null, 5, 'Limited disabled'),
                    (0, now(), 0, 'menu', 1, 'LimitedUnauthorized', '/limited-unauthorized',
                     '/dashboard/index', null, null, 6, 'Limited unauthorized')
                """);
        Long catalogId = menuId("LimitedCatalog");
        Long disabledId = menuId("LimitedDisabled");
        jdbc.update("""
                insert into az_menu
                    (del, create_time, pid, type, status, access_code, name, path, component,
                     query, `order`, title)
                values
                    (0, now(), ?, 'menu', 1, null, 'LimitedHome', 'home',
                     '/dashboard/index', '{"tab":"recent"}', 1, 'Limited home'),
                    (0, now(), ?, 'button', 1, 'limited:export', 'LimitedExport', null,
                     null, null, 2, 'Limited export'),
                    (0, now(), ?, 'menu', 1, null, 'LimitedDisabledChild', 'child',
                     '/dashboard/index', null, 1, 'Limited disabled child')
                """, catalogId, catalogId, disabledId);
        jdbc.update("""
                insert into az_role_menu
                    (del, create_time, role_id, menu_id)
                select 0, now(), ?, id
                from az_menu
                where name in ('LimitedHome', 'LimitedExport', 'LimitedDocs',
                               'LimitedRepository', 'LimitedEmpty', 'LimitedDisabledChild')
                  and del = 0
                """, limitedRoleId);
        cacheService.resetAll();
        TokenPair pair = loginAsLimitedAdmin();

        given()
                .header("Authorization", bearer(pair.getAccessToken()))
                .when()
                .get("/api/sys/admin/menu/routes")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.size()", equalTo(3))
                .body("data[0].name", equalTo("LimitedCatalog"))
                .body("data[0].children.size()", equalTo(1))
                .body("data[0].children[0].name", equalTo("LimitedHome"))
                .body("data[0].children[0].component", equalTo("/dashboard/index"))
                .body("data[0].children[0].meta.query.tab", equalTo("recent"))
                .body("data[0].children[0].children.size()", equalTo(0))
                .body("data[1].name", equalTo("LimitedDocs"))
                .body("data[1].component", equalTo("IFrameView"))
                .body("data[1].meta.iframeSrc", equalTo("https://example.test/docs"))
                .body("data[2].name", equalTo("LimitedRepository"))
                .body("data[2].component", equalTo("IFrameView"))
                .body("data[2].meta.link", equalTo("https://example.test/repository"))
                .body("data.name", not(hasItem("LimitedEmpty")))
                .body("data.name", not(hasItem("LimitedExport")))
                .body("data.name", not(hasItem("LimitedDisabled")))
                .body("data.name", not(hasItem("LimitedDisabledChild")))
                .body("data.name", not(hasItem("LimitedUnauthorized")));
    }

    private Long menuId(String name) {
        return jdbc.queryForObject(
                "select id from az_menu where name = ? and del = 0", Long.class, name);
    }

    @Test
    void malformedLoginRequestUsesTheCommonExceptionFormat() {
        given()
                .contentType(ContentType.JSON)
                .body("{")
                .when()
                .post("/api/sys/admin/login")
                .then()
                .statusCode(400)
                .body("code", equalTo(10001))
                .body("error", equalTo("The request body is malformed."));
    }

    @Test
    void currentAdministratorCanUpdateOnlyTheirEditableProfileFields() {
        TokenPair pair = loginAsDefaultAdmin();

        given()
                .header("Authorization", bearer(pair.getAccessToken()))
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "id": 999999,
                          "username": "hijacked",
                          "password": "Changed#456",
                          "nickname": "Updated Administrator",
                          "avatar": "  ",
                          "desc": " ",
                          "homePath": "/hijacked",
                          "status": false,
                          "roleCodes": []
                        }
                        """)
                .when()
                .post("/api/sys/admin/user-info/update")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .header("Authorization", bearer(pair.getAccessToken()))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(200)
                .body("data.username", equalTo("admin"))
                .body("data.nickname", equalTo("Updated Administrator"))
                .body("data.avatar", nullValue())
                .body("data.desc", nullValue())
                .body("data.homePath", equalTo("/dashboard"));
    }

    @Test
    void currentAdministratorProfileRejectsBlankNickname() {
        TokenPair pair = loginAsDefaultAdmin();

        given()
                .header("Authorization", bearer(pair.getAccessToken()))
                .contentType(ContentType.JSON)
                .body("{\"nickname\":\"   \",\"avatar\":\"https://example.test/changed.png\"}")
                .when()
                .post("/api/sys/admin/user-info/update")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("Nickname is required."));
    }

    @Test
    void currentAdministratorPasswordUpdateRevokesEveryExistingSession() {
        TokenPair first = loginAsDefaultAdmin();
        TokenPair second = loginAsDefaultAdmin();

        given()
                .header("Authorization", bearer(first.getAccessToken()))
                .contentType(ContentType.JSON)
                .body("""
                        {"oldPassword":"123456","newPassword":"Changed#456"}
                        """)
                .when()
                .post("/api/sys/admin/password/update")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given().header("Authorization", bearer(first.getAccessToken()))
                .get("/api/sys/admin/user-info").then().statusCode(401);
        given().header("Authorization", bearer(second.getAccessToken()))
                .get("/api/sys/admin/user-info").then().statusCode(401);
        given().header("X-Refresh-Token", first.getRefreshToken())
                .post("/api/sys/admin/refresh").then().statusCode(401);
        given().header("X-Refresh-Token", second.getRefreshToken())
                .post("/api/sys/admin/refresh").then().statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"123456\"}")
                .post("/api/sys/admin/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(20001));
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"Changed#456\"}")
                .post("/api/sys/admin/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    @Test
    void invalidCurrentPasswordDoesNotChangePasswordOrRevokeSession() {
        TokenPair pair = loginAsDefaultAdmin();

        given()
                .header("Authorization", bearer(pair.getAccessToken()))
                .contentType(ContentType.JSON)
                .body("{\"oldPassword\":\"Wrong#123\",\"newPassword\":\"Changed#456\"}")
                .post("/api/sys/admin/password/update")
                .then()
                .statusCode(200)
                .body("code", equalTo(10001))
                .body("error", equalTo("Current password is incorrect."));

        given()
                .header("Authorization", bearer(pair.getAccessToken()))
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(200)
                .body("data.username", equalTo("admin"));
    }
}
