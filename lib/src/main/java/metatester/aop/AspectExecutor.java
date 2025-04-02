package metatester.aop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import metatester.config.SimulatorConfig;
import metatester.runner.Workflow;
import metatester.schemacoverage.Logger;
import okhttp3.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

@Aspect
public class AspectExecutor {

    Workflow workflow = Workflow.getInstance();

    @Around("execution(@org.junit.jupiter.api.Test * *(..))")
    public Object interceptTestMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepting test method: " + joinPoint.getSignature());

        System.out.println("Executing test...");
        Object result = joinPoint.proceed();
        if (workflow.isFirstRun()) {
            if (workflow.getOriginalResponse() == null) {
                throw new IllegalStateException("Original response was not captured. Ensure response interceptors are working.");
            }
            workflow.setFirstRun(false);
            System.out.println("First run completed. Original response captured.");
        }

        if(!SimulatorConfig.isTestExcluded(joinPoint.getSignature().getName())){
            workflow.executeTestWithSimulatedFaults(joinPoint);
            workflow.setFirstRun(true); //reset flag after execution test
        }

        return result;
    }

    @Around("execution(* org.apache.http.impl.client.CloseableHttpClient.execute(..))")
    public Object interceptApacheHttpClient(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        //request
        if (args.length > 0 && args[0] instanceof HttpRequestBase) {
            HttpRequestBase originalRequest = (HttpRequestBase) args[0];
            workflow.setInterceptedUrl(originalRequest.getURI().toString());
            originalRequest.getAllHeaders();
            System.out.println("Intercepted URL: " + workflow.getInterceptedUrl());
            if(workflow.isFirstRun()){
                Logger.parseResponse(originalRequest);
            } else {
                URI originalUri = new URI(workflow.getInterceptedUrl());
                URI redirectedUri = new URI("http", null, "localhost", 8080, originalUri.getPath(), originalUri.getQuery(), null);
                System.out.println("Redirecting request to: " + redirectedUri);

                HttpGet newRequest = new HttpGet(redirectedUri);
                newRequest.setHeaders(originalRequest.getAllHeaders());

                args[0] = newRequest;
            }
        }

        Object result = joinPoint.proceed(args);
        //response
        if (result instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) result;
            if (workflow.isFirstRun()) {

                workflow.setOriginalResponse(EntityUtils.toString(response.getEntity()));
//                if (originalResponse == null) {
//                    throw new IllegalStateException("Original response was not captured");
//                }

                ObjectMapper objectMapper = new ObjectMapper();

                JsonNode rootNode = objectMapper.readTree(workflow.getOriginalResponse());
                if(rootNode.isArray()){
                  //todo
                }
                if(rootNode.isObject()){
                    workflow.setResponseMap(objectMapper.readValue(workflow.getOriginalResponse(), new TypeReference<>() {}));
                }
                response.setEntity(new StringEntity(workflow.getOriginalResponse()));

            } else {
                System.out.println("Rerun response intercepted (simulated fault applied).");
            }
        }

        return result;
    }

    @Around("execution(* okhttp3.Call.execute(..))")
    public Object interceptOkHttpClient(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepted OkHttpClient call");
        Object result = joinPoint.proceed();

        if (result instanceof Response) {
            Response response = (Response) result;

            String originalResponse = response.peekBody(Long.MAX_VALUE).string();
            System.out.println("Response intercepted for OkHttpClient: " + originalResponse);

            return response;
        }

        return result;
    }

    @Around("execution(* java.net.HttpURLConnection.connect(..))")
    public Object interceptHttpURLConnection(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepted HttpURLConnection call");
        HttpURLConnection connection = (HttpURLConnection) joinPoint.getTarget();
        Object result = joinPoint.proceed();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            String originalResponse = responseBuilder.toString();
            System.out.println("Response intercepted for HttpURLConnection: " + originalResponse);
        } catch (Exception e) {
            System.out.println("Unable to read response for HttpURLConnection: " + e.getMessage());
        }

        return result;
    }








}
