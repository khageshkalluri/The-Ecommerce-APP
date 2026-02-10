import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

public class ProductServiceIntegrationTests {


    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    @Test
    public void shouldReturnOkWithResponse() {

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

        RestAssured.given()
                .header("Authorization", "Bearer "+response.jsonPath().get("token"))
                .when()
                .get("/auth/products")
                .then()
                .statusCode(200)
                .body(notNullValue());

    }

    @Test
    public void addProductsshouldReturnOkWithBody() {
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

        RestAssured.given()
                .header("Authorization", "Bearer "+response.jsonPath().get("token"))
                .contentType(ContentType.JSON)
                .body(payload1)
                .when()
                .post("/auth/products/add")
                .then()
                .statusCode(201)
                .body(notNullValue());

    }


    @Test
    public void searchProductshouldReturnOkWithBody() {

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

        RestAssured.given()
                .header("Authorization", "Bearer "+response.jsonPath().get("token"))
                .contentType(ContentType.JSON)
                .body(payload1)
                .when()
                .post("/auth/products/add")
                .then()
                .statusCode(201)
                .body(notNullValue());

        RestAssured.given()
                .header("Authorization", "Bearer "+response.jsonPath().get("token"))
                .when()
                .get("/auth/products/search?name=Wireless")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

}
