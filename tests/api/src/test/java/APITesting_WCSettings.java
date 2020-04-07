import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.restassured.specification.RequestSpecification;
import io.restassured.builder.RequestSpecBuilder;

//import com.google.gson.Gson;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.oauth2;
import static org.hamcrest.Matchers.hasItems;

public class APITesting_WCSettings {
    private RequestSpecification requestSpec;

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
        this.requestSpec = requestBuilder.build();    
    }

    @Test
    public void canGetGeneralSettings() {
        given().
            spec(this.requestSpec).
            queryParam("path", "/wc/v4/settings/general").
        when().
            get().
        then().
            statusCode(200).
            body("data.id", hasItems("woocommerce_currency", "woocommerce_currency_pos", 
                "woocommerce_price_thousand_sep", "woocommerce_price_decimal_sep",
                "woocommerce_price_num_decimals", "woocommerce_default_country")
            );
    }

    @Test
    public void canGetProductSettings() {
        given().
            spec(this.requestSpec).
            queryParam("path", "/wc/v4/settings/products").
        when().
            get().
        then().
            statusCode(200).
            body("data.id", hasItems("woocommerce_weight_unit", "woocommerce_dimension_unit")
            );
    }
}
