package com.mfa.marketpulse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Health and OpenAPI endpoints provided by Quarkus extensions (M0 acceptance criteria). */
@QuarkusTest
class OpsEndpointsTest {

    @Test
    void healthIsUpIncludingDatabase() {
        given().when()
                .get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("Reactive PostgreSQL connections health check"));
    }

    @Test
    void openApiHasApplicationInfo() {
        given().accept("application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("info.title", equalTo("market-pulse API"))
                .body("info.version", equalTo("0.1.0-SNAPSHOT"))
                .body("info.description", not(emptyOrNullString()));
    }
}
