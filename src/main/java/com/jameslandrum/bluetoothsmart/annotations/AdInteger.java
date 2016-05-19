package com.jameslandrum.bluetoothsmart.annotations;

import com.jameslandrum.bluetoothsmart.processors.adfield.IntegerAdEvaluator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AdEvaluator(evaluator = IntegerAdEvaluator.class)
public @interface AdInteger {
    int start();
    int length() default 1;
    int mask() default 0x00;
    boolean littleEndian() default true;
}