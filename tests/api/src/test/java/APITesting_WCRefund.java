import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.builder.RequestSpecBuilder;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.oauth2;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class APITesting_WCRefund {
    private RequestSpecification mRequestSpec;

    @Before
    public void setup() {
        Map<String, String> pathParams = new HashMap<String, String>();
        pathParams.put("json", "true");
        pathParams.put("locale", "en_US");
        pathParams.put("status", "any");

        RequestSpecBuilder requestBuilder = new RequestSpecBuilder().
            setBaseUri("https://public-api.wordpress.com").
            setBasePath("rest/v1.1/jetpack-blogs/173063404/rest-api/").
            addQueryParams(pathParams).
            setAuth(oauth2(System.getenv("API_TEST_OAUTH_KEY")));
        this.mRequestSpec = requestBuilder.build();
    }

    @Test
    public void canGetAllRefunds() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/orders/161/refunds").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(1),
                "data.id", hasItems(638)
            );
    }

    @Test
    public void canGetSingleRefund() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/orders/161/refunds/638").
        when().
            get().
        then().
            statusCode(200).
            body("data.amount", equalTo("118.00"));
    }

    @Test
    public void canCreateRefund() {
        String path = "/wc/v4/orders/156/refunds";
        // Get previous refund
        Integer id = given().
            spec(this.mRequestSpec).
            queryParam("path", path).
        when().
            get().
        then().
            statusCode(200).
        extract().
            path("data[0].id");
        
        // Delete previous refund    
        String method = "delete";        
        JSONObject jsonObj = new JSONObject();      
        jsonObj.put("json", "true");
        jsonObj.put("method", method);

        if (id != null) {
            path = path + "/" + Integer.toString(id) + "&_method=" + method;
            jsonObj.put("path", path); 
            given().
                spec(this.mRequestSpec).
                header("Content-Type", ContentType.JSON).
                queryParam("path", path).
                body(jsonObj.toString()).
            when().
                post().
            then().
               statusCode(200);
        }

        // Add new refund
        method = "post";
        path = "/wc/v4/orders/156/refunds";
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("reason", "API testing");
        jsonBody.put("amount", "142.00");
        jsonBody.put("api_refund", false);
        jsonBody.put("restock_items", false);

        jsonObj.put("body", jsonBody.toString());
        jsonObj.put("method", method);
        jsonObj.put("path", path);

        given().
            spec(this.mRequestSpec).
            header("Content-Type", ContentType.JSON).
            queryParam("path", path).
            queryParam("_method", method).
            body(jsonObj.toString()).
        when().
            post().
        then().
            statusCode(200);
    }
}
