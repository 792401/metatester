package metatester.aop;

import metatester.config.SimulatorConfig;
import metatester.runner.Runner;
import metatester.schemacoverage.Logger;
import okhttp3.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

@Aspect
public class AspectExecutor {

    Runner runner = Runner.getInstance();

    @Around("execution(@org.junit.jupiter.api.Test * *(..))")
    public Object interceptTestMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepting test method: " + joinPoint.getSignature());

        System.out.println("Executing test...");
        Object result = joinPoint.proceed();
        if (runner.isFirstRun()) {
            if (runner.getOriginalResponse() == null) {
                throw new IllegalStateException("Original response was not captured. Ensure response interceptors are working.");
            }
            runner.setFirstRun(false);
            System.out.println("First run completed. Original response captured.");
        }

        if(!SimulatorConfig.isTestExcluded(joinPoint.getSignature().getName())
        && !SimulatorConfig.isEndpointExcluded(runner.getInterceptedUrl())){
                runner.executeTestWithSimulatedFaults(joinPoint);
                runner.setFirstRun(true); //reset flag after execution test
        }

        return result;
    }

    @Around("execution(* org.apache.http.impl.client.CloseableHttpClient.execute(..))")
    public Object interceptApacheHttpClient(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        //request
        if (args.length > 0 && args[0] instanceof HttpRequestBase) {
            HttpRequestBase originalRequest = (HttpRequestBase) args[0];
            runner.setOriginalRequest(originalRequest);

//            System.out.println("Intercepted URL: " + runner.getOriginalRequest().getUrl());
            if(runner.isFirstRun()){
                Logger.parseResponse(originalRequest);
            } else {
//                URI originalUri = new URI(runner.getOriginalRequest().getUrl());
//                URI redirectedUri = new URI("http", null, "localhost", 8080, originalUri.getPath(), originalUri.getQuery(), null);
//                System.out.println("Redirecting request to: " + redirectedUri);
//
//                HttpGet newRequest = new HttpGet(redirectedUri);
//                newRequest.setHeaders(originalRequest.getAllHeaders());
//
//                args[0] = newRequest;
            }
        }

        Object result = joinPoint.proceed(args);
        //response
        if (result instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) result;
            if (runner.isFirstRun()) {
                runner.setOriginalResponse(response);

                response.setEntity(new StringEntity(runner.getOriginalResponse().getBody()));
            } else {
                //run simulations
                response.setEntity(new StringEntity(runner.getSimulatedResponse().getBody()));
                System.out.println("Rerun response intercepted (simulated fault applied).");
                System.out.println(runner.getSimulatedResponse().getBody());
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
