package com.affiliate.rentals.properties.crosscutting;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Around("execution(* com.gydi.properties.adapters.in.rest..*(..)) || execution(* com.gydi.properties.application.usecase..*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        log.info("Entrando: {} - args={}", pjp.getSignature(), pjp.getArgs());
        try {
            Object result = pjp.proceed();
            log.info("Saliendo: {} - result={}", pjp.getSignature(), result);
            return result;
        } catch (Throwable t) {
            log.error("Error en {}: {}", pjp.getSignature(), t.toString(), t);
            throw t;
        }
    }
}
