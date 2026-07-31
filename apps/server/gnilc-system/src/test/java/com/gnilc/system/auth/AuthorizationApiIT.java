package com.gnilc.system.auth;

import com.gnilc.system.admin.support.AdminApiTestConfiguration;
import com.gnilc.system.admin.support.AdminApiTestSupport;
import com.gnilc.system.support.SystemContainerContextInitializer;
import com.gnilc.system.support.SystemTestApplication;
import com.gnilc.test.annotation.ApiTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@ApiTest
@Import(AdminApiTestConfiguration.class)
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
class AuthorizationApiIT extends AdminApiTestSupport {
    @Test
    void anonymousProtectedRequestIsForbiddenWithLocalizedJsonContract() {
        given()
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(403)
                .contentType("application/json;charset=UTF-8")
                .body("code", equalTo(20003))
                .body("error", equalTo("Access denied."));

        given()
                .header("Accept-Language", "zh-CN")
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003))
                .body("error", equalTo("访问被拒绝。"));
    }

    @Test
    void authenticatedAdminReceivesRolesAndButtonAccessCodes() {
        TokenPair pair = loginAsDefaultAdmin();

        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/role-codes")
                .then()
                .statusCode(200)
                .body("data", hasItem("admin"))
                .body("data", hasItem("i18n:manager"))
                .body("data", hasItem("rbac:manager"));
        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/menu/access-codes")
                .then()
                .statusCode(200)
                .body("data", hasItem("system:admin:create"))
                .body("data", hasItem("system:role:manage-permissions"))
                .body("data", hasItem("system:i18n-message:save"));
    }

    @Test
    void authenticatedUserWithoutRequiredPermissionIsForbidden() {
        TokenPair pair = loginAsLimitedAdmin();

        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/menu/routes")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
    }

    @Test
    void roleChangesApplyTheBaselineRbacAndI18nPermissionMatrixImmediately() {
        TokenPair manager = loginAsDefaultAdmin();
        String managerAuth = bearer(manager.accessToken());
        String limitedAdminId = given()
                .header("Authorization", managerAuth)
                .contentType(ContentType.JSON)
                .body("{\"username\":\"limited\",\"currentPage\":1,\"pageSize\":10}")
                .post("/api/sys/admin/page")
                .then()
                .statusCode(200)
                .body("data.totalCount", equalTo(1))
                .extract()
                .jsonPath()
                .getString("data.list[0].id");

        replaceRoles(managerAuth, limitedAdminId, List.of());
        String limitedAuth = bearer(loginAsLimitedAdmin().accessToken());
        assertGetStatus(limitedAuth, "/api/sys/admin/user-info", 200);
        assertPostStatus(limitedAuth, "/api/sys/i18n-message/bundle/admin", 200);
        assertPostStatus(limitedAuth, "/api/sys/admin/page", 403);
        assertPostStatus(limitedAuth, "/api/authz/role/list", 403);
        assertPostStatus(limitedAuth, "/api/sys/i18n-message/page", 403);
        assertPostStatus(limitedAuth, "/api/sys/i18n-message/values/menu.dashboard.title", 403);

        replaceRoles(managerAuth, limitedAdminId, List.of("rbac:manager"));
        assertGetStatus(limitedAuth, "/api/sys/admin/user-info", 200);
        assertPostStatus(limitedAuth, "/api/sys/admin/page", 200);
        assertPostStatus(limitedAuth, "/api/authz/role/list", 200);
        assertPostStatus(limitedAuth, "/api/sys/i18n-message/values/menu.dashboard.title", 200);
        assertPostStatus(limitedAuth, "/api/sys/i18n-message/page", 403);
        assertPostStatus(limitedAuth, "/api/sys/i18n-message/remove/menu.dashboard.title", 403);

        replaceRoles(managerAuth, limitedAdminId, List.of("i18n:manager"));
        assertGetStatus(limitedAuth, "/api/sys/admin/user-info", 200);
        assertPostStatus(limitedAuth, "/api/sys/i18n-message/page", 200);
        assertPostStatus(limitedAuth, "/api/sys/i18n-message/categories", 200);
        assertPostStatus(limitedAuth, "/api/sys/admin/page", 403);
        assertPostStatus(limitedAuth, "/api/authz/role/list", 403);

        assertPostStatus(managerAuth, "/api/sys/admin/page", 200);
        assertPostStatus(managerAuth, "/api/authz/role/list", 200);
        assertPostStatus(managerAuth, "/api/sys/i18n-message/page", 200);
    }

    @Test
    void namespacedBearerParsingRejectsWrongTokenTypesAndMalformedValues() {
        TokenPair pair = loginAsDefaultAdmin();

        given()
                .header("Authorization", "bearer  " + pair.accessToken())
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .header("Authorization", bearer(pair.refreshToken()))
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(401)
                .contentType("application/json;charset=UTF-8")
                .body("code", equalTo(20002))
                .body("data", equalTo(null))
                .body("error", equalTo("The access token is invalid or has expired."))
                .body("message", equalTo("The access token is invalid or has expired."));

        given()
                .header("Authorization", "Bearer sys_admin.not-a-number.value")
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(401)
                .contentType("application/json;charset=UTF-8")
                .body("code", equalTo(20002))
                .body("data", equalTo(null))
                .body("error", equalTo("The access token is invalid or has expired."))
                .body("message", equalTo("The access token is invalid or has expired."));

        given()
                .header("Authorization", bearer(pair.accessToken()) + " trailing")
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(401)
                .contentType("application/json;charset=UTF-8")
                .body("code", equalTo(20002))
                .body("data", equalTo(null))
                .body("error", equalTo("The access token is invalid or has expired."))
                .body("message", equalTo("The access token is invalid or has expired."));
    }

    @Test
    void anonymousAndLimitedUsersCannotUpdateCurrentAdministratorProfile() {
        given()
                .contentType("application/json")
                .body("{\"nickname\":\"Anonymous\"}")
                .when()
                .post("/api/sys/admin/user-info/update")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));

        TokenPair pair = loginAsLimitedAdmin();
        given()
                .header("Authorization", bearer(pair.accessToken()))
                .contentType("application/json")
                .body("{\"nickname\":\"Limited\"}")
                .when()
                .post("/api/sys/admin/user-info/update")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
    }

    @Test
    void anonymousAndLimitedUsersCannotUpdateCurrentAdministratorPassword() {
        String request = "{\"oldPassword\":\"123456\",\"newPassword\":\"Changed#456\"}";
        given()
                .contentType("application/json")
                .body(request)
                .post("/api/sys/admin/password/update")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));

        TokenPair pair = loginAsLimitedAdmin();
        given()
                .header("Authorization", bearer(pair.accessToken()))
                .contentType("application/json")
                .body(request)
                .post("/api/sys/admin/password/update")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
    }

    @Test
    void revokedNamespacedAccessTokenReturns401BeforeAuthorization() {
        TokenPair pair = loginAsDefaultAdmin();
        given().header("X-Refresh-Token", pair.refreshToken())
                .post("/api/sys/admin/logout")
                .then().statusCode(200);

        given()
                .header("Authorization", bearer(pair.accessToken()))
                .when()
                .get("/api/sys/admin/user-info")
                .then()
                .statusCode(401)
                .contentType("application/json;charset=UTF-8")
                .body("code", equalTo(20002))
                .body("data", equalTo(null))
                .body("error", equalTo("The access token is invalid or has expired."))
                .body("message", equalTo("The access token is invalid or has expired."));
    }

    private void replaceRoles(String managerAuth, String adminId, List<String> roleCodes) {
        given()
                .header("Authorization", managerAuth)
                .contentType(ContentType.JSON)
                .body(Map.of("id", adminId, "roleCodes", roleCodes))
                .post("/api/sys/admin/roles/save")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    private void assertGetStatus(String auth, String path, int status) {
        given()
                .header("Authorization", auth)
                .get(path)
                .then()
                .statusCode(status)
                .body("code", equalTo(status == 200 ? 0 : 20003));
    }

    private void assertPostStatus(String auth, String path, int status) {
        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{}")
                .post(path)
                .then()
                .statusCode(status)
                .body("code", equalTo(status == 200 ? 0 : 20003));
    }
}
