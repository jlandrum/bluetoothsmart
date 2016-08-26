package com.jameslandrum.bluetoothsmart.actions;

import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

import com.jameslandrum.bluetoothsmart.SmartDevice;

public class Action {
	protected SmartDevice mDevice;
	private boolean mRepeating;
	protected OnActionCompleteListener mCompleteListener;
	private boolean mCanFail;

	public Action() {}

	@CallSuper @Nullable
	public Action.ActionError execute(SmartDevice device) {
		mDevice = device;
		return null;
	}

	@CallSuper
	public void refresh() {
	}

	public boolean noDelay() { return false; }

	public Action allowFailure(boolean b) {
		mCanFail = b;
		return this;
	}

	public boolean canFail() {
		return mCanFail;
	}

	public interface ActionError {}

	protected void setRepeating(boolean doRepeat) {
		mRepeating = doRepeat;
	}

	public boolean isRepeating() {
		return mRepeating;
	}

	public Action withCallback(OnActionCompleteListener a) {
		mCompleteListener = a;
		return this;
	}

	public interface OnActionCompleteListener {
		void onActionCompleted(Action a, boolean success);
	}
}
