package metatester.http;

import java.util.Map;

public interface Response {

    String getUrl();
    Map<String, Object> getHeaders();
    String getBody();

    void setBody(String body);

    Map<String, Object> getResponseAsMap();

}
