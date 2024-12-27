package aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;

@Aspect
public class JUnitInterceptor {

    @Around("execution(@org.junit.jupiter.api.Test * *(..))")
    public Object aroundTestExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepting test method: " + joinPoint.getSignature());

        System.out.println("Executing the test with original response...");
        Object result = joinPoint.proceed();

        System.out.println("Re-running the test with modified response...");
        result = joinPoint.proceed();

        return result;
    }
}
