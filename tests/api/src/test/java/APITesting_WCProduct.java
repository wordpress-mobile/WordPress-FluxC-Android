import org.json.JSONArray;
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

public class APITesting_WCProduct {
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
    public void canGetAllProducts() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/products&per_page=50&offset=0").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(18));
    }

    @Test
    public void canGetSingleProduct() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/products/12").
        when().
            get().
        then().
            statusCode(200).
            body("data.name", equalTo("V-Neck T-Shirt"),
                 "data.categories.name", hasItems("Tshirts"),
                 "data.tags.name", hasItems("shirts"),
                 "data.images.name", hasItems("vneck-tee-2.jpg"),
                 "data.attributes.name", hasItems("Color", "Size"),
                 "data.attributes.find { it.name == 'Size' }.options", hasItems("Large", "Medium", "Small"),
                 "data.attributes.find { it.name == 'Color' }.options", hasItems("Blue", "Green", "Red"),
                 "data.variations", hasSize(3)    
            );
    }

    @Test
    public void canGetProductVariations() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/products/12/variations").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(3),
                 "data.id", hasItems(28, 27, 26)
            );
    }

    @Test
    public void canGetProductSkuAvailability() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/products/&_fields=sku&sku=woo-belt").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(1)
            );
    }

    @Test
    public void canGetProductShippingClasses() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/products/shipping_classes").
        when().
            get().
        then().
            statusCode(200).
            body("data", hasSize(2),
                 "data.name", hasItems("hats", "shirts")
            );
    }
    
    @Test
    public void canGetProductShippingClassbyID() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/products/shipping_classes/35").
        when().
            get().
        then().
            statusCode(200).
            body("data.name", equalTo("hats"));
    }

    @Test
    public void canGetProductReviews() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/products/reviews").
        when().
            get().
        then().
            statusCode(200).
            body("data.findAll { it.reviewer == 'WooTester' }", hasSize(2),
                 "data.status", hasItems("approved")
            );
    }

    @Test
    public void canGetSingleProductReview() {
        given().
            spec(this.mRequestSpec).
            queryParam("path", "/wc/v4/products/reviews/1088").
        when().
            get().
        then().
            statusCode(200).
            body("data.rating", equalTo(3),
                 "data.product_id", equalTo(33),
                 "data.reviewer", equalTo("JoeTester")
            );
    }

    @Test
    public void canUpdateProductReviewStatus() {
        String path = "/wc/v4/products/reviews/1088/";
        String method = "put";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("status", "approved");
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("path", path);
        jsonObj.put("body", jsonBody.toString());
        jsonObj.put("json", "true");
        jsonObj.put("method", method);

        // Set Status to approved.
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
            body("data.status", equalTo("approved"));

        // Set status to hold
        jsonBody.put("status", "hold");
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
            body("data.status", equalTo("hold"));
    }
   
    @Test
    public void canUpdateProductImages() {
        String path = "/wc/v4/products/13";
        String method = "put";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("images", new JSONArray());
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("body", jsonBody.toString());
        jsonObj.put("path", path);
        jsonObj.put("method", method);
        jsonObj.put("json", "true"); 

        // Set Images to none.
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

        // Make sure there are no images now.
        given().
            spec(this.mRequestSpec).
            header("Content-Type", ContentType.JSON).
            queryParam("path", path).
            body(jsonObj.toString()).
        when().
            get().
        then().
            statusCode(200).
            body("data.images", hasSize(0));

        // Add images back on
        JSONArray jsonImagesArray = new JSONArray();
        JSONObject jsonImage = new JSONObject();
        jsonImage.put("id", 40);
        jsonImage.put("date_created", "2020-02-18T20:45:45");
        jsonImage.put("date_created_gmt", "2020-02-18T20:45:45");    
        jsonImage.put("date_modified", "2020-02-18T20:45:45");  
        jsonImage.put("date_modified_gmt", "2020-02-18T20:45:45");  
        jsonImage.put("src", "https://woomobileapitesting.mystagingwebsite.com/wp-content"
            + "/uploads/2020/02/hoodie-2.jpg");  
        jsonImage.put("name", "hoodie-2.jpg");
        jsonImage.put("alt", "");
        jsonImagesArray.put(0, jsonImage);

        jsonImage = new JSONObject();
        jsonImage.put("id", 41);
        jsonImage.put("date_created", "2020-02-18T20:45:45");
        jsonImage.put("date_created_gmt", "2020-02-18T20:45:45");    
        jsonImage.put("date_modified", "2020-02-18T20:45:45");  
        jsonImage.put("date_modified_gmt", "2020-02-18T20:45:45");  
        jsonImage.put("src", "https://woomobileapitesting.mystagingwebsite.com/wp-content"
            + "/uploads/2020/02/hoodie-blue-1.jpg");  
        jsonImage.put("name", "hoodie-blue-1.jpg");
        jsonImage.put("alt", "");
        jsonImagesArray.put(1, jsonImage);

        jsonImage = new JSONObject();
        jsonImage.put("id", 42);
        jsonImage.put("date_created", "2020-02-18T20:45:45");
        jsonImage.put("date_created_gmt", "2020-02-18T20:45:45");    
        jsonImage.put("date_modified", "2020-02-18T20:45:45");  
        jsonImage.put("date_modified_gmt", "2020-02-18T20:45:45");  
        jsonImage.put("src", "https://woomobileapitesting.mystagingwebsite.com/wp-content"
            + "/uploads/2020/02/hoodie-green-1.jpg");  
        jsonImage.put("name", "hoodie-green-1.jpg");
        jsonImage.put("alt", "");
        jsonImagesArray.put(2, jsonImage);

        jsonImage = new JSONObject();
        jsonImage.put("id", 43);
        jsonImage.put("date_created", "2020-02-18T20:45:45");
        jsonImage.put("date_created_gmt", "2020-02-18T20:45:45");    
        jsonImage.put("date_modified", "2020-02-18T20:45:45");  
        jsonImage.put("date_modified_gmt", "2020-02-18T20:45:45");  
        jsonImage.put("src", "https://woomobileapitesting.mystagingwebsite.com/wp-content"
            + "/uploads/2020/02/hoodie-with-logo-2.jpg");  
        jsonImage.put("name", "hoodie-with-logo-2.jpg");
        jsonImage.put("alt", "");
        jsonImagesArray.put(3, jsonImage);

        jsonBody.put("images", jsonImagesArray);
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
            body("data.images", hasSize(4));
    }
}
