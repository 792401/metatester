package metatester.schemacoverage;

import java.util.List;

public class Details {
    private List<String> headers;
    private List<String> bodyFields;
    private List<String> urlParameters;

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getBodyFields() {
        return bodyFields;
    }

    public void setBodyFields(List<String> bodyFields) {
        this.bodyFields = bodyFields;
    }

    public List<String> getUrlParameters() {
        return urlParameters;
    }

    public void setUrlParameters(List<String> urlParameters) {
        this.urlParameters = urlParameters;
    }
}
