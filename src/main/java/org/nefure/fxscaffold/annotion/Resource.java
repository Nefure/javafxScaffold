package org.nefure.fxscaffold.annotion;

import java.lang.annotation.*;

/**
 * @author nefure
 * @date 2022/10/11 11:36
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resource {
    String value() default "";
}
