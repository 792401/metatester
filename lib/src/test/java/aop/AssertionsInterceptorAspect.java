package aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;

@Aspect
public class AssertionsInterceptorAspect {

    @Around("call(public static * org.junit.jupiter.api.Assertions.*(..))")
    public Object aroundAssertionMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        logBefore(joinPoint);
        Object result = joinPoint.proceed();
        logAfter(joinPoint);

        return result;
    }

    private void logBefore(ProceedingJoinPoint joinPoint) {
        System.out.println("[AssertionsInterceptor] Entering method: " + joinPoint.getSignature());
        System.out.println("[AssertionsInterceptor] Arguments: " + formatArguments(joinPoint.getArgs()));
    }

    private void logAfter(ProceedingJoinPoint joinPoint) {
        System.out.println("[AssertionsInterceptor] Exiting method: " + joinPoint.getSignature());
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "No arguments";
        }

        StringBuilder formattedArgs = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            formattedArgs.append("arg[").append(i).append("]=").append(args[i]);
            if (i < args.length - 1) {
                formattedArgs.append(", ");
            }
        }
        return formattedArgs.toString();
    }
}
