package com.jameslandrum.bluetoothsmart.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AdValue {
	int startBit();
	int endBit();
}
