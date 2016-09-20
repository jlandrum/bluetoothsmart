/**
 * Copyright 2016 James Landrum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
