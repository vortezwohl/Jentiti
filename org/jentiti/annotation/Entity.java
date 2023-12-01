package org.jentiti.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Documented
public @interface Entity {

    String value() default ""; // entity name

    String scope() default "prototype"; // prototype or singleton
}
