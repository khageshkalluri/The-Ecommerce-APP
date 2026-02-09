package org.ecomm.productservice.AOP;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class CachMissLoggin {

    private MeterRegistry meterRegistry;

    public CachMissLoggin(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("execution(* org.ecomm.productservice.Service.ProductService.getAllProducts(..))")
    public Object handleCacheMissAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Cache miss aspect");
        this.meterRegistry.counter("custom.redis.cache.miss","cache","products").increment();
        Object proceed = joinPoint.proceed();
        return proceed;
    }
}
