import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.oauth2;
import static org.hamcrest.Matchers.equalTo;

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
                .body("data.id", equalTo(1),
                        "data.first_name", equalTo(""),
                        "data.last_name", equalTo(""),
                        "data.avatar_url",
                        equalTo("https://secure.gravatar.com/avatar/fb409f96c4ccb689c8b1d42342dae3c7?s=96&d=mm&r=g")
                     );
    }
}
