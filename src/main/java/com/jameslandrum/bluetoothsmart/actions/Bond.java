package com.jameslandrum.bluetoothsmart.actions;

import android.support.annotation.Nullable;

import com.jameslandrum.bluetoothsmart.SmartDevice;
import com.jameslandrum.bluetoothsmart.actions.errors.FailedToBondError;

/**
 * Creates a bond with the given device.
 */
public class Bond extends Action {
	private final Object mHolder = new Object();
	public static final Bond BOND = new Bond();

	public Bond() {
		super();
	}

	@Nullable
	@Override
	public ActionError execute(SmartDevice smartDevice) {
		super.execute(smartDevice);
		if (smartDevice.isBonded()) return null;
		if (!smartDevice.bond()) {
			return new FailedToBondError();
		} return null;
	}

	@Override
	public boolean noDelay() {
		return true;
	}

	@Override
	public String toString() {
		return "Bonding";
	}
}
