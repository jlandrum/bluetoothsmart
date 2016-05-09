package com.jameslandrum.bluetoothsmart.annotations;

import com.jameslandrum.bluetoothsmart.processors.adfield.IntegerAdEvaluator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CharDef {
    String service();
    String id();
}
