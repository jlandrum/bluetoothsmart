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
