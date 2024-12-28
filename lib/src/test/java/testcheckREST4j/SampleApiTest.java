package testcheckREST4j;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class SampleApiTest {

    @Test
    public void testGetPostById() {

        RestAssured.baseURI = "https://jsonplaceholder.typicode.com";

        Response response = given()
                .when()
                .get("/posts/1");

//        response.then()
//                .statusCode(200)
//                .body("id", equalTo(1))
//                .body("userId", equalTo(1))
//                .body("title", notNullValue())
//                .body("body", notNullValue());
//        System.out.println("rsp " + response.body().asString());
//        Assertions.assertTrue(response.body().asString().contains("\"title\": \"sunt aut facere repellat provident occaecati excepturi optio reprehenderit\""));
        Assertions.assertTrue(response.body().asString().contains("\"id\": 1"));
    }
}
