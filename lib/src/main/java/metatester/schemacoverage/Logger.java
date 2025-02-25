package metatester.schemacoverage;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Logger {


    public static void parseResponse(HttpRequestBase httpRequestBase) {

        CollectorData collectorData = Collector.getData();

        String method = httpRequestBase.getMethod() != null ? httpRequestBase.getMethod().toUpperCase() : "UNKNOWN";

        try {
            String requestUri = httpRequestBase.getURI() != null ? httpRequestBase.getURI().toString() : "";
            URI uriObj = new URI(requestUri);
            String endpoint = uriObj.getPath();

            String baseUri = httpRequestBase.getURI() != null ? httpRequestBase.getURI().getHost() : "";
            URI baseUriObj = new URI(baseUri);
            String basePath = baseUriObj.getPath();

            if (endpoint.startsWith(basePath)) {
                endpoint = endpoint.substring(basePath.length());
                if (endpoint.isEmpty()) {
                    endpoint = "/";
                }
            }

            if (collectorData.getDateCollected() == null) {
                collectorData.setDateCollected(String.valueOf(System.currentTimeMillis()));
                collectorData.setHost(baseUriObj.getScheme() + "://" + baseUriObj.getHost());
            }

            Map<String, Map<String, EndpointCall>> paths = collectorData.getPaths();
            Map<String, EndpointCall> methodsMap = paths.computeIfAbsent(endpoint, k -> new HashMap<>());


            Map<String, String> headers = new HashMap<>();
            if (httpRequestBase.getAllHeaders() != null) {
                Arrays.stream(httpRequestBase.getAllHeaders()).forEach(header -> {
                    if (header != null && header.getName() != null && header.getValue() != null) {
                        headers.put(header.getName(), header.getValue());
                    }
                });
            }

            Object body = getRequestBody(httpRequestBase);

            Map<String, String> urlParams = getQueryParams(httpRequestBase);

            EndpointCall endpointCall = new EndpointCall();
            endpointCall.setHeaders(headers);
            endpointCall.setBody(body);
            endpointCall.setUrlParameters(urlParams);

            methodsMap.put(method, endpointCall);

            FileUtils.saveToJsonFile("schema_coverage.json");

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static String getRequestBody(HttpRequestBase request) throws IOException {
        if (request instanceof org.apache.http.client.methods.HttpEntityEnclosingRequestBase) {
            org.apache.http.client.methods.HttpEntityEnclosingRequestBase entityRequest = (org.apache.http.client.methods.HttpEntityEnclosingRequestBase) request;
            HttpEntity entity = entityRequest.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity);
            }
        }
        return null;
    }

    public static Map<String, String> getQueryParams(HttpRequestBase request) throws URISyntaxException {
        Map<String, String> queryParams = new HashMap<>();
        URI uri = request.getURI();
        String query = uri.getQuery();

        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    queryParams.put(pair.substring(0, idx), pair.substring(idx + 1));
                } else if (idx == 0){
                    queryParams.put("", pair.substring(idx + 1));
                } else {
                    queryParams.put(pair, "");
                }
            }
        }
        return queryParams;
    }

}