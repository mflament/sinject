package org.yah.sinject.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Service {

    String value() default "";

    String name() default "";

    int priority() default 0;

    int TOP_PRIORITY = Integer.MIN_VALUE;
    int LOWEST_PRIORITY = Integer.MAX_VALUE;
}
