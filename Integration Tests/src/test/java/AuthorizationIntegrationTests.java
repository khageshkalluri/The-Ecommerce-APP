import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;

public class AuthorizationIntegrationTests {

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

    }



    @Test
    public void shouldReturnUnauthorizedWithNoResponse() {

        String payload = """
                {
                    "email":"kkr@gmail.com",
                    "password":"notpassword"
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }
}
