package org.vitals.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AsyncHealthCheck {

    long period();

    ScheduleType scheduleType() default ScheduleType.FIXED_RATE;

    long initialDelay() default 0L;

    TimeUnit unit() default TimeUnit.SECONDS;

    long healthyTtl() default Long.MAX_VALUE;

    enum ScheduleType {
        FIXED_RATE, FIXED_DELAY
    }
}
