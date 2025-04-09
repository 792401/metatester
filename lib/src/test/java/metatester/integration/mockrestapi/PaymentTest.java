package metatester.integration.mockrestapi;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class PaymentTest {

        @BeforeAll
        public static void setup() {
            RestAssured.baseURI = "http://localhost:8089";
        }

        @Test
        public void testCreatePayment() {
            String requestBody = "{\"amount\": 100.00, \"currency\": \"USD\", \"orderId\": \"order-456\"}";

            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/payments");

            Assertions.assertEquals(201, response.getStatusCode(), "Check status code");
            Assertions.assertTrue(response.body().asString().contains("paymentId"), "check paymentId field");
            Assertions.assertTrue(response.body().asString().contains("PENDING"), "check status field");
        }

        @Test
        public void testGetPayment() {
            String paymentId = "payment-123";

            Response response = given()
                    .when()
                    .get("/payments/" + paymentId);

            Assertions.assertEquals(200, response.getStatusCode(), "Check status code");
            Assertions.assertTrue(response.body().asString().contains(paymentId), "check paymentId field");
            Assertions.assertTrue(response.body().asString().contains("COMPLETED"), "check status field");
            Assertions.assertTrue(response.body().asString().contains("amount"), "check amount field");
        }

        @Test
        @Disabled
        public void testGetPaymentNotFound() {
            String invalidPaymentId = "nonexistent-payment";

            Response response = given()
                    .when()
                    .get("/payments/" + invalidPaymentId);

            Assertions.assertEquals(404, response.getStatusCode(), "Check status code");
            Assertions.assertTrue(response.body().asString().contains("Payment not found"), "check error message");
        }

    }
