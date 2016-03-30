package org.wordpress.android.stores.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
public @interface ActionEnum {
    String name() default "";
}