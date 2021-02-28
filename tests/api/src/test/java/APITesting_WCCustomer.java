import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.oauth2;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class APITesting_WCCustomer {
    private RequestSpecification mRequestSpec;

    @Before
    public void setup() {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("json", "true");
        pathParams.put("locale", "en_US");
        pathParams.put("status", "any");

        RequestSpecBuilder requestBuilder = new RequestSpecBuilder()
                .setBaseUri("https://public-api.wordpress.com")
                .setBasePath("rest/v1.1/jetpack-blogs/173063404/rest-api/")
                .addQueryParams(pathParams)
                .setAuth(oauth2(System.getenv("API_TEST_OAUTH_KEY")));
        this.mRequestSpec = requestBuilder.build();
    }

    @Test
    public void canGetSingleCustomer() {
        given()
                .spec(this.mRequestSpec)
                .queryParam("path", "/wc/v3/customers/1")
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("data", hasSize(50));
    }

    @Test
    public void canGetCustomers() {
        given()
                .spec(this.mRequestSpec)
                .queryParam("path", "/wc/v3/customers")
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("data", hasSize(50));
    }

    @Test
    public void canCreateCustomer() {
        String path = "/wc/v3/customers";

        Integer id = given()
                .spec(this.mRequestSpec)
                .queryParam("path", path)
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .path("data[0].id");

        // Delete previous customer
        if (id != null) {
            given()
                    .spec(this.mRequestSpec)
                    .queryParam("path", path)
                    .queryParam("force", true)
                    .when()
                    .delete()
                    .then()
                    .statusCode(200);
        }

        // New Customer
        JSONObject customerJson = new JSONObject();
        customerJson.put("email", "john.doe@example.com");
        customerJson.put("first_name", "John");
        customerJson.put("last_name", "Doe");
        customerJson.put("username", "john.doe");
        given()
                .spec(this.mRequestSpec)
                .header("Content-Type", ContentType.JSON)
                .queryParam("path", path)
                .body(customerJson.toString())
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(
                        "data.email", equalTo("john.doe@example.com"),
                        "data.first_name", equalTo("John"),
                        "data.last_name", equalTo("Doe"),
                        "data.username", equalTo("john.doe")
                     );
    }
}
