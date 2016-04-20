package com.jameslandrum.bluetoothsmart.actions;

import android.support.annotation.Nullable;

import com.jameslandrum.bluetoothsmart.SmartDevice;

/**
 * Disconnects from the existing connection explicitly.
 */
public class Disconnect extends Action {
	public Disconnect() {
		super();
	}

	@Nullable
	@Override
	public ActionError execute(SmartDevice smartDevice) {
		super.execute(smartDevice);
		smartDevice.getGatt().disconnect();
		return null;
	}

	@Override
	public boolean noDelay() {
		return true;
	}
}
