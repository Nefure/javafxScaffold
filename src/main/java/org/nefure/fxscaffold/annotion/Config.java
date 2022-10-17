package org.nefure.fxscaffold.annotion;

import java.lang.annotation.*;

/**
 * @author nefure
 * @date 2022/10/10 20:22
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {

    /**
     * 前缀
     */
    String value() default  "";

}
