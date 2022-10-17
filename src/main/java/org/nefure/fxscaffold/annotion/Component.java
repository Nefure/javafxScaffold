package org.nefure.fxscaffold.annotion;

import java.lang.annotation.*;

/**
 * @author nefure
 * @date 2022/10/11 0:37
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    String value() default "";
}
