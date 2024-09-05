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


public class APITesting_WCOrder {
    private RequestSpecification mRequestSpec;
    private String mOrderFields = "id,number,status,currency,date_created_gmt,total,total_tax,shipping_total,"
            + "payment_method,payment_method_title,prices_include_tax,customer_note,discount_total,"
            + "coupon_lines,refunds,billing,shipping,line_items,date_paid_gmt,shipping_lines";
    private String mTrackingFields = "tracking_id,tracking_number,tracking_link,tracking_provider,date_shipped";

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
    public void canGetAllOrders() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/orders&per_page=50&offset=0&_fields=" + mOrderFields).
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(50));
    }

    @Test
    public void canGetOrderListSummary() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/orders&per_page=50&offset=0&status=failed&_fields=id,"
                + "date_created_gmt,date_modified_gmt").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(10));
    }

    @Test
    public void canGetOrdersByID() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/orders&per_page=50&include=625,628,618").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(3),
                "data.find { it.id == 618 }.status", equalTo("processing"),
                "data.find { it.id == 625 }.status", equalTo("failed")
            );
    }

    @Test
    public void canGetOrderStatusOptions() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/reports/orders/totals").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(8));
    }

    @Test
    public void canSearchOrders() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/orders&per_page=50&offset=0&status=failed&search=belt&_fields=" + mOrderFields).
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(3),
                "data.id", hasItems(620, 282, 185)
            );
    }

    @Test
    public void canGetSingleOrder() {
        given().
            spec(this.mRequestSpec).
                queryParam("path", "/wc/v3/orders/609&_fields=" + mOrderFields).
        when().
            get().
        then().
            statusCode(200).
            body("data.id", equalTo(609),
                "data.status", equalTo("completed")
            );
    }

    @Test
    public void canGetOrderCount() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/reports/orders/totals&status=processing").
        when().
            get().
        then().
            statusCode(200).
            body("data.find { it.slug == 'processing' }.total", equalTo(12));
    }

    @Test
    public void canGetHasOrders() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/orders&per_page=1&offset=0&status=processing").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(1),
                "data[0].status", equalTo("processing")
            );
    }

    @Test
    public void canUpdateOrderStatus() {
        String path = "/wc/v3/orders/607/";
        String method = "put";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("status", "processing");
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("path", path);
        jsonObj.put("body", jsonBody.toString());
        jsonObj.put("json", "true");
        jsonObj.put("method", method);

        // Set Status to processing.
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

        // Get Status.
        given().
            spec(this.mRequestSpec).
            header("Content-Type", ContentType.JSON).
            queryParam("path", path).
            body(jsonObj.toString()).
        when().
            get().
        then().
            statusCode(200).
            body("data.status", equalTo("processing"));

        // Set status to completed
        jsonBody.put("status", "completed");
        jsonObj.put("body", jsonBody.toString());
        given().
            spec(this.mRequestSpec).
            header("Content-Type", ContentType.JSON).
            queryParam("path", path).
            queryParam("_method", method).
            body(jsonObj.toString()).
        when().
            post().
        then().
            statusCode(200).
            body("data.status", equalTo("completed"));
    }

    @Test
    public void canGetOrderNotes() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/orders/591/notes").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(2),
                "data.find { it.id == 1173 }.note", equalTo("Order status changed from Processing to Completed.")
            );
    }

    @Test
    public void canAddOrderNote() {
        String path = "/wc/v3/orders/565/notes";
        String method = "post";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("note", "Adding note to an order test");
        jsonBody.put("customer_note", false);
        jsonBody.put("added_by_user", true);
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("path", path);
        jsonObj.put("body", jsonBody.toString());
        jsonObj.put("json", "true");
        jsonObj.put("method", method);

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

    @Test
    public void canGetOrderShipmentTrackings() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/orders/635/shipment-trackings&_fields=" + mTrackingFields).
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(1),
                "data[0].tracking_number", equalTo("324567890234567")
            );
    }

    @Test
    public void canGetOrderShipmentProviders() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v3/orders/635/shipment-trackings/providers").
        when().
            get().
        then().
            statusCode(200).
            body("data.'United States (US)'.USPS",
                equalTo("https://tools.usps.com/go/TrackConfirmAction_input?qtc_tLabels1=%number%")
            );
    }

    @Test
    public void canAddAndDeleteShipmentTracking() {
        String path = "/wc/v3/orders/634/shipment-trackings";
        String method = "post";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("tracking_provider", "USPS");
        jsonBody.put("tracking_number", "987654321987654321");
        jsonBody.put("date_shipped", "2020-04-05");
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("path", path);
        jsonObj.put("body", jsonBody.toString());
        jsonObj.put("json", "true");
        jsonObj.put("method", method);

        // Add tracking number
        String tracking = given().
            spec(this.mRequestSpec).
            header("Content-Type", ContentType.JSON).
            queryParam("path", path).
            queryParam("_method", method).
            body(jsonObj.toString()).
        when().
            post().
        then().
            statusCode(200).
        extract().
            path("data.tracking_id");

        // Delete tracking number
        method = "delete";
        path = path + '/' + tracking + "&_method=" + method;
        jsonObj.put("path", path);
        jsonObj.remove("method");
        jsonObj.remove("body");

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
}
