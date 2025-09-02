package ts.andrey.deviceservice.metrics;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RepositoryMetricsAspect {

    private final DeviceMetrics deviceMetrics;

    @AfterThrowing(pointcut = "execution(* ts.andrey.deviceservice.data.repository..*(..))", throwing = "ex")
    public void onDatabaseError(JoinPoint jp, DataAccessException ex) {
        String operation = jp.getSignature().getName();
        deviceMetrics.recordDatabaseError(operation);
    }

}
