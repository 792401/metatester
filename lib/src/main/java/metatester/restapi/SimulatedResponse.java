package metatester.restapi;

import java.util.Map;

public class SimulatedResponse {

    String url;
    Map<String, String> headers;
    String statusCode;

    String body;


    public SimulatedResponse(String url, Map<String, String> headers, String statusCode) {
        this.url = url;
        this.headers = headers;
        this.statusCode = statusCode;
    }

    public SimulatedResponse(){};

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
