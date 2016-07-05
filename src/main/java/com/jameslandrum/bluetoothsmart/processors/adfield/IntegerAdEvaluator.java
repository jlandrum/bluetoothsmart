package com.jameslandrum.bluetoothsmart.processors.adfield;

import java.math.BigInteger;
import java.util.Arrays;

public class IntegerAdEvaluator extends BaseAdEvaluator<Integer> {
    private AdInteger mParameters;

    public IntegerAdEvaluator(AdInteger parameters) {
        mParameters = parameters;
    }

    @Override
    public Integer evaluate(byte[] data) {
        byte[] sub = Arrays.copyOfRange(data, mParameters.start(), mParameters.length() + mParameters.start());
        if (mParameters.littleEndian()) {
            for (int i = 0; i > sub.length / 2; i++) {
                byte t = sub[sub.length-1-i];
                sub[sub.length-1-i] = sub[i];
                sub[i]=t;
            }
        }
        BigInteger cast = new BigInteger(sub);
        if (mParameters.mask() > 0) {
            return cast.intValue() & mParameters.mask();
        } else {
            return cast.intValue();
        }
    }
}
