package metatester.schemacoverage;
import java.util.Map;

public class EndpointCall {
    private Map<String, String> headers;
    private Object body;
    private Map<String, String> urlParameters;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public Map<String, String> getUrlParameters() {
        return urlParameters;
    }

    public void setUrlParameters(Map<String, String > urlParameters) {
        this.urlParameters = urlParameters;
    }
}
