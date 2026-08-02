package com.gnilc.novum.customer.api;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.novum.customer.support.CustomerApiTestConfiguration;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import com.gnilc.test.annotation.ApiTest;
import com.gnilc.test.api.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@ApiTest
@Import({CustomerApiTestConfiguration.class, RestExceptionHandlingConfiguration.class})
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
class CustomerAuthApiIT extends ApiTestSupport {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PermissionCacheService permissionCache;
    @Autowired
    private UserRoleService userRoles;

    @Test
    void loginRefreshUserInfoAndLogoutUseTheRealHttpAndRedisStack() {
        TokenPair pair = loginCustomer();
        given()
                .header("Authorization", bearer(pair.accessToken()))
                .get("/api/customer/user-info")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.username", equalTo("customer"))
                .body("data.nickname", equalTo("客户"))
                .body("data.avatar", nullValue())
                .body("data.roleCodes", containsInAnyOrder("customer"))
                .body("data.password", nullValue())
                .body("data.homePath", nullValue());

        String newAccessToken = given()
                .header("X-Refresh-Token", pair.refreshToken())
                .post("/api/customer/refresh")
                .then()
                .statusCode(200)
                .body("data.refreshToken", equalTo(pair.refreshToken()))
                .body("data.accessToken", not(equalTo(pair.accessToken())))
                .extract().path("data.accessToken");

        given().header("Authorization", bearer(pair.accessToken()))
                .get("/api/customer/user-info").then().statusCode(401);
        given().header("Authorization", bearer(newAccessToken))
                .get("/api/customer/user-info").then().statusCode(200);

        given().header("X-Refresh-Token", pair.refreshToken())
                .post("/api/customer/logout").then().statusCode(200);
        given().header("Authorization", bearer(newAccessToken))
                .get("/api/customer/user-info").then().statusCode(401);
        given().header("X-Refresh-Token", pair.refreshToken())
                .post("/api/customer/refresh").then().statusCode(401);
    }

    @Test
    void invalidCredentialsAndRefreshTokensKeepTheCustomerFailureBoundary() {
        given().contentType(ContentType.JSON)
                .body("{\"username\":\"customer\",\"password\":\"wrong\"}")
                .post("/api/customer/login")
                .then().statusCode(200)
                .body("code", equalTo(20001))
                .body("error", equalTo("Incorrect username or password."));

        given().post("/api/customer/refresh")
                .then().statusCode(401).body("code", equalTo(20002));
        given().header("X-Refresh-Token", "sys_admin.7.value")
                .post("/api/customer/refresh")
                .then().statusCode(401).body("code", equalTo(20002));
        given().header("Authorization", "Bearer customer.7.invalid")
                .get("/api/customer/user-info")
                .then().statusCode(401);
    }

    @Test
    void explicitGlobalRbacGrantAppliesToTheCustomerPrincipal() {
        Long userId = customerUserId();
        Long adminRoleId = roleId("admin");
        userRoles.bindRole(userId, adminRoleId);
        permissionCache.resetAll();
        TokenPair pair = loginCustomer();

        given().header("Authorization", bearer(pair.accessToken()))
                .get("/api/sys/admin/role-codes")
                .then().statusCode(200)
                .body("data", containsInAnyOrder("admin", "customer"));
    }

    private TokenPair loginCustomer() {
        Response response = given().contentType(ContentType.JSON)
                .body("{\"username\":\"customer\",\"password\":\"123456\"}")
                .post("/api/customer/login")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .extract().response();
        String accessToken = response.path("data.accessToken");
        String refreshToken = response.path("data.refreshToken");
        org.assertj.core.api.Assertions.assertThat(accessToken).startsWith("customer.");
        org.assertj.core.api.Assertions.assertThat(refreshToken).startsWith("customer.");
        return new TokenPair(accessToken, refreshToken);
    }

    private Long customerUserId() {
        return jdbc.queryForObject(
                "select user_id from nv_customer where username = 'customer' and del = 0", Long.class);
    }

    private Long roleId(String code) {
        return jdbc.queryForObject(
                "select id from az_role where code = ? and del = 0", Long.class, code);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
