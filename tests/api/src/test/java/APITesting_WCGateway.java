import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.restassured.specification.RequestSpecification;
import io.restassured.builder.RequestSpecBuilder;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.oauth2;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
@Ignore("Failing with 400")
public class APITesting_WCGateway {
    private RequestSpecification mRequestSpec;

    @Before
    public void setup() {
        Map<String, String> pathParams = new HashMap<>();
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
    public void canGetAllPaymentGateways() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/payment_gateways").
        when().
            get().
        then().
            statusCode(200).
            body(
                    "data.id",
                    hasItems("bacs", "cheque", "cod", "paypal", "stripe", "stripe_sepa", "stripe_giropay",
                            "stripe_ideal", "stripe_bancontact", "stripe_eps", "stripe_sofort", "stripe_p24",
                            "stripe_boleto", "stripe_oxxo", "stripe_alipay", "stripe_multibanco")
                );
    }

    @Test
    public void canGetSinglePaymentGateway() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/payment_gateways/stripe").
        when().
            get().
        then().
            statusCode(200).
            body("data.id", equalTo("stripe"),
                "data.title", equalTo("Credit Card (Stripe)")
            );
    }
}
