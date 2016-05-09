package com.jameslandrum.bluetoothsmart.actions;

import android.support.annotation.Nullable;

import com.jameslandrum.bluetoothsmart.SmartDevice;

/**
 * Disconnects from the existing connection explicitly.
 */
public class Connect extends Action implements SmartDevice.UpdateListener {
	private final Object mHolder = new Object();
	public static final Connect CONNECT = new Connect();

	public Connect() {
		super();
	}

	@Nullable
	@Override
	public ActionError execute(SmartDevice smartDevice) {
		super.execute(smartDevice);
		if (smartDevice.isConnected()) return null;

		smartDevice.connect();
		smartDevice.addOnUpdateListener(this);
		try {
			synchronized (mHolder) {
				mHolder.wait(30000);
			}
		} catch (InterruptedException e) {
			smartDevice.disconnect();
			return new FailedToConnectError();
		}

		return null;
	}

	@Override
	public boolean noDelay() {
		return true;
	}

	@Override
	public void onUpdate(Object device) {}

	@Override
	public void onConnect() {
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
