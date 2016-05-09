package com.jameslandrum.bluetoothsmart.actions;

import android.support.annotation.Nullable;

import com.jameslandrum.bluetoothsmart.SmartDevice;

/**
 * An action that will cause a forced delay before the next action in the queue
 */
public class Delay extends Action {
	private int mDelay;

	public Delay(int delay, int id, int group) {
		super(id, group);
		mDelay = delay;
	}

	@Nullable
	@Override
	public ActionError execute(SmartDevice smartDevice) {
		super.execute(smartDevice);
		try {
			Thread.sleep(mDelay);
		} catch (InterruptedException ignored) {}
		return null;
	}

	@Override
	public boolean noDelay() {
		return true;
	}

	@Override
	public int getId() {
		return mDelay;
	}

	@Override
	public String toString() {
		return "Delay " + mDelay + "ms";
	}
}
