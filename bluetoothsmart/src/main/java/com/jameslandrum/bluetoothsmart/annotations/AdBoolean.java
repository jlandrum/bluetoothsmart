package com.jameslandrum.bluetoothsmart.annotations;

import com.jameslandrum.bluetoothsmart.processors.adfield.BooleanAdEvaluator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AdEvaluator(evaluator = BooleanAdEvaluator.class)
public @interface AdBoolean {
    Class evaluator = BooleanAdEvaluator.class;

    int index();
    int mask() default 0;
}
