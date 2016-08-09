package com.jameslandrum.bluetoothsmart.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.jameslandrum.bluetoothsmart.annotations.AdValue.Type.BIT;

@Retention(RetentionPolicy.RUNTIME)
public @interface AdValue {
	int start();
	int end();
	boolean signed() default true;
	boolean flip() default false;
	Type type() default BIT;
	public enum Type {
		BIT,
		BYTE
	}
}

