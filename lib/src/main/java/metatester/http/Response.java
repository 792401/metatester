package metatester.http;

import java.util.Map;

public interface Response {

    String getUrl();
    Map<String, Object> getHeaders();
    String getBody();
    Map<String, Object> getResponseAsMap();

}
