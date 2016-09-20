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
		if (!smartDevice.createBond()) {
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
