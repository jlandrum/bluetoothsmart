package com.jameslandrum.bluetoothsmart.actions;

import android.support.annotation.Nullable;

import com.jameslandrum.bluetoothsmart.SmartDevice;
import com.jameslandrum.bluetoothsmart.scanner.DeviceScanner;

/**
 * Disconnects from the existing connection explicitly.
 */
public class Connect extends Action implements SmartDevice.UpdateListener {
	private final Object mHolder = new Object();
	public static final Connect CONNECT = new Connect();
	private ActionError mError;

	public Connect() {
		super();
	}

	@Nullable
	@Override
	public ActionError execute(SmartDevice smartDevice) {
		super.execute(smartDevice);
		if (smartDevice.isConnected()) return null;

		mError = new FailedToConnectError();

		smartDevice.connect(false);
		smartDevice.addOnUpdateListener(this);
		try {
			synchronized (mHolder) {
				mHolder.wait(15000);
			}
		} catch (InterruptedException e) {
			smartDevice.disconnect();
		}

		return mError;
	}

	@Override
	public boolean noDelay() {
		return true;
	}

	@Override
	public void onUpdate(Object device) {}

	@Override
	public void onConnect() {
		mError = null;
		synchronized (mHolder) {
			mHolder.notify();
		}
	}

	@Override
	public void onDisconnect() {}

	private class FailedToConnectError implements ActionError {}

	@Override
	public String toString() {
		return "Connect";
	}
}
