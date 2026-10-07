package com.mfa.marketpulse.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProblemExceptionMappersTest {

    @Test
    void invalidBodyIsBadRequestWithViolations() {
        given().contentType(ContentType.JSON)
                .body("{\"symbol\":\"\",\"quantity\":-1}")
                .when()
                .post("/test/problems/validated")
                .then()
                .statusCode(400)
                .contentType(startsWith(ProblemDetail.MEDIA_TYPE))
                .body("type", equalTo("about:blank"))
                .body("title", equalTo("Bad Request"))
                .body("status", equalTo(400))
                .body("instance", equalTo("/test/problems/validated"))
                .body("violations.field", containsInAnyOrder("quantity", "symbol"));
    }

    @Test
    void invalidQueryParamNamesTheParameter() {
        given().queryParam("limit", 1000)
                .when()
                .get("/test/problems/query")
                .then()
                .statusCode(400)
                .contentType(startsWith(ProblemDetail.MEDIA_TYPE))
                .body("violations.field", containsInAnyOrder("limit"));
    }

    @Test
    void invalidResponseIsServerError() {
        given().when()
                .get("/test/problems/invalid-response")
                .then()
                .statusCode(500)
                .contentType(startsWith(ProblemDetail.MEDIA_TYPE))
                .body("violations", nullValue());
    }

    @Test
    void webApplicationExceptionKeepsItsStatus() {
        given().when()
                .get("/test/problems/missing")
                .then()
                .statusCode(404)
                .contentType(startsWith(ProblemDetail.MEDIA_TYPE))
                .body("title", equalTo("Not Found"))
                .body("detail", equalTo("Portfolio 42 not found"))
                .body("violations", nullValue());
    }

    @Test
    void unknownPathIsProblemJson() {
        given().when()
                .get("/no/such/path")
                .then()
                .statusCode(404)
                .contentType(startsWith(ProblemDetail.MEDIA_TYPE))
                .body("status", equalTo(404));
    }

    @Test
    void unexpectedErrorHidesInternals() {
        given().when()
                .get("/test/problems/boom")
                .then()
                .statusCode(500)
                .contentType(startsWith(ProblemDetail.MEDIA_TYPE))
                .body("title", equalTo("Internal Server Error"))
                .body("detail", startsWith("Unexpected error, reference "))
                .body(not(containsString("secret internal detail")))
                .body(not(containsString("IllegalStateException")))
                .body(not(containsString("at com.mfa")));
    }
}
