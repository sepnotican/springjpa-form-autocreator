package agi.core.annotations;

import agi.core.form.generic.AbstractElementForm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * annotation uses in {@link AbstractElementForm}
 * when creating fields
 * to define to specific coordinates in the grid
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface AgiDrawOrder {
    int value() default 0;

}
