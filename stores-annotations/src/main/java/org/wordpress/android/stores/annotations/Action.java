package org.wordpress.android.stores.annotations;

import org.wordpress.android.stores.annotations.action.NoPayload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
public @interface Action {
    Class payloadType() default NoPayload.class;
}
