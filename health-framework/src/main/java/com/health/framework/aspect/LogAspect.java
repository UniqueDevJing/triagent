package com.health.framework.aspect;

import com.health.common.annotation.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogAspect {
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint point, Log logAnnotation) throws Throwable {
        log.info("[{}] 开始执行 - {}", logAnnotation.title(), point.getSignature().toShortString());
        long start = System.currentTimeMillis();
        try {
            Object result = point.proceed();
            log.info("[{}] 执行完成 - 耗时: {}ms", logAnnotation.title(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[{}] 执行异常 - {}", logAnnotation.title(), e.getMessage());
            throw e;
        }
    }
}
