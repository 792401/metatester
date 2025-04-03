package metatester.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;

public class ApacheHTTPResponse implements Response {

    String url;
    Map<String, Object> headers;
    String body;
    int statusCode;

    Map<String, Object> responseAsMap;


    public ApacheHTTPResponse(HttpResponse response) throws IOException {
        this.body = EntityUtils.toString(response.getEntity());
        setResponseAsMap(this.body);
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return null;
    }

    @Override
    public String getBody() {
        return this.body;
    }

    @Override
    public Map<String, Object> getResponseAsMap() {
        return responseAsMap;
    }

    public void setResponseAsMap(String response) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode rootNode = objectMapper.readTree(response);
        if(rootNode.isArray()){
            //todo
        }
        if(rootNode.isObject()){
            responseAsMap = objectMapper.readValue(response, new TypeReference<>() {});
        }
    }
}
