import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

public class OrderServiceIntegrationTests {


    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    @Test
    public void putOrderWithCreatedAndResponseBody(){

        String payload = """
                {
                    "email":"kkr@gmail.com",
                    "password":"pass"
                }
                """;

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token",notNullValue())
                .extract().response();

        String payload1= """
                 {
                 "name": "Wireless Keyboard",
                                "description": "Compact Bluetooth keyboard with rechargeable battery",
                                "price": 49.99,
                                "quantity": 100
                                }
             """;

      Response response1=  RestAssured.given()
                .header("Authorization", "Bearer "+response.jsonPath().get("token"))
                .contentType(ContentType.JSON)
                .body(payload1)
                .when()
                .post("/auth/products/add")
                .then()
                .statusCode(201)
                .body(notNullValue())
              .extract().response();

      String productId= response1.jsonPath().getString("productId");
      String name= response1.jsonPath().getString("name");

        String payload2 = String.format("""
                {
                  "customerEmail": "john.doe@example.com",
                  "customerPhone": "+1234567890",
                  "totalAmount": 249.00,
                  "currency": "INR",
                  "items": [
                    {
                      "productId": "%s",
                      "productName": "%s",
                      "quantity": 3,
                      "price": 1999.0
                    }
                  ]
                }
                """, productId,name);

        Response response2 = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer "+response.jsonPath().get("token"))
                .body(payload2)
                .when()
                .post("/auth/api/orders")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .extract().response();


    }

    @Test
    public void getOrderWithOkStatusAndResponseBody(){

        String payload = """
                {
                    "email":"kkr@gmail.com",
                    "password":"pass"
                }
                """;

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token",notNullValue())
                .extract().response();

        String payload1= """
                 {
                 "name": "Wireless Keyboard",
                                "description": "Compact Bluetooth keyboard with rechargeable battery",
                                "price": 49.99,
                                "quantity": 100
                                }
             """;

        Response response1=  RestAssured.given()
                .header("Authorization", "Bearer "+response.jsonPath().get("token"))
                .contentType(ContentType.JSON)
                .body(payload1)
                .when()
                .post("/auth/products/add")
                .then()
                .statusCode(201)
                .body(notNullValue())
                .extract().response();

        String productId= response1.jsonPath().getString("productId");
        String name= response1.jsonPath().getString("name");

        String payload2 = String.format("""
                {
                  "customerEmail": "john.doe@example.com",
                  "customerPhone": "+1234567890",
                  "totalAmount": 249.00,
                  "currency": "INR",
                  "items": [
                    {
                      "productId": "%s",
                      "productName": "%s",
                      "quantity": 3,
                      "price": 1999.0
                    }
                  ]
                }
                """, productId,name);

        Response response2 = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer "+response.jsonPath().get("token"))
                .body(payload2)
                .when()
                .post("/auth/api/orders")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .extract().response();

String orderId= response2.jsonPath().getString("id");

        RestAssured.given()
                .header("Authorization","Bearer "+response.jsonPath().get("token"))
                .when()
                .get(String.format("/auth/api/orders/%s",orderId))
                .then()
                .statusCode(200)
                .body(notNullValue());

    }

}
