package com.jameslandrum.bluetoothsmart.actions;

import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

import com.jameslandrum.bluetoothsmart.SmartDevice;

public class Action {
	private int mId;
	private int mGroup;

	public Action() {
		this(-1);
	}

	public Action(int id) {
		this(id, -1);
	}

	public Action(int id, int group) {
		mGroup = group;
		mId = id;
	}

	@CallSuper @Nullable
	public Action.ActionError execute(SmartDevice gatt) {
		return null;
	}

	@CallSuper
	public void refresh() {
	}

	public int getId() {
		return mId;
	}
	public int getGroup() { return mGroup; }
	public boolean noDelay() { return false; }

	public interface ActionError {}
}
