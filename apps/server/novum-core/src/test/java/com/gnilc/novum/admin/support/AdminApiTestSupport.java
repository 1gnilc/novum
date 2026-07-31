package com.gnilc.novum.admin.support;

import com.gnilc.test.api.ApiTestSupport;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public abstract class AdminApiTestSupport extends ApiTestSupport {
    protected TokenPair loginAsDefaultAdmin() {
        return login("admin", "123456");
    }

    protected TokenPair loginAsLimitedAdmin() {
        return login(AdminApiTestConfiguration.LIMITED_USERNAME, AdminApiTestConfiguration.LIMITED_PASSWORD);
    }

    private TokenPair login(String username, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password))
                .when()
                .post("/api/sys/admin/login")
                .then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .extract()
                .response();
        return new TokenPair(response.path("data.accessToken"), response.path("data.refreshToken"));
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected record TokenPair(String accessToken, String refreshToken) {
    }
}
