package com.gnilc.novum.image.api;

import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.novum.admin.support.AdminApiTestConfiguration;
import com.gnilc.novum.admin.support.AdminApiTestSupport;
import com.gnilc.novum.image.support.ImageApiTestConfiguration;
import com.gnilc.novum.image.support.ImageApiTestConfiguration.TestS3Service;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import com.gnilc.test.annotation.ApiTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ApiTest
@Import({
        AdminApiTestConfiguration.class,
        ImageApiTestConfiguration.class,
        RestExceptionHandlingConfiguration.class
})
@ContextConfiguration(
        classes = SystemTestApplication.class,
        initializers = SystemContainerContextInitializer.class)
@TestPropertySource(properties = {
        "app.s3.endpoint=http://127.0.0.1:9",
        "app.s3.region=auto",
        "app.s3.bucket=test-images",
        "app.s3.access-key=test-access-key",
        "app.s3.secret-key=test-secret-key",
        "app.s3.public-base-url=https://images.example.test",
        "app.s3.cleanup-cron=-"
})
class ImageApiIT extends AdminApiTestSupport {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TestS3Service storage;

    @BeforeEach
    void resetStorage() {
        storage.clear();
    }

    @Test
    void anonymousUploadRequestIsRejected() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"contentType\":\"image/png\",\"contentLength\":2048}")
                .post("/api/image/presign")
                .then()
                .statusCode(403)
                .body("code", equalTo(20003));
    }

    @Test
    void authenticatedAdminCanPresignAndFinalizeAnImage() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());

        String objectKey = presign(auth, "image/jpeg", 2048L);
        assertPending(objectKey, "image/jpeg", 2048L);

        storage.markUploaded(objectKey);
        finalize(auth, objectKey)
                .body("data.objectKey", equalTo(objectKey))
                .body("data.url", equalTo("https://images.example.test/" + objectKey))
                .body("data.status", equalTo("READY"));

        assertThat(statusOf(objectKey)).isEqualTo("READY");
    }

    @Test
    void authenticatedCustomerCanPresignAndFinalizeAnImage() {
        String auth = bearer(loginAsCustomer());

        String objectKey = presign(auth, "image/webp", 3072L);
        assertPending(objectKey, "image/webp", 3072L);

        storage.markUploaded(objectKey);
        finalize(auth, objectKey)
                .body("data.objectKey", equalTo(objectKey))
                .body("data.url", equalTo("https://images.example.test/" + objectKey));

        assertThat(statusOf(objectKey)).isEqualTo("READY");
    }

    @Test
    void imageManagerCanListReadyImagesAndDeleteOne() {
        String auth = bearer(loginAsDefaultAdmin().getAccessToken());
        String readyKey = createReadyImage(auth, "image/png", 4096L);
        String pendingKey = presign(auth, "image/jpeg", 1024L);
        Long imageId = imageId(readyKey);

        given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{\"currentPage\":1,\"pageSize\":10}")
                .post("/api/image/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.totalCount", equalTo(1))
                .body("data.list[0].objectKey", equalTo(readyKey));

        assertThat(statusOf(pendingKey)).isEqualTo("PENDING");
        given()
                .header("Authorization", auth)
                .post("/api/image/remove/{id}", imageId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertThat(storage.wasDeleted(readyKey)).isTrue();
        assertThat(storage.contains(readyKey)).isFalse();
        assertThat(jdbc.queryForObject(
                "select del from sys_image where id = ?", Integer.class, imageId)).isEqualTo(1);
    }

    @Test
    void adminWithoutImageManagerCannotListOrDeleteImages() {
        String managerAuth = bearer(loginAsDefaultAdmin().getAccessToken());
        String objectKey = createReadyImage(managerAuth, "image/png", 2048L);
        Long imageId = imageId(objectKey);
        String limitedAuth = bearer(loginAsLimitedAdmin().getAccessToken());

        given()
                .header("Authorization", limitedAuth)
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/api/image/page")
                .then()
                .statusCode(403);
        given()
                .header("Authorization", limitedAuth)
                .post("/api/image/remove/{id}", imageId)
                .then()
                .statusCode(403);

        assertThat(storage.contains(objectKey)).isTrue();
        assertThat(storage.wasDeleted(objectKey)).isFalse();
        assertThat(jdbc.queryForObject(
                "select del from sys_image where id = ?", Integer.class, imageId)).isZero();
    }

    private String presign(String auth, String contentType, long contentLength) {
        Response response = given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("""
                        {"contentType":"%s","contentLength":%d}
                        """.formatted(contentType, contentLength))
                .post("/api/image/presign")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.method", equalTo("PUT"))
                .body("data.headers.Content-Type", equalTo(contentType))
                .extract()
                .response();
        String objectKey = response.path("data.objectKey");
        assertThat(objectKey).matches(
                "images/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]{36}\\.(jpg|png|webp)");
        return objectKey;
    }

    private io.restassured.response.ValidatableResponse finalize(String auth, String objectKey) {
        return given()
                .header("Authorization", auth)
                .contentType(ContentType.JSON)
                .body("{\"objectKey\":\"" + objectKey + "\"}")
                .post("/api/image/finalize")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    private String createReadyImage(String auth, String contentType, long contentLength) {
        String objectKey = presign(auth, contentType, contentLength);
        storage.markUploaded(objectKey);
        finalize(auth, objectKey);
        return objectKey;
    }

    private void assertPending(String objectKey, String contentType, long contentLength) {
        MapRow row = jdbc.queryForObject("""
                select content_type, content_length, status
                  from sys_image
                 where object_key = ? and del = 0
                """, (result, rowNumber) -> new MapRow(
                result.getString("content_type"),
                result.getLong("content_length"),
                result.getString("status")), objectKey);
        assertThat(row).isEqualTo(new MapRow(contentType, contentLength, "PENDING"));
    }

    private String statusOf(String objectKey) {
        return jdbc.queryForObject(
                "select status from sys_image where object_key = ? and del = 0",
                String.class,
                objectKey);
    }

    private Long imageId(String objectKey) {
        return jdbc.queryForObject(
                "select id from sys_image where object_key = ? and del = 0",
                Long.class,
                objectKey);
    }

    private String loginAsCustomer() {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"customer\",\"password\":\"123456\"}")
                .post("/api/customer/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.accessToken");
    }

    @Data
    @AllArgsConstructor
    private static class MapRow {
        private final String contentType;
        private final long contentLength;
        private final String status;
    }
}
