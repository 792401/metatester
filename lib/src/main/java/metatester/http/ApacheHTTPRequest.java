package metatester.http;

import org.apache.http.client.methods.HttpRequestBase;

import java.util.Map;

public class ApacheHTTPRequest implements Request {

    String url;
    Map<String, Object> headers;
    String body;
    int statusCode;

    public ApacheHTTPRequest(HttpRequestBase requestBase) {
        this.url = requestBase.getURI().toString();
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return null;
    }

    @Override
    public String getResponse() {
        return null;
    }
}
