package coo.cfapps.mds.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class SchemaContextHolder {

    @Around("@annotation(coo.cfapps.mds.config.SchemaAware) || @within(coo.cfapps.mds.config.SchemaAware)")
    public Object maintainSchemaContext(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Setting schema context for user: {}", auth.getName());
        if (auth.isAuthenticated()) {
            TenantRoutingDataSource.setCurrentUser(auth.getName());
            try {
                log.info("execute the join point with current schema context set");
                return joinPoint.proceed();
            } finally {
                log.info("execute the join point ended");
                TenantRoutingDataSource.clearCurrentUser();
            }
        }
        return joinPoint.proceed();
    }
}
