package metatester.integration.mockrestapi;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class UsersTest {

    @BeforeAll
    public static void beforeAll(){
        RestAssured.baseURI = "http://localhost:8089";
    }

//    @Test
//    public void testGetUsers() {
//        Response response = given()
//                .when()
//                .get("/users");
//
//        Assertions.assertTrue(response.body().asString().contains("name"),"check name field");
//    }

    @Test
    public void testGetUser() {
        Response response = given()
                .when()
                .get("/users/1");

        Assertions.assertTrue(response.body().asString().contains("name"),"check name field");
    }

    @Test
    public void testPostUsers() {
        String requestBody = "{\"name\": \"Simulated New User\", \"email\": \"simulated.new.user@example.com\"}";

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/users");


        Assertions.assertTrue(response.body().asString().contains("email"),"check title field");
    }


}
