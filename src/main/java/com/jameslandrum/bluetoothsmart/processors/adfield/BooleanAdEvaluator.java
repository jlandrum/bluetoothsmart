package com.jameslandrum.bluetoothsmart.processors.adfield;

import com.jameslandrum.bluetoothsmart.annotations.AdBoolean;

public class BooleanAdEvaluator extends BaseAdEvaluator<Boolean> {
    AdBoolean mParams;

    public BooleanAdEvaluator(AdBoolean params) {
        mParams = params;
    }

    @Override
    public Boolean evaluate(byte[] data) {
        return (data[mParams.index()] & mParams.mask()) != 0;
    }
}
