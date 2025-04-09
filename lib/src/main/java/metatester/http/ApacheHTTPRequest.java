package metatester.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the Request interface for Apache HTTP Client.
 * Wraps HttpRequestBase objects and extracts URL, headers and body.
 */
public class ApacheHTTPRequest implements Request {
    private final String url;
    private final Map<String, Object> headers;
    private final String body;

    /**
     * Constructs an ApacheHTTPRequest from an Apache HttpRequestBase
     *
     * @param requestBase the Apache HTTP request object to wrap
     */
    public ApacheHTTPRequest(HttpRequestBase requestBase) {
        this.url = requestBase.getURI().toString();
        this.headers = extractHeaders(requestBase);
        this.body = extractBody(requestBase);
    }

    /**
     * Extracts headers from the HttpRequestBase object
     *
     * @param requestBase the request to extract headers from
     * @return a map of header names to values, or an empty map if no headers exist
     */
    private Map<String, Object> extractHeaders(HttpRequestBase requestBase) {
        Header[] allHeaders = requestBase.getAllHeaders();
        if (allHeaders != null && allHeaders.length > 0) {
            return Arrays.stream(allHeaders)
                    .collect(Collectors.toMap(
                            Header::getName,
                            Header::getValue,
                            // Handle duplicate header names by using the latest value
                            (existing, replacement) -> replacement
                    ));
        }
        return Collections.emptyMap();
    }

    /**
     * Extracts the body from HttpEntityEnclosingRequestBase objects
     *
     * @param requestBase the request to extract the body from
     * @return the request body as a string, or null if no body exists
     */
    private String extractBody(HttpRequestBase requestBase) {
        if (!(requestBase instanceof HttpEntityEnclosingRequestBase)) {
            return null;
        }

        HttpEntity entity = ((HttpEntityEnclosingRequestBase) requestBase).getEntity();
        if (entity == null) {
            return null;
        }

        try {
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            // Consider logging the exception here
            return null;
        }
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public String getBody() {
        return body;
    }
}